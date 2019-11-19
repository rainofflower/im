package com.yanghui.im.server;

import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public abstract class AbstractSession implements Session {

    private String sessionId;

    /**
     * session中存储的session 变量属性值
     */
    private transient ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * session管理器
     */
    private transient Manager sessionManager;

    public String getSessionId(){
        return sessionId;
    }

    public void setSessionId(String id){
        this.sessionId = id;
    }

    public void setManager(Manager sessionManager){
        this.sessionManager = sessionManager;
    }

    public Manager getManager(){
        return sessionManager;
    }

    @Override
    public boolean isValid() throws RuntimeException{
        if(sessionManager == null){
            log.error("session管理器未设置，检查session是否正常失败");
            throw new RuntimeException("session管理器未设置！");
        }
        return sessionManager.isValid(sessionId);
    }

    public void setAttribute(String key, Object value){
        attributes.put(key, value);
    }

    public Object getAttribute(String key){
        return attributes.get(key);
    }

    public String buildSessionId() {
        String uuid = UUID.randomUUID().toString();
        return uuid.replaceAll("-", "");
    }

    @Override
    public Future close() {
        //子类可重写
        return null;
    }
}