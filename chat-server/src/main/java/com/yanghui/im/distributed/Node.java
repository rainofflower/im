package com.yanghui.im.distributed;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分布式服务节点
 */
@Component
@Data
@Slf4j
public class Node implements Comparable<Node>, Serializable {


    private long id;

    @Value("${service.host}")
    private String host;

    @Value("${service.port}")
    private int port;

    private final AtomicInteger balance = new AtomicInteger(0);

    @Value("${service.register.rootPath}")
    private transient String serviceRegisterRootPath;

    @Value("${service.register.prefix}")
    private transient String serviceRegisterPathPrefix;

    @Autowired
    private transient CuratorFramework client;

    //@PostConstruct //改成先启动netty，再注册
    public void register() throws Exception {
        try {
            log.info("节点正在注册到zookeeper...");
            Stat rootPathStat = client.checkExists()
                    .forPath(serviceRegisterRootPath);
            if(rootPathStat == null){
                //服务注册根节点，需要创建一个永久的节点
                client.create()
                        .creatingParentsIfNeeded()
                        .withProtection()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(serviceRegisterRootPath);
            }
            String registerPathPrefix = serviceRegisterRootPath + "/" + serviceRegisterPathPrefix;
            String nodeJson = JSONObject.toJSONString(Node.this);
            //当前服务注册一个临时节点
            String registerPath = client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(registerPathPrefix, nodeJson.getBytes("UTF-8"));
            String idStr = registerPath.substring(registerPathPrefix.length());
            id = Long.parseLong(idStr);
            log.info("节点注册到zookeeper成功");
        } catch (Exception e) {
            log.error("服务注册失败，error: ",e);
            throw e;
        }
    }

    @Override
    public int compareTo(Node o) {
        int weight1 = this.balance.get();
        int weight2 = o.getBalance().get();
        if(weight1 > weight2){
            return 1;
        }
        else if(weight1 < weight2){
            return -1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return port == node.port &&
                Objects.equals(host, node.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, host, port);
    }
}
