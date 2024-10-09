package com.example.assistantdemo.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Slf4j
public class RequestFilterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final String allowedPath;

    public RequestFilterHandler(String allowedPath) {
        this.allowedPath = allowedPath;
    }

    /**
     * 우리가 받을 요청이 아니면 연결을 끊어버린다
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 요청 경로 확인
        String uri = request.uri();
        log.info("uri: {}", uri);
        if (!uri.startsWith(allowedPath)) {
            // 허용되지 않은 경로일 경우 404 반환
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_FOUND);
            // Keep-alive 설정에 따른 헤더 처리
            if (HttpUtil.isKeepAlive(request)) {
                response.headers().set("Connection", "keep-alive");
            }
            // 응답 후 채널 닫음
            ctx.writeAndFlush(response).addListener(future -> ctx.close());
            return;
        }
        // 허용된 경로일 경우 다음 핸들러로 요청 전달
        ctx.fireChannelRead(request.retain());
    }

}
