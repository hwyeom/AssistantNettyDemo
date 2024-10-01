package com.example.assistantdemo.netty.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.buffer.Unpooled;

import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
public class WebSocketFrameHandler extends ChannelInboundHandlerAdapter {
    private final ChannelGroup dotnetClients;           // 이미지 전송 클라이언트 그룹
    private final ChannelGroup webClients;              // 웹 클라이언트 그룹
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 파서

    // 닷넷 클라이언트 ID와 웹 클라이언트를 매핑하기 위한 맵
    private final Map<String, List<ChannelId>> webClientChannelMap;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof WebSocketFrame) {
            try {
                WebSocketFrame frame = (WebSocketFrame) msg;
                if (frame instanceof TextWebSocketFrame) {
                    String message = ((TextWebSocketFrame) msg).text();
                    // 클라이언트 ID 처리 및 이벤트 처리
                    handleClientIdAndEvent(ctx, message);
                } else if (frame instanceof BinaryWebSocketFrame) {
                    ByteBuf content = frame.content();
                    // 이미지 전송 로직
                    handleImageTransmission(ctx, content);
                } else {
                    // 다른 유형의 프레임이 들어오면 처리할 수 있음 (예: 텍스트 메시지)
                    ctx.fireChannelRead(msg); // 필요한 경우 파이프라인의 다음 핸들러로 전달
                }

            } finally {
                ((WebSocketFrame) msg).release();
            }
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        log.info("Handler added. {}" , ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        log.info("Handler removed. {}" , ctx.channel());
        dotnetClients.remove(ctx.channel());
        webClients.remove(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception caught.", cause);
        ctx.close();
    }

    private void handleClientIdAndEvent(ChannelHandlerContext ctx, String message) {
        Map<String, Object> parsedMessage = null;
        try {
            parsedMessage = objectMapper.readValue(message, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            ctx.close();
            return;
        }

        String type = (String) parsedMessage.get("type");
        if ("imageSender".equals(type)) {
            log.info("닷넷 클라이언트 채널그룹 추가 ID:{}, {}", ctx.channel().id(), ctx.channel());
            dotnetClients.add(ctx.channel());

        } else if ("webClient".equals(type)) {
            String dotnetClientId = (String) parsedMessage.get("clientId");

            // 새로운 웹 클라이언트가 연결될 때 현재 연결된 닷넷 클라이언트 목록을 전송
            if (!hasText(dotnetClientId)) {
                sendDotnetClientList(ctx);
                ctx.close();
                return;
            }

            // 닷넷 클라이언트 ID에 대한 웹 클라이언트 그룹을 추가 또는 가져오기
            log.info("웹 클라이언트 채널그룹 추가 ID:{}, dotnetClientId {}", ctx.channel().id(), dotnetClientId);
            webClients.add(ctx.channel());
            webClientChannelMap.computeIfAbsent(dotnetClientId, k -> new ArrayList<>())
                    .add(ctx.channel().id());
        } else if ("mouseEvent".equals(type)) {
            // 마우스 이벤트 처리
            handleMouseEvent(parsedMessage);
        } else if ("keyboardEvent".equals(type)) {
            // 키보드 이벤트 처리
            handleKeyboardEvent(parsedMessage);
        }
    }

    private void handleImageTransmission(ChannelHandlerContext ctx, ByteBuf content) {
        // 닷넷 클라이언트에서 온 이미지를 웹 클라이언트로 전송
        if (dotnetClients.contains(ctx.channel())) {
            // 특정 웹 클라이언트에만 이미지 전송
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            String robotIp = socketAddress.getAddress().getHostAddress();

            List<ChannelId> channelIdListByDotnetClient = webClientChannelMap.get(robotIp);
            if (channelIdListByDotnetClient != null && !channelIdListByDotnetClient.isEmpty()) {
                for (ChannelId cId : channelIdListByDotnetClient) {
                    Channel c = webClients.find(cId);
                    if(c != null)
                        c.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(content)));
                }
            }
        }
    }

    private void handleMouseEvent(Map<String, Object> data) {
        // 마우스 이벤트 처리 로직 구현
        String eventType = (String) data.get("eventType");
        int x = (int) data.get("x");
        int y = (int) data.get("y");

        log.info("Mouse event: {}, x: {}, y: {}", eventType, x, y);

        Optional<Channel> robotChannel = findChannelByIp(dotnetClients, (String) data.get("robotIp"));
        if (robotChannel.isPresent()) {
            try {
                log.info("로봇에 마우스 이벤트 전달");
                String s = objectMapper.writeValueAsString(data);
                robotChannel.get().writeAndFlush(new TextWebSocketFrame(s));
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        }
        // 웹 클라이언트가 발생시킨 마우스 이벤트를 특정 닷넷 클라이언트로 전송
//        String[] parts = eventMessage.split(":");
//        if (parts.length == 2) {
//            String targetClientId = parts[0];  // 타겟 클라이언트 ID
//            String mouseEvent = parts[1];  // 마우스 이벤트 정보
//
//            ChannelHandlerContext targetCtx = clientChannelMap.get(targetClientId);
//            if (targetCtx != null) {
//                targetCtx.writeAndFlush(new TextWebSocketFrame(mouseEvent)); // 특정 클라이언트에 이벤트 전송
//                log.info("Mouse event sent to client {}: {}", targetClientId, mouseEvent);
//            } else {
//                log.warn("No client found with ID: {}", targetClientId);
//            }
//        }
    }

    private void handleKeyboardEvent(Map<String, Object> keyboardEvent) {
        // 키보드 이벤트 처리 로직 구현
        String eventType = (String) keyboardEvent.get("eventType");
        String key = (String) keyboardEvent.get("key");

        log.info("Keyboard event: {}, key: {}", eventType, key);
    }


    private void sendDotnetClientList(ChannelHandlerContext ctx) {
        List<String> connectedClientIds = dotnetClients.stream()
                .map(channel -> {
//                    channel.id().asShortText()
                    InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                    return socketAddress.getAddress().getHostAddress();
                })  // 또는 필요에 따라 다른 ID를 사용
                .collect(Collectors.toList());

        try {
            String jsonClientList = objectMapper.writeValueAsString(connectedClientIds);
            ctx.writeAndFlush(new TextWebSocketFrame(jsonClientList)); // 웹 클라이언트에 전송
        } catch (JsonProcessingException e) {
            log.error("Error serializing client list", e);
        }
    }

    // 채널 그룹에서 IP 주소로 채널 찾기
    private Optional<Channel> findChannelByIp(ChannelGroup channelGroup, String targetIp) {
        for (Channel channel : channelGroup) {
            InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
            String clientIp = socketAddress.getAddress().getHostAddress();

            // IP 주소가 일치하면 해당 채널을 반환
            if (clientIp.equals(targetIp)) {
                return Optional.of(channel);
            }
        }
        // IP 주소에 해당하는 채널이 없을 경우 null 반환
        return Optional.empty();
    }
}