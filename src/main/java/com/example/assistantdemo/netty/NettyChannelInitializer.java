package com.example.assistantdemo.netty;

import com.example.assistantdemo.netty.dto.RobotMetaInfoDto;
import com.example.assistantdemo.netty.handler.RequestFilterHandler;
import com.example.assistantdemo.netty.handler.RobotMonitorHandler;
import io.netty.channel.Channel;
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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@RequiredArgsConstructor
public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {
    // 연결된 클라이언트 채널
    private final ChannelGroup robotClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final ChannelGroup webUserClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final Map<String, List<Channel>> robotGroups = new HashMap<>();
    private final Map<String, RobotMetaInfoDto> robotMetaMap = new HashMap<>();

    // 클라이언트 소켓 채널이 생성될 때 호출 됨
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // 웹소캣 통신 시 파이프라인 설정
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));  // HTTP 메시지 조각을 모음
        pipeline.addLast(new ChunkedWriteHandler());    // 파일 및 데이터 스트림 핸들링

        // 요청 URL 등 검증
        String webSocketPath = "/robot";
        pipeline.addLast(new RequestFilterHandler(webSocketPath));
        // 여기서 최대 프레임 크기를 설정 (예: 10MB로 설정)
        pipeline.addLast(new WebSocketServerProtocolHandler(webSocketPath,null, true, 10 * 1024 * 1024));
        pipeline.addLast(new RobotMonitorHandler(robotClients, webUserClients, robotGroups, robotMetaMap));  // 커스텀 핸들러
    }
}
