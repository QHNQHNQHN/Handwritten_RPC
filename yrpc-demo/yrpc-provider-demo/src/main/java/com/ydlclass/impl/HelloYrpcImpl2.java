package com.ydlclass.impl;

import com.ydlclass.HelloYrpc2;
import com.ydlclass.annotation.YrpcApi;


/**
 * @author QHN
 * @date 2025/08/12
 */
@YrpcApi
public class HelloYrpcImpl2 implements HelloYrpc2 {
    @Override
    public String sayHi(String msg) {
        return "hi consumer:" + msg;
    }
}
