package com.yanghui.im.handler;

import com.yanghui.im.bean.msg.ProtoMsg;
import com.yanghui.im.concurrent.CallbackExecutor;
import com.yanghui.im.concurrent.CallbackTask;
import com.yanghui.im.processor.LoginProcesser;
import com.yanghui.im.server.LocalSession;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 处理登录
 */
@Slf4j
@Service
@ChannelHandler.Sharable
public class LoginRequestHandler extends ChannelInboundHandlerAdapter {

    @Autowired
    private LoginProcesser loginProcesser;

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
        if (null == msg
                || !(msg instanceof ProtoMsg.Message)) {
            super.channelRead(ctx, msg);
            return;
        }

        ProtoMsg.Message pkg = (ProtoMsg.Message) msg;

        //取得请求类型
        ProtoMsg.HeadType headType = pkg.getType();

        if (!headType.equals(loginProcesser.type())) {
            super.channelRead(ctx, msg);
            return;
        }

        LocalSession localSession = new LocalSession(ctx.channel());
        //异步任务，处理登录的逻辑
        CallbackExecutor.getInstance().execute(new CallbackTask<Boolean>() {

            public Boolean call() throws Exception {
                boolean r = loginProcesser.action(localSession, pkg);
                return r;
            }

            //异步任务返回
            public void onSuccess(Boolean r) {
                if (r) {
                    ctx.pipeline().remove(LoginRequestHandler.this);
                } else {
                    LocalSession.closeSession(ctx);
                }
            }
            //异步任务异常
            public void onFailure(Throwable t) {
                log.error("登录失败,error:{}",t);
                LocalSession.closeSession(ctx);
            }
        });
    }
}
