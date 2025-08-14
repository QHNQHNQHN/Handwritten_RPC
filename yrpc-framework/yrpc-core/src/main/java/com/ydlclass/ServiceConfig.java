package com.ydlclass;

/**
 * @author QHN
 * @date 2025/08/13
 */
public class ServiceConfig<T> {
    // 接口
    private Class<?> interfaceProvider;
    // 具体的实现
    private Object ref;
    // 分组信息
    private String group = "default";
    
    public Class<?> getInterface() {
     return interfaceProvider;
    }

    public void setInterface(Class<?> interfaceProvider) {
     this.interfaceProvider = interfaceProvider;
    }

    public Object getRef() {
     return ref;
    }

    public void setRef(Object ref) {
     this.ref = ref;
    }
    
    public void setGroup(String group) {
        this.group = group;
    }
    
    public String getGroup() {
        return group;
    }
}
