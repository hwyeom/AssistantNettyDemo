package com.example.assistantdemo;

import com.example.assistantdemo.netty.NettyImageServer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationStartupTask implements ApplicationListener<ApplicationReadyEvent> {

    private final NettyImageServer nettyImageServer;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        nettyImageServer.start();
    }
}
