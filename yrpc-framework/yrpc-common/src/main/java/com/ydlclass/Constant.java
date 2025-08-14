package com.ydlclass;

/**
 * 常量类，每一个字段都是常量
 * @author QHN
 * @date 2025/08/14
 */
public class Constant {
    
    // zookeeper的默认连接地址
    public static final String DEFAULT_ZK_CONNECT = "127.0.0.1:2181";
    
    // zookeeper默认的超时时间
    public static final int TIME_OUT = 10000;
    
    // 服务提供方和调用方在注册中心的基础路径
    public static final String BASE_PROVIDERS_PATH = "/yrpc-metadata/providers";
    public static final String BASE_CONSUMERS_PATH = "/yrpc-metadata/consumers";
    
}
