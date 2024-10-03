//package com.example.assistantdemo.netty.codec;
//
//import com.example.assistantdemo.netty.dto.RobotImageDto;
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.handler.codec.MessageToByteEncoder;
//
//import java.nio.charset.StandardCharsets;
//
//// 객체 → 바이트
//public class RobotImageDtoEncoder extends MessageToByteEncoder<RobotImageDto> {
//
//    @Override
//    protected void encode(ChannelHandlerContext ctx, RobotImageDto msg, ByteBuf out) throws Exception {
//        // clientId (String)
//        byte[] clientIdBytes = msg.getClientId().getBytes(StandardCharsets.UTF_8);
//        out.writeInt(clientIdBytes.length);  // String 길이 먼저 전송
//        out.writeBytes(clientIdBytes);       // 실제 문자열 전송
//
//        // clientIp (String)
//        byte[] clientIpBytes = msg.getClientIp().getBytes(StandardCharsets.UTF_8);
//        out.writeInt(clientIpBytes.length);  // String 길이 먼저 전송
//        out.writeBytes(clientIpBytes);       // 실제 문자열 전송
//
//        // imageData (ByteBuf)
//        int imageDataLength = msg.getImageData().readableBytes();
//        out.writeInt(imageDataLength);        // 이미지 데이터 크기
//        out.writeBytes(msg.getImageData());   // 실제 이미지 데이터 전송
//    }
//}