package com.yanghui.im.handler;

import com.yanghui.im.bean.User;
import com.yanghui.im.bean.msg.ProtoMsg;
import com.yanghui.im.client.ClientSession;
import com.yanghui.im.protoBuilder.HeartBeatMsgBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 心跳检测 - 客户端定时发送心跳包
 */
@Slf4j
//@ChannelHandler.Sharable
//@Service
public class HeartBeatClientHandler extends ChannelInboundHandlerAdapter {
    /**
     * 心跳的时间间隔，单位为s
     * 一般要比服务端的空闲检测时间的一半还短一些，可以直接定义为空闲检测时间间隔的1/3
     */
    public static final int DEFAULT_HEARTBEAT_INTERVAL = 50;

    private int heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;

    public HeartBeatClientHandler(){}

    public HeartBeatClientHandler(int heartbeatInterval){
        this.heartbeatInterval = heartbeatInterval;
    }

    //在Handler被加入到Pipeline时，开始发送心跳
    @Override
    public void handlerAdded(ChannelHandlerContext ctx)
            throws Exception {
        ClientSession session = ClientSession.getSession(ctx);
        User user = session.getUser();
        HeartBeatMsgBuilder builder =
                new HeartBeatMsgBuilder(user, session);

        ProtoMsg.Message message = builder.buildMsg();
        //发送心跳
        heartBeat(ctx, message);
    }

    //使用定时器，发送心跳报文
    public void heartBeat(ChannelHandlerContext ctx,
                          ProtoMsg.Message heartbeatMsg) {
        ctx.executor().schedule(() -> {

            if (ctx.channel().isActive()) {
                //log.info(" 发送 HEART_BEAT  消息 to server");
                ctx.writeAndFlush(heartbeatMsg);

                //递归调用，发送下一次的心跳
                heartBeat(ctx, heartbeatMsg);
            }

        }, heartbeatInterval, TimeUnit.SECONDS);
    }

    /**
     * 接受到服务器的心跳回写
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //判断消息实例
        if (null == msg || !(msg instanceof ProtoMsg.Message)) {
            super.channelRead(ctx, msg);
            return;
        }

        //判断类型
        ProtoMsg.Message pkg = (ProtoMsg.Message) msg;
        ProtoMsg.HeadType headType = pkg.getType();
        if (headType.equals(ProtoMsg.HeadType.HEART_BEAT)) {
            //log.info(" 收到回写的 HEART_BEAT  消息 from server");
            return;
        } else {
            super.channelRead(ctx, msg);
        }

    }

}
