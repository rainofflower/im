package com.yanghui.im.distributed;

import com.yanghui.im.bean.msg.ProtoMsg;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 心跳检测 - 分布式各节点之间定时发送心跳包
 */
@Slf4j
public class HeartBeatNodeHandler extends ChannelInboundHandlerAdapter {
    /**
     * 心跳的时间间隔，单位为s
     * 一般要比服务端的空闲检测时间的一半还短一些，可以直接定义为空闲检测时间间隔的1/3
     */
    public static final int DEFAULT_HEARTBEAT_INTERVAL = 50;

    private int heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;

    public HeartBeatNodeHandler(){}

    public HeartBeatNodeHandler(int heartbeatInterval){
        this.heartbeatInterval = heartbeatInterval;
    }

    //在Handler被加入到Pipeline时，开始发送心跳
    @Override
    public void handlerAdded(ChannelHandlerContext ctx)
            throws Exception {
        ProtoMsg.Message.Builder mb =
                ProtoMsg.Message
                        .newBuilder()
                        .setType(ProtoMsg.HeadType.HEART_BEAT)
                        .setSessionId("unknown")
                        .setSequence(-1);
        ProtoMsg.Message message =   mb.buildPartial();
        ProtoMsg.MessageHeartBeat.Builder lb =
                ProtoMsg.MessageHeartBeat.newBuilder()
                        .setSeq(0)
                        .setJson("{\"from\":\"imNode\"}")
                        .setUid("-1");
        message.toBuilder().setHeartBeat(lb).build();
        //发送心跳
        heartBeat(ctx, message);
    }

    //使用定时器，发送心跳报文
    public void heartBeat(ChannelHandlerContext ctx,
                          ProtoMsg.Message heartbeatMsg) {
        ctx.executor().schedule(() -> {

            if (ctx.channel().isActive()) {
                log.info(" 集群节点 发送 HEART_BEAT  消息 到其它节点");
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

            log.info(" 节点收到回写的 HEART_BEAT  消息 从其它节点");

            return;
        } else {
            super.channelRead(ctx, msg);

        }

    }

}
