package com.yanghui.im.distributed;

import com.alibaba.fastjson.JSONObject;
import com.yanghui.im.codec.ProtoBufDecoder;
import com.yanghui.im.codec.ProtoBufEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分布式节点之间通信
 *
 * 相同的用户可以使用一定的负载均衡算法命中同一台服务器
 */
@Slf4j
@Data
@Service
public class ServiceRouter {

    private final ConcurrentMap<Long, NodeClient> serviceMap = new ConcurrentHashMap<>();

    @Autowired
    private Node currentNode;

    private int pathPrefixLength;

    @Autowired
    private CuratorFramework client;

    @Value("${cluster.client.threads}")
    private int nodeClientThreads;

    private EventLoopGroup eventExecutors;

    @Value("${cluster.client.heartbeat.interval}")
    private int heartbeatInterval;

    @Value("${cluster.client.retry.maxCount}")
    private int retryMaxCount;

    /**
     * 初始化集群节点连接
     * 监听集群事件
     */
    public void init(){
        eventExecutors = new NioEventLoopGroup(nodeClientThreads);
        String serviceRegisterRootPath = currentNode.getServiceRegisterRootPath();
        pathPrefixLength = (serviceRegisterRootPath + "/" + currentNode.getServiceRegisterPathPrefix()).length();
        try {
            //监听集群节点增加和删除事件
            PathChildrenCache childrenCache = new PathChildrenCache(client, serviceRegisterRootPath, true);
            childrenCache.getListenable().addListener((CuratorFramework client, PathChildrenCacheEvent event) -> {
                ChildData data = event.getData();
                if (data == null || data.getData() == null || data.getData().length == 0) {
                    return;
                }
                switch (event.getType()) {
                    case CHILD_ADDED:
                        processNodeAdd(data);
                        break;
                    case CHILD_UPDATED:

                        break;
                    case CHILD_REMOVED:
                        processNodeRemove(data);
                        break;
                    default:
                        break;
                }
            });
            childrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        } catch (Exception e) {
            log.error("初始化与远程节点的通信失败，error:",e);
        }
    }


    private void processNodeAdd(ChildData data){
        Node node = JSONObject.parseObject(new String(data.getData(), Charset.forName("UTF-8")), Node.class);
        String path = data.getPath();
        String idStr = path.substring(pathPrefixLength);
        long id = Long.parseLong(idStr);
        node.setId(id);
        log.info("收到增加的节点 data：{}, path: {}",JSONObject.toJSONString(node), path);
        if(node.equals(currentNode)){
            //本地节点无需建立连接
            return;
        }
        NodeClient client = serviceMap.get(id);
        if(client != null && client.getNode().equals(node)){
            log.info("节点重复添加，data:{}, path:{}",JSONObject.toJSONString(node), path);
            return;
        }
        if(client != null){
            //关闭老的连接
            client.close();
        }
        NodeClient nodeClient = new NodeClient(node, eventExecutors);
        //异步处理
        nodeClient.connect();
    }

    private void processNodeRemove(ChildData data){
        Node node = JSONObject.parseObject(new String(data.getData(), Charset.forName("UTF-8")), Node.class);
        String path = data.getPath();
        String idStr = path.substring(pathPrefixLength);
        long id = Long.parseLong(idStr);
        node.setId(id);
        log.info("收到删除的节点 data：{}, path: {}",JSONObject.toJSONString(node), path);
        NodeClient client = serviceMap.get(id);
        if(client != null){
            client.setRemoved(true);
            client.close();
        }
    }

    /**
     * 异步将数据发送给指定节点
     * @param nodeId
     * @param msg
     * @return
     */
    public Future writeAndFlush(Long nodeId, Object msg){
        NodeClient nodeClient = serviceMap.get(nodeId);
        if(nodeClient == null){
            String error = "节点:id = "+currentNode.getId()+" 与节点:id = "+nodeId+" 的连接异常";
            log.error(error);
            DefaultPromise promise = new DefaultPromise(eventExecutors.next());
            promise.setFailure(new RuntimeException(error));
            return promise;
        }else{
            return nodeClient.writeAndFlush(msg);
        }
    }


    /**
     * 远程节点
     * 包含远程节点的信息，以及与它们的连接通道
     */
    @Data
    private class NodeClient {

        private Channel channel;

        private Node node;

        private EventLoopGroup eventExecutors;

        private volatile boolean removed;

        private final AtomicInteger retryCount = new AtomicInteger(0);

        public NodeClient(Node node, EventLoopGroup eventExecutors){
            this.node = node;
            this.eventExecutors = eventExecutors;
        }

        public void connect(){
            Bootstrap b = new Bootstrap();
            try {
                b.group(eventExecutors)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel channel) throws Exception {
                                channel.pipeline()
                                        .addLast("decoder", new ProtoBufDecoder())
                                        .addLast("encoder", new ProtoBufEncoder())
                                        .addLast("heartbeat", new HeartBeatNodeHandler(heartbeatInterval));
                            }
                        });
                log.info("节点 {} 开始连接远程节点：{}",currentNode.getHost(),node.getHost());
                ChannelFuture f = b.connect(node.getHost(), node.getPort());
                f.addListener(connectedListener);
            }catch (Exception e){
                log.error("节点 {} 连接远程节点 {} 失败，信息：{}",currentNode.getHost(),node.getHost(),e);
            }
        }

        /**
         * 连接关闭回调
         */
        GenericFutureListener<ChannelFuture> closedListener = (ChannelFuture f) ->{
            //this.eventExecutors.shutdownGracefully();
            serviceMap.remove(this.node.getId());
            log.info("节点 {} 与远程节点 {} 的连接已经断开并移除",currentNode.getHost(),node.getHost());
        };

        /**
         * 与远程节点建立连接之后回调
         * 成功就加入到serviceMap中
         * 失败则重试
         */
        GenericFutureListener<ChannelFuture> connectedListener = (ChannelFuture f) ->{
            if(f.isSuccess()){
                this.channel = f.channel();
                this.channel.closeFuture().addListener(closedListener);
                serviceMap.put(this.node.getId(), this);
                log.info("{} 连接远程节点 {} 成功",currentNode.getHost(),node.getHost());
            }
            else{
                log.error("{} 连接 {} 失败! 已重连 {} 次，最多重连 {} 次",
                        currentNode.getHost(), node.getHost(), this.retryCount.get(), retryMaxCount);
                //判断节点是否被移除，节点未移除才尝试重连
                if(!this.removed && this.retryCount.get() < retryMaxCount){
                    log.error("在10s之后准备尝试重连 {}!",node.getHost());
                    f.channel().eventLoop().schedule(
                            () -> {
                                this.connect();
                                this.retryCount.incrementAndGet();
                            },
                            10,
                            TimeUnit.SECONDS
                    );
                }
            }
        };

        /**
         * 异步关闭连接
         */
        public ChannelFuture close(){
            if(this.channel != null && this.channel.isActive()){
                return this.channel.closeFuture();
            }
            return null;
        }


        /**
         * 异步向集群中的远程节点发送消息
         * @param msg
         * @return
         */
        public ChannelFuture writeAndFlush(Object msg){
            if(this.channel == null || !this.channel.isActive()){
                log.error("信息发送失败,连接已关闭，等待重连");
            }
            return this.channel.writeAndFlush(msg);
        }

    }

}
