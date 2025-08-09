package com.ydlclass.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


import java.nio.charset.StandardCharsets;


/**
 * @author QHN
 * @date 2025/08/08
 */

// ChannelInboundHandlerAdapter 是一个入站事件处理器适配类。
// 它实现了 ChannelInboundHandler 接口的所有方法，可以按需重写方法。
public class MyChannelHandler extends ChannelInboundHandlerAdapter {

    /**
     * channelRead：当通道有数据可读时，Netty 自动调用
     * @param ctx：当前 Handler 和 Channel 的上下文，提供操作通道、触发事件、调用其他 handler
     * @param msg：收到的数据对象。一般应为 ByteBuf
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 因为数据是二进制，所以将 msg 强转 ByteBuf
        ByteBuf byteBuf = (ByteBuf) msg;
        // 打印 ByteBuf
        System.out.println("服务端已经收到了消息：-->" + byteBuf.toString(StandardCharsets.UTF_8));
        // 通过 ctx 获取 channel，向通道写入数据，并封装成一个 ByteBuf 用于发送
        ctx.channel().writeAndFlush(Unpooled.copiedBuffer("hello client".getBytes(StandardCharsets.UTF_8)));
    }


    /**
     * exceptionCaught：Netty 运行出现异常会调用
     * @param ctx：上下文
     * @param cause：调用 handler 发生的异常对象 (错误原因)
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        // 打印异常堆栈信息，方便调试
        cause.printStackTrace();
        // 关闭当前通道，释放资源
        ctx.close();
    }
}
