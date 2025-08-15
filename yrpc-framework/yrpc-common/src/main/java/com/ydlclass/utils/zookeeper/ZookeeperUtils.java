package com.ydlclass.utils.zookeeper;

import com.ydlclass.Constant;
import com.ydlclass.exceptions.ZookeeperException;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
/**
 * ZookeeperUtils 是一个 Zookeeper 客户端工具类，
 * 专门为 YRPC 框架封装了与 Zookeeper 的常用交互操作。
 * 创建连接 ：createZookeeper()：封装了连接逻辑（同步连接 + 事件监听）
 * 创建节点：createNode()：自动判断节点是否存在，不重复创建
 * 判断节点存在：exists()：判断指定路径的节点是否存在
 * 查询子节点：getChildren()：获取指定节点下的所有子节点名称
 * 关闭连接 ：close()：安全关闭 ZooKeeper 实例连接
 * @author QHN
 * @date 2025/08/14
 */
@Slf4j
public class ZookeeperUtils {

    /**
     * 使用默认配置创建zookeeper实例
     * @return zookeeper实例
     */
    public static ZooKeeper createZookeeper() {
        // 定义连接参数
        String connectString = Constant.DEFAULT_ZK_CONNECT;
        // 定义超时时间
        int timeout = Constant.TIME_OUT;
        return createZookeeper(connectString, timeout);
    }

    /**
     * 重载方法，支持自定义参数
     * @param connectString
     * @param timeout
     * @return {@link ZooKeeper }
     */
    public static ZooKeeper createZookeeper(String connectString, int timeout) {
        // CountDownLatch 让主线程等待 zooKeeper 连接成功后再继续执行。
        // 初始化为 1，调用 countDown() 会减到 0，此时 await 阻塞会被释放
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            // 创建 zookeeper 客户端实例，传入连接地址、会话超时时间、事件监听器
            final ZooKeeper zooKeeper = new ZooKeeper(connectString, timeout, event -> {
                // 当 zooKeeper 客户端状态变成 SyncConnected 时，表示连接成功
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    log.debug("客户端已经连接成功。");
                    // 连接完成，释放主线程
                    countDownLatch.countDown();
                }
            });

            // 主线程阻塞，直到连接成功
            countDownLatch.await();
            // 返回已经连接的 zooKeeper 客户端对象
            return zooKeeper;
        } catch (IOException | InterruptedException e) {
            log.error("创建zookeeper实例时发生异常：", e);
            throw new ZookeeperException();
        }
    }
    /**
     * 创建一个节点的工具方法
     * @param zooKeeper  zooKeeper 客户端实例
     * @param node       自定义封装的节点对象
     * @param watcher    watcher 监听器实例
     * @param createMode 节点的类型
     * @return true: 成功创建  false: 已经存在  异常：抛出
     */
    public static Boolean createNode(ZooKeeper zooKeeper, ZookeeperNode node, Watcher watcher, CreateMode createMode) {
        try {
            // 检查节点是否存在，null 为不存在
            if (zooKeeper.exists(node.getNodePath(), watcher) == null) {
                // 不存在就创建节点1
                String result = zooKeeper.create(node.getNodePath(), node.getData(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
                log.info("节点{}，成功创建。", result);
                // 创建成功就返回 true
                return true;
            } else {
                // 已经存在就在 debug 级别下输出提示日志
                if (log.isDebugEnabled()) {
                    log.info("节点{}已经存在，无需创建。", node.getNodePath());
                }
                // 已经存在就返回 false
                return false;
            }
        } catch (KeeperException | InterruptedException e) {
            log.error("创建基础目录时发生异常：", e);
            throw new ZookeeperException();
        }
    }

    /**
     * 判断节点是否存在
     *
     * @param zk      zk实例
     * @param node    节点路劲
     * @param watcher watcher
     * @return ture 存在 | false 不存在
     */
    public static boolean exists(ZooKeeper zk, String node, Watcher watcher) {
        try {
            return zk.exists(node, watcher) != null;
        } catch (KeeperException | InterruptedException e) {
            log.error("判断节点[{}]是否存在是发生异常", node, e);
            throw new ZookeeperException(e);
        }
    }

    /**
     * 关闭zookeeper的方法
     *
     * @param zooKeeper zooKeeper实例
     */
    public static void close(ZooKeeper zooKeeper) {
        try {
            zooKeeper.close();
        } catch (InterruptedException e) {
            log.error("关闭zookeeper时发生问题：", e);
            throw new ZookeeperException();
        }
    }

    /**
     * 查询一个节点的子元素
     *
     * @param zooKeeper   zk实例
     * @param serviceNode 服务节点
     * @return 子元素列表
     */
    public static List<String> getChildren(ZooKeeper zooKeeper, String serviceNode, Watcher watcher) {
        try {
            // 这里调用了 zooKeeper 原生 api
            return zooKeeper.getChildren(serviceNode, watcher);
        } catch (KeeperException | InterruptedException e) {
            log.error("获取节点【{}】的子元素时发生异常.", serviceNode, e);
            throw new ZookeeperException(e);
        }
    }
}
