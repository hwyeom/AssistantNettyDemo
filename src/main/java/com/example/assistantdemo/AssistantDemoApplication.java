package com.example.assistantdemo;

import com.example.assistantdemo.netty.NettyImageServer;
import com.example.assistantdemo.websocket.ImageWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class AssistantDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistantDemoApplication.class, args);
    }


//    @Autowired
//    private ImageWebSocketHandler wsHandler;

//    @PostConstruct
//    public void init() throws InterruptedException {
//        new NettyImageServer(wsHandler).start();
//    }
}
