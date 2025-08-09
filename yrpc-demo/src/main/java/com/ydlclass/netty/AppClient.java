package com.ydlclass.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;


/**
 * @author QHN
 * @date 2025/08/07
 */
public class AppClient implements Serializable {

    public void run() {
        // 定义线程池，EventLoopGroup
        NioEventLoopGroup group = new NioEventLoopGroup();

        // 创建 Netty 客户端启动器对象，用于配置客户端参数
        Bootstrap bootstrap = new Bootstrap();
        try {
            // 设置线程组
            bootstrap = bootstrap.group(group)
                    // 指定连接的服务器地址和端口。这里是 localhost:8080。没有指定 ip，默认本机
                    .remoteAddress(new InetSocketAddress(8080))
                    // 指定客户端 Channel 类型
                    .channel(NioSocketChannel.class)
                    // ChannelInitializer 是特殊 handler。用于初始化 Pipeline
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        // 向处理链添加一个自定义的 Handler，处理 I/O 事件
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new MyChannelHandler2());
                        }
                    });

            // connect 发起异步连接请求。sync 会阻塞当前线程直到连接完成
            ChannelFuture channelFuture = bootstrap.connect().sync();
            // 获取channel，并且写出数据
            // 这里转 ByteBuf 可以写成 handler
            channelFuture.channel().writeAndFlush(Unpooled.copiedBuffer("hello netty".getBytes(StandardCharsets.UTF_8)));

            // 阻塞程序，等到接受消息
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                // 优雅关闭线程组
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new AppClient().run();
    }

}
