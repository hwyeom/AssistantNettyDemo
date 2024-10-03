package com.example.assistantdemo.netty;

import com.example.assistantdemo.netty.handler.RobotMonitorHandler;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {
    // 연결된 클라이언트 채널
    private final ChannelGroup robotClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final ChannelGroup webUserClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final ChannelGroup watchThumbnailUsers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private Map<String, List<ChannelId>> robotGroups = new HashMap<>();

    // 클라이언트 소켓 채널이 생성될 때 호출 됨
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // 웹소캣 통신 시 파이프라인 설정
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));  // HTTP 메시지 조각을 모음
        pipeline.addLast(new ChunkedWriteHandler());    // 파일 및 데이터 스트림 핸들링

        // 여기서 최대 프레임 크기를 설정 (예: 10MB로 설정)
        pipeline.addLast(new WebSocketServerProtocolHandler("/robot",null, true, 10 * 1024 * 1024));
        pipeline.addLast(new RobotMonitorHandler(robotClients, webUserClients, watchThumbnailUsers, robotGroups));  // 커스텀 핸들러
    }
}
