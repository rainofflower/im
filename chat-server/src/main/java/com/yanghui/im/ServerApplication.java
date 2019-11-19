package com.yanghui.im;

import com.yanghui.im.distributed.ServiceRouter;
import com.yanghui.im.server.ChatServer;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class ServerApplication implements ApplicationRunner{

    public static void main(String... args){
        SpringApplication.run(ServerApplication.class, args);
    }

    @Autowired
    private ServiceRouter serviceRouter;

    @Autowired
    private ChatServer chatServer;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ChannelFuture f = chatServer.run();
        f.addListener((ChannelFuture future)-> {
            if(future.isSuccess()){
                log.info("IM 服务已启动,端口号：{}",future.channel().localAddress());
                serviceRouter.init();
            }
            else{
                log.error("IM 服务启动失败，error:{}",future.cause());
            }
        });
        chatServer.waitClose();
    }

}
