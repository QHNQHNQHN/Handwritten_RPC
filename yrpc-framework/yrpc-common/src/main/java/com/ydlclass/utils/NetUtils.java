package com.ydlclass.utils;

import com.ydlclass.exceptions.NetworkException;
import lombok.extern.slf4j.Slf4j;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 这段代码定义了一个工具类 NetUtils，
 * 其核心功能是：自动获取当前主机的局域网 IPv4 地址，并过滤掉不合适的地址（如虚拟网卡、回环地址、IPv6 等）。
 *
 * @author QHN
 * @date 2025/08/14
 */
@Slf4j
public class NetUtils {
    
    public static String getIp() {
        try {
            // 获取所有的网卡信息
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // 过滤非回环接口和虚拟接口
                if (iface.isLoopback() || iface.isVirtual() || !iface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // 过滤IPv6地址和回环地址
                    if (addr instanceof Inet6Address || addr.isLoopbackAddress()) {
                        continue;
                    }
                    String ipAddress = addr.getHostAddress();
                    if(log.isDebugEnabled()){
                        log.debug("局域网 IP 地址：{}",ipAddress);
                    }
                    return ipAddress;
                }
            }
            throw new NetworkException();
        } catch (SocketException e) {
            log.error("获取局域网 ip 时放生异常。", e);
            throw new NetworkException(e);
        }
    }
    
    public static void main(String[] args) {
        String ip = NetUtils.getIp();
        System.out.println("ip = " + ip);
    }
    
}
