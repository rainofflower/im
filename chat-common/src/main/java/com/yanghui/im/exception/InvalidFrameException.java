package com.yanghui.im.exception;

/**
 * 非法报文异常
 **/
public class InvalidFrameException extends Exception {

    public InvalidFrameException(String s) {
        super(s);
    }
}
