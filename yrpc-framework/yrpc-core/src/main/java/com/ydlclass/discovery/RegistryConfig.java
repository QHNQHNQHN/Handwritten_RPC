package com.ydlclass.discovery;

import com.ydlclass.Constant;
import com.ydlclass.discovery.impl.NacosRegistry;
import com.ydlclass.discovery.impl.ZookeeperRegistry;
import com.ydlclass.exceptions.DiscoveryException;

/**
 * 注册中心连接配置类
 * 从外部配置读取注册中心连接字符串（如 "zookeeper://127.0.0.1:2181"）；
 * 识别是哪种类型的注册中心（zookeeper、nacos、redis 等）；
 * 返回对应实现类（如 ZookeeperRegistry）；
 * @author QHN
 * @date 2025/08/13
 */

public class RegistryConfig {

    /*
        定义连接的 url，此处是 服务提供方配置注册中心时传入的参数
            zookeeper://127.0.0.1:2181
            redis://192.168.12.125:3306
     */
    private final String connectString;

    public RegistryConfig(String connectString) {
        this.connectString = connectString;
    }
    /**
     * 根据 url 获得对应的注册中心实现类实例
     * @return 具体的注册中心实例
     * 简单的工厂模式：调用者只管传地址，至于具体用哪个实现类，内部帮你选好
     */
    public Registry getRegistry() {
        // 1、获取注册中心的类型
        String registryType = getRegistryType(connectString, true).toLowerCase().trim();
        // 2、通过类型获取具体注册中心
        if (registryType.equals("zookeeper")) {
            String host = getRegistryType(connectString, false);
            return new ZookeeperRegistry(host, Constant.TIME_OUT);
        } else if (registryType.equals("nacos")) {
            String host = getRegistryType(connectString, false);
            return new NacosRegistry(host, Constant.TIME_OUT);
        }
        throw new DiscoveryException("未发现合适的注册中心。");
    }


    /**
     * 判断注册中心类型
     * @param connectString 注册中心连接的 url
     * @param ifType 开关参数，true 返回类型；false 返回 ip 和端口
     * @return {@link String }
     */
    private String getRegistryType(String connectString, boolean ifType) {
        // 根据 // 分割成
        String[] typeAndHost = connectString.split("://");
        if (typeAndHost.length != 2) {
            throw new RuntimeException("给定的注册中心连接url不合法");
        }
        if (ifType) {
            // 返回类型：zookeeper
            return typeAndHost[0];
        } else {
            // 返回 ip 和端口：127.0.0.1:2181
            return typeAndHost[1];
        }
    }

}
