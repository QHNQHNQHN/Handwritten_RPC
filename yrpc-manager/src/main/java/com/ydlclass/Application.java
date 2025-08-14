package com.ydlclass;

import com.ydlclass.utils.zookeeper.ZookeeperNode;
import com.ydlclass.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.util.List;

/**
 * Application 是一个初始化 Zookeeper 注册中心目录结构的启动类。
 * 在 Zookeeper 中创建基础的持久化节点结构，为服务注册和消费打下 “目录基础 ”。
 *
 * @author QHN
 * @date 2025/08/14
 */
@Slf4j
public class Application {

    /*
     帮我们创建基础目录
     yrpc-metadata                  (根目录, 持久节点)
      └─ providers                  (服务提供者的注册目录, 持久节点)
              └─ service1             (持久节点, 接口的全限定名)
                  ├─ node1 [data]     /ip:port
                  ├─ node2 [data]
                └─ node3 [data]
      └─ consumers
            └─ service1
                 ├─ node1 [data]
                 ├─ node2 [data]
                 └─ node3 [data]
      └─ config
    */
    public static void main(String[] args) throws InterruptedException {

        // 使用工具类 ZooKeeper 创建一个 zookeeper 客户端实例，建立与 Zookeeper 服务器的连接。
        ZooKeeper zooKeeper = ZookeeperUtils.createZookeeper();
        // 定义基础路径
        // /yrpc-metadata：是 rpc 在 zooKeeper 中的根目录
        String basePath = "/yrpc-metadata";
        // /providers 是存储服务提供者节点 ()
        String providerPath = basePath + "/providers";
        //
        String consumersPath = basePath + "/consumers";
        // 封装为节点对象
        ZookeeperNode baseNode = new ZookeeperNode(basePath, null);
        ZookeeperNode providersNode = new ZookeeperNode(providerPath, null);
        ZookeeperNode consumersNode = new ZookeeperNode(consumersPath, null);

        // 批量创建持久节点
        List.of(baseNode, providersNode, consumersNode).forEach(node -> {
            ZookeeperUtils.createNode(zooKeeper, node, null, CreateMode.PERSISTENT);
        });

        // 关闭连接
        ZookeeperUtils.close(zooKeeper);
    }
}
