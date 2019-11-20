package com.yanghui.im.processor;

import com.alibaba.fastjson.JSONObject;
import com.yanghui.im.bean.msg.ProtoMsg;
import com.yanghui.im.distributed.ServiceRouter;
import com.yanghui.im.protoBuilder.ChatMsgBuilder;
import com.yanghui.im.protoBuilder.SystemMsgBuilder;
import com.yanghui.im.server.DistributedSession;
import com.yanghui.im.server.Session;
import com.yanghui.im.server.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 发送/转发消息
 */
@Slf4j
@Service
public class ChatRedirectProcesser extends AbstractServerProcesser {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private ServiceRouter serviceRouter;

    public ProtoMsg.HeadType type() {
        return ProtoMsg.HeadType.MESSAGE_REQUEST;
    }

    @Override
    public boolean action(Session session, ProtoMsg.Message proto) {
        ProtoMsg.MessageRequest msg = proto.getMessageRequest();
        log.info("chatMsg 来源["+(msg.getRedirect() ? "其它节点转发" : "客户端直接发送")+"] from="
                + msg.getFrom()
                + " , to=" + msg.getTo()
                + " , content=" + msg.getContent() + " , 发送源sessionId=" + proto.getSessionId());
        // int platform = msg.getPlatform();
        if(msg.getRedirect()){
            //其它节点转发过来的数据，直接去localSessionMap中查询session并发送数据给客户端
            List<String> sessionList = JSONObject.parseObject(msg.getToSessionIds(), List.class);
            sendMessageWithLocalSession(sessionList, proto);
        }else{
            Session localSession = sessionManager.getLocalSession(proto.getSessionId());
            if(localSession == null){
                //用户刚刚才发送消息过来，这会儿连接被移除了，可能是session短期内就超时或其它异常
                return false;
            }
            if(!sessionManager.valid(localSession.getSessionId())){
                //发送消息的用户session过期
                String info = "用户session过期，sessionId: "+proto.getSessionId()+"， userId: "+msg.getFrom();
                log.info(info);
                localSession.writeAndFlush(SystemMsgBuilder.buildMsgRequest(proto, info));
                sessionManager.removeSession(localSession, true);
                return false;
            }
            //在session存活期间用户有发送消息则进行session续期
            sessionManager.refreshSession(localSession);
            /*
             * 客户端发送的数据，需要根据接收方的userId去redis集群中获取所有的分布式session，
             * 然后根据nodeId分组，将数据发送给对应的node（如果某些session刚好就是当前node,则直接走上面那个分支的逻辑）
             */
            //接收方的userId
            String to = msg.getTo();
            Set<DistributedSession> toSessions = sessionManager.getSessionsByUserId(to);
            if(!CollectionUtils.isEmpty(toSessions)){
                //按node分组
                Map<Long, List<DistributedSession>> groupSession = toSessions.stream()
                        .collect(Collectors.groupingBy(DistributedSession::getNodeId));
                for(Map.Entry<Long, List<DistributedSession>> group: groupSession.entrySet()){
                    Long nodeId = group.getKey();
                    List<DistributedSession> sessions = group.getValue();
                    if(nodeId == sessionManager.getNode().getId()){
                        //当前节点
                        List<String> sessionList = new LinkedList<>();
                        for(DistributedSession s : sessions){
                            sessionList.add(s.getSessionId());
                        }
                        sendMessageWithLocalSession(sessionList, proto);
                    }else{
                        //集群中的其它节点
                        List<String> sessionList = new LinkedList<>();
                        for(DistributedSession s : sessions){
                            sessionList.add(s.getSessionId());
                        }
                        ProtoMsg.Message redirectMsgRequest = ChatMsgBuilder.buildRedirectMsgRequest(proto, sessionList);
                        serviceRouter.writeAndFlush(nodeId, redirectMsgRequest);
                    }
                }
            }
        }
        return true;
    }

    /**
     * 一条消息发送给本地维护的多个session
     * @param sessions
     * @param proto
     */
    private void sendMessageWithLocalSession(List<String> sessions, ProtoMsg.Message proto){
        for(String sessionId : sessions){
            Session localSession = sessionManager.getLocalSession(sessionId);
            if(localSession == null){
                //接收方离线
                log.info("[" + sessionId + "] 不在线，发送失败!");
            }else{
                if(sessionManager.valid(localSession.getSessionId())){
                    //session有效
                    localSession.writeAndFlush(proto);
                }else{
                    //session过期
                    sessionManager.removeSession(localSession, true);
                }
            }
        }
    }

}
