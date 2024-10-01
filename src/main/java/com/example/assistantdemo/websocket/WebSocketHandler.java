package com.example.assistantdemo.websocket;

import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.*;

import java.util.concurrent.CopyOnWriteArrayList;

public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {

    private final CopyOnWriteArrayList<Channel> webSocketChannels = new CopyOnWriteArrayList<>();

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (req.uri().equals("/ws")) {
            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                    getWebSocketLocation(req), null, true);
            WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            } else {
                handshaker.handshake(ctx.channel(), req);
                webSocketChannels.add(ctx.channel());
            }
        } else {
            // HTTP 처리 필요 시 구현
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
            ctx.channel().close();
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.write(new PongWebSocketFrame(frame.content().retain()));
        }
    }

    // 클라이언트에게 이미지 데이터를 브로드캐스트하는 메서드
    public void broadcastImageToClients(ChannelHandlerContext ctx, byte[] imageBytes) {
        BinaryWebSocketFrame frame = new BinaryWebSocketFrame(ctx.alloc().buffer(imageBytes.length).writeBytes(imageBytes));
        for (Channel channel : webSocketChannels) {
            channel.writeAndFlush(frame.retain());
        }
    }

    private String getWebSocketLocation(FullHttpRequest req) {
        return "ws://" + req.headers().get(HttpHeaderNames.HOST) + "/ws";
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
