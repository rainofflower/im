package com.yanghui.im.protoBuilder;

import com.alibaba.fastjson.JSONObject;
import com.yanghui.im.bean.msg.ProtoMsg;
import com.yanghui.im.constant.ProtoInstant;

import java.util.List;

public class ChatMsgBuilder {

    public static ProtoMsg.Message buildRedirectMsgRequest(
            ProtoMsg.Message msg, List<String> sessionIds) {
        ProtoMsg.Message.Builder mb = ProtoMsg.Message.newBuilder()
                .setType(ProtoMsg.HeadType.MESSAGE_REQUEST)  //设置消息类型
                .setSequence(msg.getSequence())           //设置应答流水
                .setSessionId(msg.getSessionId());
        ProtoMsg.MessageRequest mq = msg.getMessageRequest();
        ProtoMsg.MessageRequest.Builder rb =
                ProtoMsg.MessageRequest
                        .newBuilder()
                        .setMsgId(mq.getMsgId())
                        .setFrom(mq.getFrom())
                        .setTo(mq.getTo())
                        .setTime(mq.getTime())
                        .setMsgType(mq.getMsgType())
                        .setContent(mq.getContent())
                        .setUrl(mq.getUrl())
                        .setProperty(mq.getProperty())
                        .setFromNick(mq.getFromNick())
                        .setJson(mq.getJson())
                        //转发消息
                        .setRedirect(true)
                        .setToSessionIds(JSONObject.toJSONString(sessionIds))
                        .setContent(mq.getContent());
        mb.setMessageRequest(rb.build());
        return mb.build();
    }

    public static ProtoMsg.Message buildChatResponse(
            long seqId,
            ProtoInstant.ResultCodeEnum en) {
        ProtoMsg.Message.Builder mb = ProtoMsg.Message.newBuilder()
                .setType(ProtoMsg.HeadType.MESSAGE_RESPONSE)  //设置消息类型
                .setSequence(seqId);                 //设置应答流水，与请求对应
        ProtoMsg.MessageResponse.Builder rb =
                ProtoMsg.MessageResponse.newBuilder()
                        .setCode(en.getCode())
                        .setInfo(en.getDesc())
                        .setExpose(1);
        mb.setMessageResponse(rb.build());
        return mb.build();
    }


    /**
     * 登录应答 应答消息protobuf
     */
    public static ProtoMsg.Message buildLoginResponce(
            ProtoInstant.ResultCodeEnum en,
            long seqId) {
        ProtoMsg.Message.Builder mb = ProtoMsg.Message.newBuilder()
                .setType(ProtoMsg.HeadType.MESSAGE_RESPONSE)  //设置消息类型
                .setSequence(seqId);  //设置应答流水，与请求对应

        ProtoMsg.LoginResponse.Builder rb =
                ProtoMsg.LoginResponse.newBuilder()
                        .setCode(en.getCode())
                        .setInfo(en.getDesc())
                        .setExpose(1);

        mb.setLoginResponse(rb.build());
        return mb.build();
    }


}
