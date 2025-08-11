package com.ydlclass;


import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;


/**
 * @author QHN
 * @date 2025/08/11
 */
public class ZookeeperTest {

    // 创建一个 zooKeeper 变量，存放客户端连接对象
    ZooKeeper zooKeeper;
    // 创建一个计数器为 1 的1闭锁，用来让主线程等待 zooKeeper 客户端连接成功再继续执行
    CountDownLatch countDownLatch = new CountDownLatch(1);

    // 创建 zooKeeper 客户端连接。
    @Before
    public void createZk(){

        // 定义 zooKeeper 服务端的地址 (ip：端口)
        String connectString = "127.0.0.1:2181";
        /*
        如果是 zooKeeper 集群，用逗号隔开多个地址
        String connectString = "192.168.126.129:2181, 192.168.126.132:2181, 192.168.126.133:2181";
        */

        // 定义连接会话超时时间，单位是毫秒
        // 如果这个时间内 zookeeper 服务端没有收到客户端的心跳，会认为客户端掉线。
        int timeout = 10000;

        try {
            zooKeeper = new ZooKeeper(connectString, timeout, event -> {
                // 只有连接成功才放行
                if(event.getState() == Watcher.Event.KeeperState.SyncConnected){
                    System.out.println("客户端已经连接成功。");
                    countDownLatch.countDown();
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 测试创建永久节点
    @Test
    public void testCreatePNode(){
        try {
            // 等待连接成功
            countDownLatch.await();

            // 将节点 /ydlclass 的数据改为 “hi”，-1 表示不检查版本号，直接写入
//            zooKeeper.setData("/ydlclass","hi".getBytes(),-1);

            /*
             参数 1：要创建的节点路径
             参数 2：节点保存的数据，必须是字节数组
             参数 3：访问权限控制。这里表示完全开放权限，任何连接的客户端均可以操作 ZNode
             参数 4：节点类型：这里表示持久化节点
             */
            String result = zooKeeper.create("/ydlclass", "hello".getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            System.out.println("result = " + result);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 如果 zookeeper 不为空，关闭与服务端的连接
            try {
                if(zooKeeper != null){
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 测试删除持久化节点
    @Test
    public void testDeletePNode(){
        try {
            /*
                参数 1：要删除的节点  参数 2：版本号
                zookeeper 每个节点都有版本号，每次更新数据/结构都会加 1。乐观锁机制。
                删节点时，传入版本号。zookeeper 先检查版本号是否一致，一致才删除。
                如果传入的版本号为 -1，表示忽略版本号，直接删除节点。
             */
            zooKeeper.delete("/ydlclass",-1);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if(zooKeeper != null){
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testExistsPNode(){
        try {
            // 检查节点是否存在。存在返回一个 stat 对象，不存在返回 null。
            Stat stat = zooKeeper.exists("/ydlclass", null);

            zooKeeper.setData("/ydlclass","hi".getBytes(),-1);

            // 当前节点的数据版本
            int version = stat.getVersion();
            System.out.println("version = " + version);
            // 当前节点的 acl 访问控制列表版本号
            int aversion = stat.getAversion();
            System.out.println("aversion = " + aversion);
            // 当前子节点数据的版本
            int cversion = stat.getCversion();
            System.out.println("cversion = " + cversion);

        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if(zooKeeper != null){
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @Test
    public void testWatcher(){
        try {
            // 以下三个方法可以注册watcher，可以直接new一个新的watcher，
            // 也可以使用true来选定默认的watcher
            zooKeeper.exists("/ydlclass", true);
//            zooKeeper.getChildren();
//            zooKeeper.getData();

            while(true){
                Thread.sleep(1000);
            }

        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if(zooKeeper != null){
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



}
