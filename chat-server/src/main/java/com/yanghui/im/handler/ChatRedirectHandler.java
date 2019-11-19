package com.yanghui.im.handler;

import com.yanghui.im.bean.msg.ProtoMsg;
import com.yanghui.im.concurrent.SimpleThreadPool;
import com.yanghui.im.processor.ChatRedirectProcesser;
import com.yanghui.im.server.LocalSession;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息转发
 */
@Slf4j
@ChannelHandler.Sharable
@Service
public class ChatRedirectHandler extends ChannelInboundHandlerAdapter {

    @Autowired
    ChatRedirectProcesser chatRedirectProcesser;

    /**
     * 收到消息
     */
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        //判断消息实例
        if (null == msg || !(msg instanceof ProtoMsg.Message)) {
            super.channelRead(ctx, msg);
            return;
        }

        //判断消息类型
        ProtoMsg.Message pkg = (ProtoMsg.Message) msg;
        ProtoMsg.HeadType headType = ((ProtoMsg.Message) msg).getType();
        if (!headType.equals(chatRedirectProcesser.type())) {
            super.channelRead(ctx, msg);
            return;
        }
        ProtoMsg.MessageRequest messageRequest = pkg.getMessageRequest();
        if(!messageRequest.getRedirect()){
            //非转发消息，先要简单验证channel是否与本地session绑定
            LocalSession session = LocalSession.getSession(ctx);
            if (null == session) {
                log.error("用户尚未登录，不能发送消息");
                return;
            }
        }

        //异步处理IM消息转发的逻辑
        SimpleThreadPool.getInstance().execute(() ->
            chatRedirectProcesser.action(null,pkg)
        );
    }
}

