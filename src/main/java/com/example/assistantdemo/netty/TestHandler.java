package com.example.assistantdemo.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class TestHandler extends ChannelInboundHandlerAdapter {
    private int DATA_LENGTH = 2048;
    private ByteBuf buff;

    //핸들러가 생성될 때 호출되는 메소드
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        log.info("handler added : {}", ctx.channel().remoteAddress());
        buff = ctx.alloc().buffer(DATA_LENGTH);
    }

    // 핸들러가 제거될 때 호출 되는 메소드
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (buff.refCnt() > 0) {
            buff.release();
        }
        buff = null;
        log.info("Removed handler : {}", ctx.channel().remoteAddress());
    }

    // 클라이언트와 연결되어 트래픽을 생성할 준비가 되었을 때 호출되는 메소드
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String remoteAddress = ctx.channel().remoteAddress().toString();
        log.info("Remote Address: {}", remoteAddress);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf msgBuf = (ByteBuf) msg;
        buff.writeBytes(msgBuf);   // 클라이언트에서 보내는 데이터가 축적됨
        msgBuf.release();

        final ChannelFuture f = ctx.writeAndFlush(buff);
        f.addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 에러 발생 시 커넥션 클로즈
        ctx.close();
        log.error(cause.getMessage(), cause);
    }
}
