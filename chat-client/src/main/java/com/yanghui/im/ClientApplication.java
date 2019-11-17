package com.yanghui.im;

import com.yanghui.im.client.CommandController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class ClientApplication {
    /**
     * @param args
     */
    public static void main(String[] args) {
        // 启动并初始化 Spring 环境及其各 Spring 组件
        ApplicationContext context =
                SpringApplication.run(ClientApplication.class, args);
        CommandController commandClient =
                context.getBean(CommandController.class);

        commandClient.initCommandMap();
        try {
            commandClient.startCommandThread();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
