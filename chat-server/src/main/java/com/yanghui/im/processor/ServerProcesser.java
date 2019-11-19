package com.yanghui.im.processor;

import com.yanghui.im.bean.msg.ProtoMsg;
import com.yanghui.im.server.Session;

/**
 * 操作类
 */
public interface ServerProcesser {

    ProtoMsg.HeadType type();

    boolean action(Session session, ProtoMsg.Message proto);

}
