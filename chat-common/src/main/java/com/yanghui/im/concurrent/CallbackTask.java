package com.yanghui.im.concurrent;

import java.util.concurrent.Callable;

/**
 * 异步回调任务接口
 */
public interface CallbackTask<T> extends Callable<T> {

    void onSuccess(T result);

    void onFailure(Throwable t);
}
