package com.ydlclass;


import com.ydlclass.annotation.YrpcApi;
import com.ydlclass.channelhandler.handler.MethodCallHandler;
import com.ydlclass.channelhandler.handler.YrpcRequestDecoder;
import com.ydlclass.channelhandler.handler.YrpcResponseEncoder;
import com.ydlclass.config.Configuration;
import com.ydlclass.core.HeartbeatDetector;
import com.ydlclass.core.YrpcShutdownHook;
import com.ydlclass.discovery.RegistryConfig;
import com.ydlclass.loadbalancer.LoadBalancer;
import com.ydlclass.transport.message.YrpcRequest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
/**
    YrpcBootstrap 是整个 YRPC 框架的启动器和上下文配置核心类，采用单例模式。
 | 功能区域              | 具体职责                                               |
 | ----------------- | -------------------------------------------------- |
 |  全局配置管理          | 持有 `Configuration` 对象，用于统一管理注册中心、序列化、压缩、分组等配置      |
 |  服务提供者（Provider） | 提供 `publish()` 方法将服务注册到注册中心（如 Zookeeper），供客户端发现与调用 |
 |  服务消费者（Consumer） | 提供 `reference()` 方法生成动态代理，实现远程调用，集成心跳检测            |
 |  Netty 服务端启动     | 封装 Netty 的启动流程，包括 Handler 注册、端口监听等                 |
 |  扫描注册服务          | 提供 `scan()` 方法，支持通过注解 + 包扫描的方式自动发布服务               |
 |  网络资源与请求管理       | 管理请求线程上下文、连接缓存、未响应请求等数据结构                          |

 */
@Slf4j
public class YrpcBootstrap {
    
    
    // YrpcBootstrap是个单例，我们希望每个应用程序只有一个实例
    private static final YrpcBootstrap yrpcBootstrap = new YrpcBootstrap();
    
    // 全局的配置中心
    private final Configuration configuration;
   
    // 保存request对象，可以到当前线程中随时获取
    public static final ThreadLocal<YrpcRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();
    
    // 连接的缓存,如果使用InetSocketAddress这种对象的类做key，一定要看他有没有重写equals方法和toString方法
    // key 为服务端地址(ip+端口)，value 为该地址建立的netty通信通道。用来缓存与远程服务器提供者的连接通道Channel
    // ConcurrentHashMap 保证线程安全
    public final static Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);
    public final static TreeMap<Long, Channel> ANSWER_TIME_CHANNEL_CACHE = new TreeMap<>();
    
    // 维护已经发布且暴露的服务列表 key-> interface的全限定名  value -> ServiceConfig
    // 这个映射表是服务提供方用来保存自己暴露的服务实现。通过接口名快速查找对应实现的对象，以便执行请求方法
    public final static Map<String, ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);
    
    // 定义全局的对外挂起的 completableFuture
    public final static Map<Long, CompletableFuture<Object>> PENDING_REQUEST = new ConcurrentHashMap<>(128);
    
    
    // 维护一个zookeeper实例
