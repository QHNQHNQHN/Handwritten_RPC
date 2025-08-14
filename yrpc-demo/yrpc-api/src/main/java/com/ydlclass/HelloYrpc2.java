package com.ydlclass;


/**
 * @author QHN
 * @date 2025/08/12
 */
public interface HelloYrpc2 {

    /**
     * 通用接口，server和 client 都需要依赖
     * @param msg 发送的具体的消息
     * @return 返回的结果
     */
    String sayHi(String msg);
}
