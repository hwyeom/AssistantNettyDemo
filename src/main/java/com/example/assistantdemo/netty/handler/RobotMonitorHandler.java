package com.example.assistantdemo.netty.handler;

import com.example.assistantdemo.netty.dto.*;
import com.example.assistantdemo.netty.enums.WebSocketMessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.buffer.Unpooled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.example.assistantdemo.netty.enums.WebSocketMessageType.ROBOT_LOG;
import static com.example.assistantdemo.netty.enums.WebSocketMessageType.ROBOT_THUMBNAIL_LIST;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
public class RobotMonitorHandler extends ChannelInboundHandlerAdapter {
    private final ObjectMapper om = new ObjectMapper(); // JSON 파서

    private final ChannelGroup robotClients;           // 이미지 전송 클라이언트 그룹
    private final ChannelGroup watchThumbnailUsers;     // 썸네일 확인 중인 유저 그룹
    private final Map<String, List<Channel>> webClientChannelMap; // 로봇 IP 기준 모니터링 중인 유저 그룹
    private final Map<String, RobotMetaInfoDto> robotMetaMap;     // 로봇 메타 정보를 가지고 있는 맵..

    /**
     * 웹소캣 연결 후 메시지를 수신 받는 곳
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 메시지가 웹소켓 포맷인 경우만 수행
        if (msg instanceof WebSocketFrame) {
            try {
                WebSocketFrame frame = (WebSocketFrame) msg;
                if (frame instanceof TextWebSocketFrame) {
                    // 텍스트 포맷 메시지 처리
                    String message = ((TextWebSocketFrame) msg).text();
                    handleTextTypeEvent(ctx, message);
                } else if (frame instanceof BinaryWebSocketFrame) {
                    // 바이너리 포맷 메시지 처리
                    ByteBuf content = frame.content();
                    handleBinaryTypeEvent(ctx, content);
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
        log.info("Handler remove. {}" , ctx.channel());
        Channel c = ctx.channel();
        if (robotClients.contains(ctx.channel())) {
            log.info("로봇 채널 연결 해제 {}" , c);
            robotMetaMap.remove(c.id().toString());
            robotClients.remove(c);
            return;
        }
        if (watchThumbnailUsers.contains(ctx.channel())) {
            log.info("썸네일 웹 접속 유저 채널 연결 해제 {}" , c);
            watchThumbnailUsers.remove(c);
            return;
        }
        for (Map.Entry<String, List<Channel>> entry : webClientChannelMap.entrySet()) {
            List<Channel> channels = entry.getValue();
            if (channels.contains(c)) {
                log.info("로봇 모니터링 유저 채널 연결 해제 {}" , c);
                channels.remove(c);
                return;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception caught.", cause);
        ctx.close();
    }

    private void handleTextTypeEvent(ChannelHandlerContext ctx, String message) throws JsonProcessingException {
        // 수신 받은 메시지를 맵 타입으로 변환 (규칙에 안맞는 메시지는 끊어버리자)
        Map<String, Object> parsedMessage = om.readValue(message, new TypeReference<>() {});
        WebSocketMessageType type = WebSocketMessageType.valueOf((String) parsedMessage.get("type"));
        switch (type) {
            case ROBOT_REGISTER:
                log.info("로봇 채널그룹 추가 channel:{}, meta: {}", ctx.channel(), message);
                // 로봇을 채널 그룹에 추가하고 메타정보 저장
                robotClients.add(ctx.channel());
                RobotMetaInfoDto rMeta = om.readValue(message, RobotMetaInfoDto.class);
                rMeta.setChannelId(ctx.channel().id().toString());
                robotMetaMap.put(ctx.channel().id().toString(), rMeta);
                break;
            case ROBOT_MOUSE_POINTER:
                // 로봇 -> 웹 마우스 위치 정보
                sendTextDataRobotCursorPoint(ctx, message);
                break;
            case ROBOT_LOG: {
                // 로봇 -> 웹 로그 전송
                RobotLogFromRobotDto fromRobotDto = om.readValue(message, RobotLogFromRobotDto.class);
                List<RobotLogEntryDto> logList = extractRobotLogEntryDtoByClientMessage(fromRobotDto.getMessage());
                RobotLogToWebDto dto = RobotLogToWebDto.builder()
                        .type(ROBOT_LOG.name())
                        .meta(fromRobotDto.getMeta())
                        .logs(logList).build();
                sendWebRobotLog(ctx, dto);
                break;
            }
            case WEB_USER_REGISTER: {
                String robotIpAddress = (String) parsedMessage.get("robotIpAddress");
                // 새로운 웹 클라이언트가 연결될 때 현재 연결된 닷넷 클라이언트 목록을 전송
                if (!hasText(robotIpAddress)) {
                    sendRobotClientList(ctx);
                    ctx.close(); return;
                }

                // 특정 로봇을 팔로우 하는 웹유저에 추가
                addWebClientChannelMap(robotIpAddress, ctx.channel());
                ctx.writeAndFlush(new TextWebSocketFrame(om.writeValueAsString(getRobotMetaMapByRobotIpAddress(robotIpAddress))));
                break;
            }
            case WEB_USER_THUMBNAIL_REGISTER: {
                // 웹 썸네일 필요한 유저 추가
                if (!watchThumbnailUsers.contains(ctx.channel())) {
                    log.info("웹 썸네일 유저 채널그룹 추가 ID:{}", ctx.channel());
                    watchThumbnailUsers.add(ctx.channel());
                }
                break;
            }
            case WEB_USER_MOUSE_EVENT:
                // 웹 -> 마우스 이벤트 전송
                handleMouseEvent(parsedMessage);
                break;
        }
    }

    private RobotMetaInfoDto getRobotMetaMapByRobotIpAddress(String robotIpAddress) {
        for (Map.Entry<String, RobotMetaInfoDto> entry : robotMetaMap.entrySet()) {
            if(entry.getValue().getIpAddress().equals(robotIpAddress)) {
                return entry.getValue();
            }
        }
        return null;
    }

    // 로봇에서 온 로그를 웹 클라이언트로 전송
    private void sendWebRobotLog(ChannelHandlerContext ctx, RobotLogToWebDto dto) throws JsonProcessingException {
        if (robotClients.contains(ctx.channel())) {
            // 특정 웹 클라이언트에만 이미지 전송
            sendTextDataRobotConsumer(om.writeValueAsString(dto), dto.getMeta().getIpAddress());
        }
    }

    // 로봇으로 부터 로그가 오면 {} 부분만 추출해서 List 객체로 변환
    private List<RobotLogEntryDto> extractRobotLogEntryDtoByClientMessage(String message) {
        List<RobotLogEntryDto> robotLogEntries = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        Pattern logPattern = Pattern.compile("\\{.*?}");
        Matcher matcher = logPattern.matcher(message);

        while (matcher.find()) {
            String jsonString = matcher.group(); // 매칭된 JSON 문자열
            try {
                // JSON 문자열을 RobotLogEntryDto로 변환
                RobotLogEntryDto logEntry = objectMapper.readValue(jsonString, RobotLogEntryDto.class);
                robotLogEntries.add(logEntry);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return robotLogEntries;
    }

    // 특정 로봇을 팔로우하는 유저를 맵에 추가
    private void addWebClientChannelMap(String channelId, Channel channel) {
        log.info("로봇 모니터링 유저 채널그룹 추가 ID:{}, robot-channelId {}", channel, channelId);
        // robotIp에 해당하는 리스트를 가져옵니다. 없으면 new ArrayList<>() 할당
        List<Channel> channels = webClientChannelMap.computeIfAbsent(channelId, k -> new ArrayList<>());
        // 리스트에 채널 추가
        channels.add(channel);
    }

    // 웹 클라이언트에 로봇 마우스 위치 전송
    private void sendTextDataRobotCursorPoint(ChannelHandlerContext ctx, String message) {
        // 로봇에서 온 이미지를 웹 클라이언트로 전송
        if (robotClients.contains(ctx.channel())) {
            // 특정 웹 클라이언트에만 이미지 전송
            RobotMetaInfoDto meta = robotMetaMap.get(ctx.channel().id().toString());
            if (meta != null) {
                sendTextDataRobotConsumer(message, meta.getIpAddress());
            }
        }
    }

    // 웹 클라이언트에 이미지 전송
    private void handleBinaryTypeEvent(ChannelHandlerContext ctx, ByteBuf content) {
        // 닷넷 클라이언트에서 온 이미지를 웹 클라이언트로 전송
        if (robotClients.contains(ctx.channel())) {
            // 특정 웹 클라이언트에만 이미지 전송
            String cId = ctx.channel().id().toString();
            RobotMetaInfoDto meta = robotMetaMap.get(cId);

            // 썸네일 유저들에게 이미지 전송
            RobotImageToWebDto dto = RobotImageToWebDto.builder()
                    .meta(meta)
                    .byteBuf(Unpooled.copiedBuffer(content))
                    .build();
            watchThumbnailUsers.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(dto.serializeToByteArray())));
            // 상세 모니터링 유저들에게 이미지 전송
            sendBinaryImageToDetailMonitorUser(dto, meta.getIpAddress());
        }
    }

    private void sendBinaryImageToDetailMonitorUser(RobotImageToWebDto d, String robotIp) {
        // 로봇 IP 기준 채널맵에서 요청한 IP를 팔로우 하고 있는 채널을 추출
        List<Channel> channelUsersByRobotIp = webClientChannelMap.get(robotIp);
        if (channelUsersByRobotIp != null && !channelUsersByRobotIp.isEmpty()) {
            for (Channel c : channelUsersByRobotIp) {
                if(c != null){
                    c.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(d.serializeToByteArray())));
                }
            }
        }
    }

    private void sendTextDataRobotConsumer(String message, String robotIp) {
        List<Channel> channelUsersByRobotIp = webClientChannelMap.get(robotIp);
        if (channelUsersByRobotIp != null && !channelUsersByRobotIp.isEmpty()) {
            for (Channel c : channelUsersByRobotIp) {
                if(c != null){
                    c.writeAndFlush(new TextWebSocketFrame(message));
                }
            }
        }
    }

    private void handleMouseEvent(Map<String, Object> data) {
        // 마우스 이벤트
        data.put("x", ((Number) data.get("x")).intValue()); // int 로 가져오기
        data.put("y", ((Number) data.get("y")).intValue()); // int 로 가져오기

        log.info("Mouse event: {}", data);

        Optional<Channel> robotChannel = findChannelByChannelId(robotClients, (String) data.get("channelId"));
        if (robotChannel.isPresent()) {
            try {
                String s = om.writeValueAsString(data);
                log.info("로봇에 마우스 이벤트 전달");
                robotChannel.get().writeAndFlush(new TextWebSocketFrame(s));
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        }
    }

    private void handleKeyboardEvent(Map<String, Object> keyboardEvent) {
        // 키보드 이벤트 처리 로직 구현
        String eventType = (String) keyboardEvent.get("eventType");
        String key = (String) keyboardEvent.get("key");

        log.info("Keyboard event: {}, key: {}", eventType, key);
    }


    private void sendRobotClientList(ChannelHandlerContext ctx) {
        List<String> connectedClientIds = robotClients.stream()
                .map(channel -> {
                    InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                    return socketAddress.getAddress().getHostAddress();
                })  // 또는 필요에 따라 다른 ID를 사용
                .collect(Collectors.toList());

        try {
            String jsonClientList = om.writeValueAsString(connectedClientIds);
            ctx.writeAndFlush(new TextWebSocketFrame(jsonClientList)); // 웹 클라이언트에 전송
        } catch (JsonProcessingException e) {
            log.error("Error serializing client list", e);
        }
    }

    // 채널 그룹에서 IP 주소로 채널 찾기
    private Optional<Channel> findChannelByTargetIp(ChannelGroup channelGroup, String targetIp) {
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

    private Optional<Channel> findChannelByChannelId(ChannelGroup channelGroup, String channelId) {
        for (Channel channel : channelGroup) {
            if (channel.id().toString().equals(channelId)) {
                return Optional.of(channel);
            }
        }
        // 채널이 없을 경우 null 반환
        return Optional.empty();
    }

    private String getChannelRemoteAddress(Channel channel) {
        InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
        return socketAddress.getAddress().getHostAddress();
    }
}