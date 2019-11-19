package com.yanghui.im.server;

public interface Manager {

    void addSession(LocalSession session);

    Session getSession(String sessionId);

    boolean isValid(String sessionId);
}
