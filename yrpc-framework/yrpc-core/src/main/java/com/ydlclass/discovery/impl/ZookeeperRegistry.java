package com.ydlclass.discovery.impl;

import com.ydlclass.Constant;
import com.ydlclass.ServiceConfig;
import com.ydlclass.YrpcBootstrap;
import com.ydlclass.discovery.AbstractRegistry;
import com.ydlclass.exceptions.DiscoveryException;
import com.ydlclass.utils.NetUtils;
import com.ydlclass.utils.zookeeper.ZookeeperNode;
import com.ydlclass.utils.zookeeper.ZookeeperUtils;
import com.ydlclass.watch.UpAndDownWatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.List;


/**
 * 这段代码实现的基于 zookeeper 的服务注册与发现，具体是两个核心功能
 * 1. 服务提供方把服务注册到 zookeeper (服务发布)
 * 2. 服务调用方从 zookeeper 获取可用服务节点列表 (服务发现)
 * @author QHN
 * @date 2025/08/14
 */
@Slf4j
public class ZookeeperRegistry extends AbstractRegistry {

    // 维护一个 zk 客户端实例
    private ZooKeeper zooKeeper;

    // 默认构造器
    public ZookeeperRegistry() {
        this.zooKeeper = ZookeeperUtils.createZookeeper();
    }

    // 带参构造器
    public ZookeeperRegistry(String connectString, int timeout) {
        this.zooKeeper = ZookeeperUtils.createZookeeper(connectString, timeout);
    }


    @Override
    /*
        服务发现
        /yrpc-metadata
        └── providers
            └── com.example.MyService       (持久节点)
                └── default                 (持久节点)
                    ├── 192.168.1.100:8094  (临时节点)

     */
    public void register(ServiceConfig<?> service) {

        // 创建服务名称的节点
        String parentNode = Constant.BASE_PROVIDERS_PATH + "/" + service.getInterface().getName();
        // 判断节点是否存在
        if (!ZookeeperUtils.exists(zooKeeper, parentNode, null)) {
            // 不存在就创建服务节点(持久节点)
            ZookeeperNode zookeeperNode = new ZookeeperNode(parentNode, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.PERSISTENT);
        }

        // 创建分组节点
        parentNode = parentNode + "/" + service.getGroup();
        // 判断节点是否存在
        if (!ZookeeperUtils.exists(zooKeeper, parentNode, null)) {
            // 不存在就创建分组节点(持久节点)
            ZookeeperNode zookeeperNode = new ZookeeperNode(parentNode, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.PERSISTENT);
        }


        //todo: 后续处理端口的问题
        /*
            构造当前服务实例节点路径
            parentNode：/yrpc-metadata/providers/接口名/分组名
            NetUtils.getIp()：获取当前机器的局域网 ip (例如 192.168.1.100，不是 127.0.0.1)
            最后一项是框架配置里设置的服务端口
         */
        String node = parentNode + "/" + NetUtils.getIp() + ":" + YrpcBootstrap.getInstance().getConfiguration().getPort();
        if (!ZookeeperUtils.exists(zooKeeper, node, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(node, null);
            // 注意此时是临时节点
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.EPHEMERAL);
        }

        if (log.isDebugEnabled()) {
            log.debug("服务{}，已经被注册", service.getInterface().getName());
        }
    }

    /**
     * 在注册中心查找某个服务名和分组下的所有可用服务节点，并返回 ip 和端口列表
     * @param serviceName 服务名称
     * @return 服务列表
     */
    @Override
    public List<InetSocketAddress> lookup(String serviceName, String group) {
        // 1、拼接出该服务的完整路径
        String serviceNode = Constant.BASE_PROVIDERS_PATH + "/" + serviceName + "/" + group;

        // 2、从zk中获取他的子节点 (ip:port 192.168.12.123:2151)
        List<String> children = ZookeeperUtils.getChildren(zooKeeper, serviceNode, new UpAndDownWatcher());
        // 获取了所有的可用的服务列表
        List<InetSocketAddress> inetSocketAddresses = children.stream().map(ipString -> {
            // 按：分割 ip 和端口
            String[] ipAndPort = ipString.split(":");
            String ip = ipAndPort[0];
            int port = Integer.parseInt(ipAndPort[1]);
            // 用 ip 和端口创建 InetSocketAddress 对象
            return new InetSocketAddress(ip, port);
        }).toList();

        // 如果没有任何可用实例，则抛出异常
        if (inetSocketAddresses.size() == 0) {
            throw new DiscoveryException("未发现任何可用的服务主机.");
        }

        return inetSocketAddresses;
    }
}
