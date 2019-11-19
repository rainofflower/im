package com.yanghui.im.processor;

import com.yanghui.im.bean.User;
import com.yanghui.im.bean.msg.ProtoMsg;
import com.yanghui.im.constant.ProtoInstant;
import com.yanghui.im.protoBuilder.LoginResponseBuilder;
import com.yanghui.im.server.LocalSession;
import com.yanghui.im.server.Session;
import com.yanghui.im.server.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoginProcesser extends AbstractServerProcesser {

    @Autowired
    private LoginResponseBuilder loginResponseBuilder;

    @Autowired
    private SessionManager sessionManager;

    public ProtoMsg.HeadType type() {
        return ProtoMsg.HeadType.LOGIN_REQUEST;
    }

    public boolean action(Session session, ProtoMsg.Message proto) {
        // 取出token验证
        ProtoMsg.LoginRequest info = proto.getLoginRequest();
        long seqNo = proto.getSequence();

        User user = User.fromMsg(info);

        //检查用户
        boolean isValidUser = checkUser(user);
        if (!isValidUser) {
            ProtoInstant.ResultCodeEnum resultcode =
                    ProtoInstant.ResultCodeEnum.NO_TOKEN;
            //构造登录失败的报文
            ProtoMsg.Message response =
                    loginResponseBuilder.loginResponse(resultcode, seqNo, "-1");
            //发送登录失败的报文
            session.writeAndFlush(response);
            log.info("登录失败:" + user);
            return false;
        }

        LocalSession localSession = (LocalSession)session;
        localSession.setManager(sessionManager);
        localSession.setUser(user);

        localSession.bind();
        sessionManager.addSession(localSession);

        //登录成功
        ProtoInstant.ResultCodeEnum resultcode =
                ProtoInstant.ResultCodeEnum.SUCCESS;
        //构造登录成功的报文
        ProtoMsg.Message response =
                loginResponseBuilder.loginResponse(
                        resultcode, seqNo, localSession.getSessionId());
        //发送登录成功的报文
        session.writeAndFlush(response);
        log.info("登录成功:" + user);
        return true;
    }

    private boolean checkUser(User user) {

        if (sessionManager.hasLogin(user)) {
            return false;
        }

        //校验用户,比较耗时的操作,需要100 ms以上的时间
        //方法1：调用远程用户restfull 校验服务
        //方法2：调用数据库接口校验

        return true;

    }

}
