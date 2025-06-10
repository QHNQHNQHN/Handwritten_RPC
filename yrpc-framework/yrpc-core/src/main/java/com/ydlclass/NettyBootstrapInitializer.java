package com.ydlclass;

import com.ydlclass.channelhandler.ConsumerChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
/**
        Netty 客户端引导类 NettyBootstrapInitializer。
        作用：为服务消费者（Consumer）提供一个全局唯一的 Netty Bootstrap 实例，用于连接远程服务节点。
 */
@Slf4j
public class NettyBootstrapInitializer {

    // 声明了一个全局 Bootstrap 实例
    private static final Bootstrap bootstrap = new Bootstrap();

    // 静态代码块，类加载时运行一次，用于配置 Bootstrap
    //
    static {
        NioEventLoopGroup group = new NioEventLoopGroup();
        // Netty 的标准客户端配置
        bootstrap.group(group)                          //设置线程池
            .channel(NioSocketChannel.class)            //表明使用 NIO Socket 通道
            .handler(new ConsumerChannelInitializer()); //设置成功连接后要加载的ChannelHandle管道链(用于消息编码)
    }
    
    private NettyBootstrapInitializer() {
    }
    
    public static Bootstrap getBootstrap() {
        return bootstrap;
    }
}
