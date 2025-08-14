package com.ydlclass;

import com.ydlclass.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于 YRPC 框架的 RPC 消费者端启动类（ConsumerApplication），
 * 其核心目的是从远程服务中获取一个接口的代理对象（HelloYrpc），
 * 并调用其中定义的方法（如 sayHi），从而实现客户端到服务端的远程调用。
 *
 * @author QHN
 * @date 2025/08/12
 */
@Slf4j
public class ConsumerApplication {

    // 消费者端获取代理对象
    public static void main(String[] args) {
        // ReferenceConfig<T> 用于封装消费者端调用远程接口的配置，泛型表示要调用的接口类型
        ReferenceConfig<HelloYrpc> reference = new ReferenceConfig<>();
        // 将远程调用的接口类型告诉 referenceConfig 配置对象
        reference.setInterface(HelloYrpc.class);

        YrpcBootstrap.getInstance()
            //设置应用的名字
            .application("first-yrpc-consumer")
            //配置一个注册中心
            .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
            .serialize("hessian")
            //设置数据压缩方式
            .compress("gzip")
            //指定服务分组
            .group("primary")
            //注册创建的reference，告诉框架生成哪个接口的代理对象
            .reference(reference);
    
        System.out.println("++------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        /*
         获取远程接口的代理对象
         代理做了些什么?
         1、连接注册中心
         2、拉取服务列表
         3、选择一个服务并建立连接
         4、发送请求，携带一些信息（接口名，参数列表，方法的名字），获得结果
        */
        HelloYrpc helloYrpc = reference.get();
     
        while (true) {
            for (int i = 0; i < 50; i++) {
                String sayHi = helloYrpc.sayHi("你好 yrpc");
                log.info("sayHi-->{}", sayHi);
            }
        }
        
    }
}
