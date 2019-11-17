package com.yanghui.im.concurrent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CompositeThreadPoolConfig {

    private int cpus = Runtime.getRuntime().availableProcessors();

    private int corePoolSize = 2;

    private int maxPoolSize = cpus << 1;

    private int queueCapacity = 2000;

    private long keepAliveSeconds = 300;

    public ThreadPoolExecutor threadPoolExecutor(){
        ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity));
        executor.prestartAllCoreThreads();
        return executor;
    }
}
