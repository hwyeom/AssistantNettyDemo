package com.example.assistantdemo.netty;

import com.example.assistantdemo.netty.handler.WebSocketFrameHandler;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final TestHandler testHandler;
    // 연결된 클라이언트 채널
    private ChannelGroup dotnetClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private ChannelGroup webClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private Map<String, List<ChannelId>> robotGroups = new HashMap<>();

    // 클라이언트 소켓 채널이 생성될 때 호출 됨
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // decoder는 @Shareble이 안됨, Bean 객체 주입이 안되고 매번 새로운 객체를 생성해야함;
//        pipeline.addLast(new TestDecoder());
//        pipeline.addLast(testHandler);
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));  // HTTP 메시지 조각을 모음
        pipeline.addLast(new ChunkedWriteHandler());    // 파일 및 데이터 스트림 핸들링

        // 여기서 최대 프레임 크기를 설정 (예: 10MB로 설정)
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws",null, true, 10 * 1024 * 1024));

        pipeline.addLast(new WebSocketFrameHandler(dotnetClients, webClients, robotGroups));  // 커스텀 핸들러
    }
}
