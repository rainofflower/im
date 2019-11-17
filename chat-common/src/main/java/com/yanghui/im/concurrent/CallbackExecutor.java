package com.yanghui.im.concurrent;

import com.google.common.util.concurrent.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.ExecutorService;

/**
 * 异步回调任务执行器
 */
public class CallbackExecutor {

    private CallbackExecutor(){}

    private static class CallbackExecutorHolder{
        static CallbackExecutor instance = new CallbackExecutor();
    }

    /**
     * 单例模式
     */
    public static CallbackExecutor getInstance(){
        return CallbackExecutorHolder.instance;
    }


    /**
     * 默认线程池
     */
    private ExecutorService jPool = new CompositeThreadPoolConfig().threadPoolExecutor();

    /**
     * guava线程池
     */
    private ListeningExecutorService gPool = MoreExecutors.listeningDecorator(jPool);

    /**
     * 异步执行任务并回调 CallbackTask中的 onSuccess/onFailure方法
     * @param task CallbackTask实例
     * @param <T> 任务返回结果类型
     */
    public <T> void execute(CallbackTask<T> task){
        ListenableFuture<T> future = gPool.submit(task);
        Futures.addCallback(future, new FutureCallback<T>() {
            public void onSuccess(@Nullable T result) {
                task.onSuccess(result);
            }

            public void onFailure(Throwable t) {
                task.onFailure(t);
            }
        },gPool);
    }
}
