package com.ydlclass.discovery.impl;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.ydlclass.ServiceConfig;
import com.ydlclass.discovery.AbstractRegistry;
import com.ydlclass.exceptions.DiscoveryException;
import com.ydlclass.utils.NetUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 真正的 Nacos 注册中心实现类
 */
@Slf4j
public class NacosRegistry extends AbstractRegistry {

    private final NamingService namingService;

    public NacosRegistry(String connectString, int timeout) {
        try {
            Properties properties = new Properties();
            properties.setProperty("serverAddr", connectString);
            properties.setProperty("namespace", "public");
            this.namingService = NacosFactory.createNamingService(properties);
        } catch (NacosException e) {
            log.error("创建 Nacos 实例失败");
            throw new RuntimeException("NacosRegistry 初始化失败", e);
        }
    }

    @Override
    public void register(ServiceConfig<?> service) {
        try {
            String serviceName = service.getInterface().getName() + ":" + service.getGroup();
            String ip = NetUtils.getIp();
            int port = 8080; // 需从 ServiceConfig 获取绑定端口
            namingService.registerInstance(serviceName, ip, port);

            log.info("服务 [{}] 成功注册到 Nacos，地址：{}:{}", serviceName, ip, port);
        } catch (NacosException e) {
            log.error("注册服务到 Nacos 失败");
            throw new RuntimeException("Nacos 注册失败", e);
        }
    }

    @Override
    public List<InetSocketAddress> lookup(String serviceName, String group) {
        try {
            String fullServiceName = serviceName + ":" + group;
            var instances = namingService.getAllInstances(fullServiceName);
            List<InetSocketAddress> addressList = new ArrayList<>();
            for (var instance : instances) {
                addressList.add(new InetSocketAddress(instance.getIp(), instance.getPort()));
            }

            if (addressList.isEmpty()) {
                throw new DiscoveryException("未找到可用服务实例: " + fullServiceName);
            }

            return addressList;
        } catch (NacosException e) {
            log.error("从 Nacos 获取服务失败", e);
            throw new RuntimeException("Nacos 查找失败", e);
        }
    }
}
