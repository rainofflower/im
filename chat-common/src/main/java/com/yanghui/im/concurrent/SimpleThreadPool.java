package com.yanghui.im.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleThreadPool {

    private SimpleThreadPool(){}

    private static class SingletonHolder{
        static SimpleThreadPool instance = new SimpleThreadPool();
    }

    public static SimpleThreadPool getInstance(){
        return SingletonHolder.instance;
    }

    private ExecutorService pool = Executors.newFixedThreadPool(5);

    public ExecutorService getPool(){
        return pool;
    }

    public void execute(Runnable r){
        pool.execute(r);
    }

}
