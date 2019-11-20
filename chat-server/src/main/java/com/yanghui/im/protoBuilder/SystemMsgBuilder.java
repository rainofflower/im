package com.yanghui.im.protoBuilder;

import com.yanghui.im.bean.msg.ProtoMsg;

public class SystemMsgBuilder {

    public static ProtoMsg.Message buildMsgRequest(
            ProtoMsg.Message msg, String content) {
        ProtoMsg.Message.Builder mb = ProtoMsg.Message.newBuilder()
                .setType(ProtoMsg.HeadType.MESSAGE_NOTIFICATION)  //设置消息类型
                .setSequence(msg.getSequence())           //设置应答流水
                .setSessionId(msg.getSessionId());
        ProtoMsg.MessageNotification.Builder rb =
                ProtoMsg.MessageNotification
                        .newBuilder()
                        .setContent(content);
        mb.setNotification(rb.build());
        return mb.build();
    }
}
