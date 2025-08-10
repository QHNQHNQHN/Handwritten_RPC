package com.ydlclass;

import com.ydlclass.netty.AppClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * @author QHN
 * @date 2025/08/08
 */
public class NettyTest {

    @Test
    public void testCompositeByteBuf (){
        ByteBuf header = Unpooled.buffer();
        ByteBuf body = Unpooled.buffer();

        // 通过逻辑组装而不是物理拷贝，实现在jvm中的零拷贝
        CompositeByteBuf byteBuf = Unpooled.compositeBuffer();
        byteBuf.addComponents(header,body);
    }

    @Test
    public void testWrapper (){
        byte[] buf = new byte[1024];
        byte[] buf2 = new byte[1024];
        // 共享byte数组的内容而不是拷贝，这也算零拷贝
        ByteBuf byteBuf = Unpooled.wrappedBuffer(buf, buf2);
    }

    @Test
    public void testSlice (){
        byte[] buf = new byte[1024];
        byte[] buf2 = new byte[1024];
        // 共享byte数组的内容而不是拷贝，这也算零拷贝
        ByteBuf byteBuf = Unpooled.wrappedBuffer(buf, buf2);

        // 同样可以将一个byteBuf，分割成多个，使用共享地址，而非拷贝
        ByteBuf buf1 = byteBuf.slice(1, 5);
        ByteBuf buf3 = byteBuf.slice(6, 15);
    }

    // 测试封装报文
    @Test
    public void testMessage() throws IOException {
        // 创建一个字节缓冲对象
        ByteBuf message = Unpooled.buffer();
        // 魔数：ydl
        message.writeBytes("ydl".getBytes(StandardCharsets.UTF_8));
        // 版本号：1
        message.writeByte(1);
        // 头部长度：125
        message.writeShort(125);
        // body 长度
        message.writeInt(256);
        // 消息类型
        message.writeByte(1);
        // 序列化类型
        message.writeByte(0);
        // 请求 id
        message.writeLong(251455L);

        // 得到 body部分
        byte[] bytes = getBytes();
        // 将 body 部分与上面的 header 部门写在一起
        message.writeBytes(bytes);

        printAsBinary(message);

    }


    private static byte[] getBytes() throws IOException {
        AppClient appClient = new AppClient();
        // ByteArrayOutputStream 是一个内存缓冲流，可以把数据写到内存中的字节数组里
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // ObjectOutputStream 是对象序列化流，可以把 java 对象转成一串字节
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        // 把 appClient 对象序列化成二进制。写入 outputStream
        oos.writeObject(appClient);
        // 从 ByteArrayOutputStream 中取出刚才序列化得到的全部字节数据。
        byte[] bytes = outputStream.toByteArray();
        return bytes;
    }

    //将 byteBuf 变成一个 16 进制的字节数组
    public static void printAsBinary(ByteBuf byteBuf) {
        // 创建一个字节数组保存 ByteBuf 的数据
        byte[] bytes = new byte[byteBuf.readableBytes()];
        // 把 byteBuf 的数据拷贝到字节数组里
        byteBuf.getBytes(byteBuf.readerIndex(), bytes);
        // 把字节数组转成 16 进制字符串
        String binaryString = ByteBufUtil.hexDump(bytes);
        StringBuilder formattedBinary = new StringBuilder();
        // 每两个字符一组，取出后加一个空格
        for (int i = 0; i < binaryString.length(); i += 2) {
            formattedBinary.append(binaryString.substring(i, i + 2)).append(" ");
        }

        System.out.println("Binary representation: " + formattedBinary.toString());
    }

    // 把原始字节数组用 GZIP 压缩，对比压缩前后大小并打印结果
    @Test
    public void testCompress() throws IOException {
        // 构造原始数据
        byte[] buf = new byte[]{12,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14,12,12,12,12,25,34,23,25,14};

        // 本质就是，将 buf 作为输入，将结果输出到另一个字节数组当中
        // 内存缓冲输出流，作为压缩结果的承接容器
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 把 baos 包一层 GZIP 压缩流
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(baos);
        // 把 buf 写入压缩流，写入过程中完成压缩
        gzipOutputStream.write(buf);
        // 告诉压缩流结束写入
        gzipOutputStream.finish();
        // 从内存缓冲里取出压缩后的全部字节
        byte[] bytes = baos.toByteArray();
        System.out.println(buf.length + "--> " + bytes.length);
        System.out.println(Arrays.toString(bytes));
    }


    // 解压测试
    @Test
    public void testDeCompress() throws IOException {
        // 压缩后的数据
        byte[] buf = new byte[]{31, -117, 8, 0, 0, 0, 0, 0, 0, -1, -29, -31, 1, 2, 73, 37, 113, 73, -66, 65, -62, 0, 0, 25, -102, -59, -115, -111, 0, 0, 0};

        // 本质就是，将buf作为输入，将结果输出到另一个字节数组当中
        // 创建一个内存输入流
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        // 用 GZIPInputStream 包一层内存输入流
        GZIPInputStream gzipInputStream = new GZIPInputStream(bais);

        // 读完所有解压后的字节
        byte[] bytes = gzipInputStream.readAllBytes();
        System.out.println(buf.length + "--> " + bytes.length);
        System.out.println(Arrays.toString(bytes));
    }
}
