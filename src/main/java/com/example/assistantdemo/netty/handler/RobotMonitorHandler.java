package com.example.assistantdemo.netty.handler;

import com.example.assistantdemo.netty.dto.RobotImageDto;
import com.example.assistantdemo.netty.enums.WebSocketMessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.buffer.Unpooled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
public class RobotMonitorHandler extends ChannelInboundHandlerAdapter {
    private final ChannelGroup robotClients;           // 이미지 전송 클라이언트 그룹
    private final ChannelGroup webUserClients;              // 웹 클라이언트 그룹
    private final ChannelGroup watchThumbnailUsers;
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 파서

    // 로봇 IP 기준 모니터링 중인 웹 클라이언트 정보
    private final Map<String, List<ChannelId>> webClientChannelMap;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof WebSocketFrame) {
            try {
                WebSocketFrame frame = (WebSocketFrame) msg;
                if (frame instanceof TextWebSocketFrame) {
                    String message = ((TextWebSocketFrame) msg).text();
                    // 클라이언트 ID 처리 및 이벤트 처리
                    handleTextTypeEvent(ctx, message);
                } else if (frame instanceof BinaryWebSocketFrame) {
                    ByteBuf content = frame.content();
                    // 이미지 전송 로직
                    handleImageTypeEvent(ctx, content);
                } else {
                    // 다른 유형의 프레임이 들어오면 처리할 수 있음
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
        robotClients.remove(ctx.channel());
        webUserClients.remove(ctx.channel());
        watchThumbnailUsers.remove(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception caught.", cause);
        ctx.close();
    }

    private void handleTextTypeEvent(ChannelHandlerContext ctx, String message) {
        Map<String, Object> parsedMessage = null;
        try {
            parsedMessage = objectMapper.readValue(message, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            ctx.close();
            return;
        }

        WebSocketMessageType type = WebSocketMessageType.valueOf((String) parsedMessage.get("type"));
        switch (type) {
            case ROBOT_REGISTER:
                log.info("닷넷 클라이언트 채널그룹 추가 ID:{}, {}", ctx.channel().id(), ctx.channel());
                robotClients.add(ctx.channel());
                break;
            case WEB_USER_REGISTER:
                String robotIp = (String) parsedMessage.get("robotIp");
                // 새로운 웹 클라이언트가 연결될 때 현재 연결된 닷넷 클라이언트 목록을 전송
                if (!hasText(robotIp)) {
                    sendDotnetClientList(ctx);
                    ctx.close();
                    return;
                }

                // 닷넷 클라이언트 ID에 대한 웹 클라이언트 그룹을 추가 또는 가져오기
                log.info("웹 클라이언트 채널그룹 추가 ID:{}, robotIp {}", ctx.channel().id(), robotIp);
                webUserClients.add(ctx.channel());
                webClientChannelMap.computeIfAbsent(robotIp, k -> new ArrayList<>()).add(ctx.channel().id());
                break;
            case WEB_USER_THUMBNAIL_REGISTER:
                watchThumbnailUsers.add(ctx.channel());
                break;
            case ROBOT_MOUSE_POINTER:
                // 로봇 마우스 위치 정보
                sendTextDataRobotCursorPoint(ctx, message);
                break;
        }
    }

    // 웹 클라이언트에 로봇 마우스 위치 전송
    private void sendTextDataRobotCursorPoint(ChannelHandlerContext ctx, String message) {
        // 닷넷 클라이언트에서 온 이미지를 웹 클라이언트로 전송
        if (robotClients.contains(ctx.channel())) {
            // 특정 웹 클라이언트에만 이미지 전송
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            String robotIp = socketAddress.getAddress().getHostAddress();

            sendTextDataRobotConsumer(message, robotIp);
        }
    }

    // 웹 클라이언트에 이미지 전송
    private void handleImageTypeEvent(ChannelHandlerContext ctx, ByteBuf content) {
        // 닷넷 클라이언트에서 온 이미지를 웹 클라이언트로 전송
        if (robotClients.contains(ctx.channel())) {
            // 특정 웹 클라이언트에만 이미지 전송
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            String robotIp = socketAddress.getAddress().getHostAddress();

            // 썸네일 유저들에게 이미지 전송
            RobotImageDto dto = new RobotImageDto(ctx.channel().id().toString(), robotIp, Unpooled.copiedBuffer(content));

//            watchThumbnailUsers.writeAndFlush(message);
            watchThumbnailUsers.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(serializeToByteArray(dto))));
            // 상세 모니터링 유저들에게 이미지 전송
            sendBinaryDataRobotConsumer(dto, robotIp);
//            sendBinaryDataRobotConsumer(content, robotIp);
        }
    }

    // 직렬화 메소드 (예: JSON, Protobuf 등 사용)
    private byte[] serializeToByteArray(RobotImageDto dto) {
        // JSON으로 직렬화
        String json = new Gson().toJson(dto);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private void sendBinaryDataRobotConsumer(RobotImageDto d, String robotIp) {
        List<ChannelId> channelUsersByRobotIp = webClientChannelMap.get(robotIp);
        if (channelUsersByRobotIp != null && !channelUsersByRobotIp.isEmpty()) {
            for (ChannelId userCID : channelUsersByRobotIp) {
                Channel c = webUserClients.find(userCID);
                if(c != null){
//                    c.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(d)));
                    c.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(serializeToByteArray(d))));
                }
//                    c.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(d)));
//                    c.writeAndFlush(new BinaryWebSocketFrame(buf.retainedDuplicate()));
            }
        }
    }

    private void sendTextDataRobotConsumer(String message, String robotIp) {
        List<ChannelId> channelUsersByRobotIp = webClientChannelMap.get(robotIp);
        if (channelUsersByRobotIp != null && !channelUsersByRobotIp.isEmpty()) {
            for (ChannelId userCID : channelUsersByRobotIp) {
                Channel c = webUserClients.find(userCID);
                if(c != null)
                    c.writeAndFlush(new TextWebSocketFrame(message));
            }
        }
    }

    private void handleMouseEvent(Map<String, Object> data) {
        // 마우스 이벤트 처리 로직 구현
        String eventType = (String) data.get("eventType");
        int x = (int) data.get("x");
        int y = (int) data.get("y");

        log.info("Mouse event: {}, x: {}, y: {}", eventType, x, y);

        Optional<Channel> robotChannel = findChannelByIp(robotClients, (String) data.get("robotIp"));
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
        List<String> connectedClientIds = robotClients.stream()
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