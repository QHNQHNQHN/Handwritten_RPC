package com.ydlclass.discovery;

import com.ydlclass.ServiceConfig;
import java.net.InetSocketAddress;
import java.util.List;
/**
     Registry 是 YRPC 框架中服务注册与服务发现的接口规范。
     将注册中心的功能抽象化，支持多种实现方式（如 Zookeeper、Nacos）。
     并为服务的动态注册与调用提供基础能力。
 */
public interface Registry {
    /**
     * 服务注册
     * 将某个服务注册进注册中心 ZooKeeper
     * @param serviceConfig 服务的配置内容，其封装了服务接口，实现类，分组，地址，端口等信息
     */
    void register(ServiceConfig<?> serviceConfig);
    /**
     * 服务发现
     * 从注册中心获取可用的服务提供者地址列表
     * @param serviceName 接口的全限定名  group 分组名
     * @return 服务的地址(ip地址 + 端口号)
     */
    List<InetSocketAddress> lookup(String serviceName,String group);
    
}
