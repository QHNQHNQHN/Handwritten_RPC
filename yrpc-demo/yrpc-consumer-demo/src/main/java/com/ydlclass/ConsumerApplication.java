package com.ydlclass;

import com.ydlclass.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;
/**
 基于 YRPC 框架的 RPC 消费者端启动类（ConsumerApplication），
 其核心目的是从远程服务中获取一个接口的代理对象（HelloYrpc），
 并调用其中定义的方法（如 sayHi），从而实现客户端到服务端的远程调用。
 */
@Slf4j
public class ConsumerApplication {
    
    public static void main(String[] args) {
        // 想尽一切办法获取代理对象,使用ReferenceConfig进行封装
        // reference一定用生成代理的模板方法，get()
        ReferenceConfig<HelloYrpc> reference = new ReferenceConfig<>();
        reference.setInterface(HelloYrpc.class);
        
        // 代理做了些什么?
        // 1、连接注册中心
        // 2、拉取服务列表
        // 3、选择一个服务并建立连接
        // 4、发送请求，携带一些信息（接口名，参数列表，方法的名字），获得结果
        YrpcBootstrap.getInstance()
            .application("first-yrpc-consumer")                             //设置应用的名字
            .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))  //配置一个注册中心
            .serialize("hessian")                                         //
            .compress("gzip")
            .group("primary")
            .reference(reference);
    
        System.out.println("++------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        //获取一个代理对象
        HelloYrpc helloYrpc = reference.get();
     
        while (true) {
            for (int i = 0; i < 50; i++) {
                String sayHi = helloYrpc.sayHi("你好 yrpc");
                log.info("sayHi-->{}", sayHi);
            }
        }
        
    }
}
