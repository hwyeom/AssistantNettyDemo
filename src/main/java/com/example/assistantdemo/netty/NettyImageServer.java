package com.example.assistantdemo.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;

@Slf4j
@RequiredArgsConstructor
@Component
public class NettyImageServer {
    private final ServerBootstrap bootstrap;
    private final InetSocketAddress tcpPort;
    private Channel serverChannel;

    /**
     * 1. 이미지를 웹소켓으로 수신: .NET 클라이언트에서 전송한 이미지를 웹소켓 서버가 수신.
     * 2. 웹 클라이언트에 실시간으로 이미지 전달: 수신한 이미지를 다른 웹 클라이언트에게 실시간으로 브로드캐스트.
     * 3. 웹 클라이언트에서 이미지 표시: 웹 클라이언트가 수신한 이미지를 웹 페이지에 표시.
     */

    public void start() {
        try {
            // ChannelFuture: I/O operation 의 결과나 상태를 제공하는 객체
            // 지정한 host, port로 소켓을 바인딩하고 incoming connections 을 받도록 준비함
            ChannelFuture serverChannelFuture = bootstrap.bind(tcpPort).sync();
            log.info("Netty 서버가 실행되었습니다 {}" , tcpPort);

            // 서버 소켓이 닫힐 때까지 기다림
            serverChannel = serverChannelFuture.channel().closeFuture().sync().channel();
        }
        catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    // 애플리케이션 종료 시 네티 서버도 종료
    @PreDestroy
    public void stopServer() {
        if (serverChannel != null) {
            serverChannel.close();
            serverChannel.parent().closeFuture();
            log.info("Netty 서버가 안전하게 종료되었습니다.");
        }
    }
}
