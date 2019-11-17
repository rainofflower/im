package com.yanghui.im.processor;

import com.yanghui.im.bean.msg.ProtoMsg;
import com.yanghui.im.server.ServerSession;
import com.yanghui.im.server.SessionMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 发送消息
 */
@Slf4j
@Service
public class ChatRedirectProcesser extends AbstractServerProcesser {
    @Override
    public ProtoMsg.HeadType type() {
        return ProtoMsg.HeadType.MESSAGE_REQUEST;
    }

    @Override
    public boolean action(ServerSession fromSession, ProtoMsg.Message proto) {
        // 聊天处理
        ProtoMsg.MessageRequest msg = proto.getMessageRequest();
        log.info("chatMsg | from="
                + msg.getFrom()
                + " , to=" + msg.getTo()
                + " , content=" + msg.getContent());
        // 获取接收方的chatID
        String to = msg.getTo();
        // int platform = msg.getPlatform();
        List<ServerSession> toSessions = SessionMap.inst().getSessionsBy(to);
        if (toSessions == null) {
            //接收方离线
            log.info("[" + to + "] 不在线，发送失败!");
        }
        else {
            for(ServerSession session : toSessions) {
                // 将IM消息发送到接收方
                session.writeAndFlush(proto);
            }
        }
        return true;
    }

}
