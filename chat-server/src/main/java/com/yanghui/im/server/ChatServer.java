package com.yanghui.im.server;

import com.yanghui.im.codec.ProtoBufDecoder;
import com.yanghui.im.codec.ProtoBufEncoder;
import com.yanghui.im.handler.ChatRedirectHandler;
import com.yanghui.im.handler.HeartBeatServerHandler;
import com.yanghui.im.handler.LoginRequestHandler;
import com.yanghui.im.handler.ServerExceptionHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * IM 服务器
 * 维持 TCP 长连接
 */
@Data
@Slf4j
@Service
public class ChatServer{

    @Value("${service.port}")
    private int port;

    @Value("${service.netty.boss-threads}")
    private int bossThreads;

    @Value("${service.netty.worker-threads}")
    private int workerThreads;

    @Value("${service.heartbeat.read-idle-gap}")
    private int readIdleGap;

    @Autowired
    private LoginRequestHandler loginRequestHandler;

    @Autowired
    private ChatRedirectHandler chatRedirectHandler;

    @Autowired
    private ServerExceptionHandler serverExceptionHandler;

    private Channel channel;

    private NioEventLoopGroup bossGroup = new NioEventLoopGroup(bossThreads);

    private NioEventLoopGroup workerGroup = new NioEventLoopGroup(workerThreads);

    /**
     * 服务端异步启动
     */
    public ChannelFuture run(){
        ServerBootstrap b = new ServerBootstrap();
        ChannelFuture f = b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new ProtoBufDecoder())
                                .addLast(new ProtoBufEncoder())
                                .addLast(new HeartBeatServerHandler(readIdleGap))
                                .addLast(loginRequestHandler)
                                .addLast(chatRedirectHandler)
                                .addLast(serverExceptionHandler);
                    }
                })
                .bind(port);
        this.channel = f.channel();
        return f;
    }

    /**
     * 等待Netty服务端通道关闭
     */
    public void waitClose(){
        try {
            this.channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            //优雅关闭EventLoopGroup，释放资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
