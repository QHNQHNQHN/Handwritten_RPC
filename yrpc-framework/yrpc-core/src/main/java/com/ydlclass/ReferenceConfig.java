package com.ydlclass;

import com.ydlclass.discovery.Registry;
import com.ydlclass.proxy.handler.RpcConsumerInvocationHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * ReferenceConfig<T> 是 YRPC 框架中 客户端（Consumer）引用服务的配置类
 * 封装消费者对某个远程服务接口的引用配置，并通过动态代理生成一个远程调用代理对象，供客户端调用。
 */
@Slf4j
public class ReferenceConfig<T> {

    private Class<T> interfaceRef;  // 目标接口类，例如 HelloYrpc.class
    private Registry registry;      // 注册中心对象，用于服务发现
    private String group;           // 分组信息，用于区分同一接口的多个版本


    public void setInterface(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }

    /**
     * 代理设计模式，生成一个api接口的代理对象，helloYrpc.sayHi("你好");
     *
     * @return 代理对象
     */
    public T get() {
        // 获取当前线程的类加载器，用于生成代理类。
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // 把目标接口类型放在数组里，即代理类要实现 interfaceRef 这个接口
        Class<T>[] classes = new Class[]{interfaceRef};
        /*
            创建调用处理器InvocationHandler对象,拦截方法调用的核心逻辑。
            1.发现服务：找到注册中心 registry 里的服务地址；
            2.传入服务的名字，构建 RPC 请求（接口名 + 方法名 + 参数）；
            3.使用netty连接服务器，发送调用的服务名字+方法名+参数列表，得到结果
                3.1 整个连接过程放在这里不行，因为每次调用都会产生一个新的netty连接。每次建立新连接是不合适的，解决方案是缓存channel？
                    尝试从缓存中获取channel，如果未获取，则创建新的连接，并进行缓存。
                    放在了RpcConsumerInvocationHandler类的getAvailableChannel方法中
            4.接收服务端响应并返回结果。
         */
        InvocationHandler handler = new RpcConsumerInvocationHandler(registry, interfaceRef, group);
        // 使用 JDK 原生的动态代理 API 创建代理对象。
        Object helloProxy = Proxy.newProxyInstance(classLoader, classes, handler);
        // 最后将代理对象强制转换为 T 类型返回给调用者。
        return (T) helloProxy;
    }


    public Class<T> getInterface() {
        return interfaceRef;
    }

    public void setInterfaceRef(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }
}
