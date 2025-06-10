package com.ydlclass.utils.zookeeper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 封装 Zookeeper 节点信息的简单数据对象（POJO）
 */
// lombok
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZookeeperNode {
    private String nodePath;  //节点路径
    private byte[] data;      //节点数据
}
