package com.ydlclass.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;


/**
 * @author QHN
 * @date 2025/08/08
 */
public class AppServer {

    // 端口参数
    private int port;

    // 构造器
    public AppServer(int port) {
        this.port = port;
    }

    public void start(){

        // 1、创建eventLoop。
        // boss 接收新连接 (accept事件)，worker 处理已经建立连接的读写 (read/write事件)
        EventLoopGroup boss = new NioEventLoopGroup(2);
        EventLoopGroup worker = new NioEventLoopGroup(10);
        try {

            // 2、服务器引导类
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 3、配置服务器
            serverBootstrap = serverBootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    // 指定子通道(新建立的客户端连接)初始化逻辑
                    // handler 是配置 bossChannel的 pipeline，childHandler 是配置子 channel 的 pipeline
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // 添加自定义 handler
                            socketChannel.pipeline().addLast(new MyChannelHandler());
                        }
                    });

            // 4、绑定端口
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            // 5. 拿到服务端的 channel，等待关闭事件
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e){
            e.printStackTrace();
        } finally {
            try {
                boss.shutdownGracefully().sync();
                worker.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    public static void main(String[] args) {
        new AppServer(8080).start();
    }
}
