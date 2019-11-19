package com.yanghui.im.server;

import com.yanghui.im.bean.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务器Socket Session会话
 */
@Data
@Slf4j
public class LocalSession extends AbstractSession{

    public static final AttributeKey<String> KEY_USER_ID =
            AttributeKey.valueOf("key_user_id");

    public static final AttributeKey<LocalSession> SESSION_KEY =
            AttributeKey.valueOf("session_key");

    //用户
    private User user;

    //登录状态
    private boolean isLogin = false;

    private Channel channel;


    public LocalSession(Channel channel) {
        this.channel = channel;
        setSessionId(super.buildSessionId());
    }


    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return channel.writeAndFlush(msg);
    }

    //关闭连接
    public ChannelFuture close() {
        return channel.close();
    }

    //反向导航
    public static LocalSession getSession(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        return channel.attr(LocalSession.SESSION_KEY).get();
    }

    //关闭连接
    public static void closeSession(ChannelHandlerContext ctx) {
        LocalSession session =
                ctx.channel().attr(LocalSession.SESSION_KEY).get();
        if (null != session) {
            session.close();
        }
    }

    //和channel 通道实现双向绑定
    public void bind() {
        log.info(" LocalSession 绑定会话 " + channel.remoteAddress());
        channel.attr(LocalSession.SESSION_KEY).set(this);
    }

    public void unbind() {
        this.close();
    }

    public void setUser(User user) {
        this.user = user;
        user.setSessionId(getSessionId());
    }

}
