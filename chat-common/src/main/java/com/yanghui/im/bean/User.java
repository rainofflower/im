package com.yanghui.im.bean;

import com.yanghui.im.bean.msg.ProtoMsg;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class User {

    String uid;
    String devId;
    String token;
    String nickName = "nickName";
    PLATTYPE platform = PLATTYPE.WINDOWS;
    private String sessionId;

    // windows,mac,android, ios, web , other
    public enum PLATTYPE {
        WINDOWS, MAC, ANDROID, IOS, WEB, OTHER;

    }


    public void setPlatform(int platform) {
        PLATTYPE[] values = PLATTYPE.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].ordinal() == platform) {
                this.platform = values[i];
            }
        }

    }


    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", devId='" + devId + '\'' +
                ", token='" + token + '\'' +
                ", nickName='" + nickName + '\'' +
                ", platform=" + platform +
                '}';
    }

    public static User fromMsg(ProtoMsg.LoginRequest info) {
        User user = new User();
        user.uid = new String(info.getUid());
        user.devId = new String(info.getDeviceId());
        user.token = new String(info.getToken());
        user.setPlatform(info.getPlatform());
        log.info("登录中: {}", user.toString());
        return user;

    }

}
