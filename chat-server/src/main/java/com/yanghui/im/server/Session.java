package com.yanghui.im.server;

import io.netty.util.concurrent.Future;

public interface Session {

    Future writeAndFlush(Object msg);

    void setAttribute(String key, Object value);

    Object getAttribute(String key);

    String getSessionId();

    void setSessionId(String id);

    boolean valid(); // bug 使用isValid() fastjson解析报错

    Future close();
}
