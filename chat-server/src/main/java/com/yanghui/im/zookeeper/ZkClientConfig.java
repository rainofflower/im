package com.yanghui.im.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZkClientConfig {

    @Value("${zookeeper.connectString}")
    private String connectString;

    @Bean
    public CuratorFramework client(){
        CuratorFramework client = ZkClientFactory.createSimple(connectString);
        client.start();
        return client;
    }

}