//    private ZooKeeper zooKeeper;
    
    private YrpcBootstrap() {
        // 构造启动引导程序，时需要做一些什么初始化的事
        configuration = new Configuration();
    }
    
    public static YrpcBootstrap getInstance() {
        return yrpcBootstrap;
    }
    
    /**
     * 用来定义当前应用的名字
     *
     * @param appName 应用的名字
     * @return this当前实例
     */
    public YrpcBootstrap application(String appName) {
        configuration.setAppName(appName);
        return this;
    }
    
    /**
     * 用来配置一个注册中心
     *
     * @param registryConfig 注册中心
     * @return this当前实例
     */
    public YrpcBootstrap registry(RegistryConfig registryConfig) {
        // 这里维护一个zookeeper实例，但是，如果这样写就会将zookeeper和当前工程耦合
        // 我们其实是更希望以后可以扩展更多种不同的实现
        
        // 尝试使用 registryConfig 获取一个注册中心，有点工厂设计模式的意思了
        configuration.setRegistryConfig(registryConfig);
        return this;
    }
    
    /**
     * 配置负载均衡策略
     * @param loadBalancer 注册中心
     * @return this当前实例
     */
    public YrpcBootstrap loadBalancer(LoadBalancer loadBalancer) {
        configuration.setLoadBalancer(loadBalancer);
        return this;
    }
    
    
    /**
     * ---------------------------服务提供方 provide 的相关 api---------------------------------
     */
    
    /**
     * 发布服务: 将接口及实现类，注册到服务中心 ZOOKEEPER，使得别的客户端可以远程访问这个服务
     *
     * @param service 封装的需要发布的服务
     * @return this当前实例
     */
    public YrpcBootstrap publish(ServiceConfig<?> service) {
        // 我们抽象了注册中心的概念，使用注册中心的一个实现完成注册
        // 有人会想，此时此刻难道不是强耦合了吗？
        configuration.getRegistryConfig().getRegistry().register(service);
        
        // 1、当服务调用方，通过接口、方法名、具体的方法参数列表发起调用，提供怎么知道使用哪一个实现
        // (1) new 一个  （2）spring beanFactory.getBean(Class)  (3) 自己维护映射关系
        SERVERS_LIST.put(service.getInterface().getName(), service);
        return this;
    }
    
    /**
     * 批量发布
     *
     * @param services 封装的需要发布的服务集合
     * @return this当前实例
     */
    public YrpcBootstrap publish(List<ServiceConfig<?>> services) {
        for (ServiceConfig<?> service : services) {
            this.publish(service);
        }
        return this;
    }
    
    /**
     * 启动netty服务
     */
    public void start() {
        // 注册关闭应用程序的钩子函数
        Runtime.getRuntime().addShutdownHook(new YrpcShutdownHook());
        
        // 1、创建eventLoop，老板只负责处理请求，之后会将请求分发至worker
        EventLoopGroup boss = new NioEventLoopGroup(2);
        EventLoopGroup worker = new NioEventLoopGroup(10);
        try {
            
            // 2、需要一个服务器引导程序
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 3、配置服务器
            serverBootstrap = serverBootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        // 是核心，我们需要添加很多入站和出站的handler
                        socketChannel.pipeline().addLast(new LoggingHandler())
                            .addLast(new YrpcRequestDecoder())
                            // 根据请求进行方法调用
                            .addLast(new MethodCallHandler())
                            .addLast(new YrpcResponseEncoder())
                        ;
                    }
                });
            
            // 4、绑定端口
            ChannelFuture channelFuture = serverBootstrap.bind(configuration.getPort()).sync();
            
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
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
    /**
     * ---------------------------服务调用方 consumer 的相关 api---------------------------------
     */
    public YrpcBootstrap reference(ReferenceConfig<?> reference) {
        
        // 开启对这个服务的心跳检测
        HeartbeatDetector.detectHeartbeat(reference.getInterface().getName());
        
        // 在这个方法里我们是否可以拿到相关的配置项-注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        // 1、reference需要一个注册中心
        reference.setRegistry(configuration.getRegistryConfig().getRegistry());
        reference.setGroup(this.getConfiguration().getGroup());
        return this;
    }
    
    /**
     * 配置序列化的方式
     * @param serializeType 序列化的方式
     */
    public YrpcBootstrap serialize(String serializeType) {
        configuration.setSerializeType(serializeType);
        if (log.isDebugEnabled()) {
            log.debug("我们配置了使用的序列化的方式为【{}】.", serializeType);
        }
        return this;
    }
    
    public YrpcBootstrap compress(String compressType) {
        configuration.setCompressType(compressType);
        if (log.isDebugEnabled()) {
            log.debug("我们配置了使用的压缩算法为【{}】.", compressType);
        }
        return this;
    }
    
    /**
     * 扫描包，进行批量注册
     * @param packageName 包名
     * @return  this本身
     */
    public YrpcBootstrap scan(String packageName) {
        // 1、需要通过packageName获取其下的所有的类的权限定名称
        List<String> classNames = getAllClassNames(packageName);
        // 2、通过反射获取他的接口，构建具体实现
        List<Class<?>> classes = classNames.stream()
            .map(className -> {
                try {
                    return Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }).filter(clazz -> clazz.getAnnotation(YrpcApi.class) != null)
            .collect(Collectors.toList());
    
        for (Class<?> clazz : classes) {
           // 获取他的接口
            Class<?>[] interfaces = clazz.getInterfaces();
            Object instance = null;
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            
            // 获取分组信息
            YrpcApi yrpcApi = clazz.getAnnotation(YrpcApi.class);
            String group = yrpcApi.group();
    
            for (Class<?> anInterface : interfaces) {
                ServiceConfig<?> serviceConfig = new ServiceConfig<>();
                serviceConfig.setInterface(anInterface);
                serviceConfig.setRef(instance);
                serviceConfig.setGroup(group);
                if (log.isDebugEnabled()){
                    log.debug("---->已经通过包扫描，将服务【{}】发布.",anInterface);
                }
                // 3、发布
                publish(serviceConfig);
            }
            
        }
        return this;
    }
    
    private List<String> getAllClassNames(String packageName) {
        // 1、通过packageName获得绝对路径
        // com.ydlclass.xxx.yyy -> E://xxx/xww/sss/com/ydlclass/xxx/yyy
        String basePath = packageName.replaceAll("\\.","/");
        URL url = ClassLoader.getSystemClassLoader().getResource(basePath);
        if(url == null){
            throw new RuntimeException("包扫描时，发现路径不存在.");
        }
        String absolutePath = url.getPath();
        //
        List<String> classNames = new ArrayList<>();
        classNames = recursionFile(absolutePath,classNames,basePath);
    
        return classNames;
    }
    
    private List<String> recursionFile(String absolutePath, List<String> classNames,String basePath) {
        // 获取文件
        File file = new File(absolutePath);
        // 判断文件是否是文件夹
        if (file.isDirectory()){
            // 找到文件夹的所有的文件
            File[] children = file.listFiles(pathname -> pathname.isDirectory() || pathname.getPath().contains(".class"));
            if(children == null || children.length == 0){
                return classNames;
            }
            for (File child : children) {
                if(child.isDirectory()){
                    // 递归调用
                    recursionFile(child.getAbsolutePath(),classNames,basePath);
                } else {
                    // 文件 --> 类的权限定名称
                    String className = getClassNameByAbsolutePath(child.getAbsolutePath(),basePath);
                    classNames.add(className);
                }
            }
    
        } else {
            // 文件 --> 类的权限定名称
            String className = getClassNameByAbsolutePath(absolutePath,basePath);
            classNames.add(className);
        }
        return classNames;
    }
    
    private String getClassNameByAbsolutePath(String absolutePath,String basePath) {
        // E:\project\ydlclass-yrpc\yrpc-framework\yrpc-core\target\classes\com\ydlclass\serialize\Serializer.class
        // com\ydlclass\serialize\Serializer.class --> com.ydlclass.serialize.Serializer
        String fileName = absolutePath
            .substring(absolutePath.indexOf(basePath.replaceAll("/","\\\\")))
            .replaceAll("\\\\",".");
        
        fileName = fileName.substring(0,fileName.indexOf(".class"));
        return fileName;
    }
    
    
    public static void main(String[] args) {
        List<String> allClassNames = YrpcBootstrap.getInstance().getAllClassNames("com.ydlclass");
        System.out.println(allClassNames);
    }
    
    public Configuration getConfiguration() {
        return configuration;
    }
    
    public YrpcBootstrap group(String group) {
        this.getConfiguration().setGroup(group);
        return this;
    }
}
