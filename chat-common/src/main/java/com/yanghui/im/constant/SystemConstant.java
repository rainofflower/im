package com.yanghui.im.constant;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class SystemConstant {

    public static String host;

    static {
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }
}
