package com.ydlclass.netty;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;


/**
 * @author QHN
 * @date 2025/08/11
 * 公共类 MyWatcher 实现了 zooKeeper 的 watcher 接口。
 * watcher 是事件监听接口，用来接收服务端推送的事件
 */
public class MyWatcher implements Watcher {
     @Override
     // 参数 event 是事件对象
     public void process(WatchedEvent event) {
         // 判断事件类型,连接类型的事件
         // 1. 连接相关事件
         if(event.getType() == Event.EventType.None){
             // 1.1 SyncConnected 表示客户端成功连接到 zookeeper
              if(event.getState() == Event.KeeperState.SyncConnected){
                  System.out.println("zookeeper连接成功");
              // 1.2 AuthFailed 表示 zookeeper 认证失败，可能是权限/密码错误
              } else if (event.getState() == Event.KeeperState.AuthFailed){
                  System.out.println("zookeeper认证失败");
              // 1.3 Disconnected 表示 客户端与 zookeeper 连接断开
              } else if (event.getState() == Event.KeeperState.Disconnected){
                  System.out.println("zookeeper断开连接");
              }
         // 2. NodeCreated 表示一个节点被创建了
         } else if (event.getType() == Event.EventType.NodeCreated){
             System.out.println(event.getPath() + "被创建了");
         // 3. NodeDeleted 表示一个节点被删除了
         } else if (event.getType() == Event.EventType.NodeDeleted){
             System.out.println(event.getPath() + "被删除了了");
         // 4. NodeDataChanged 表示某个节点的数据内容发生了变化
         } else if (event.getType() == Event.EventType.NodeDataChanged){
             System.out.println(event.getPath() + "节点的数据改变了");
         // 5. NodeChildrenChanged 表示某个节点的子节点列表发生了变化
         } else if (event.getType() == Event.EventType.NodeChildrenChanged){
             System.out.println(event.getPath() + "子节点发生了变化");
         }
     }
}
