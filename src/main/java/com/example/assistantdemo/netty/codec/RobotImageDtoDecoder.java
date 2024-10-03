//package com.example.assistantdemo.netty.codec;
//
//import com.example.assistantdemo.netty.dto.RobotImageDto;
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.handler.codec.ByteToMessageDecoder;
//
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//
////바이트 → 객체
//public class RobotImageDtoDecoder extends ByteToMessageDecoder {
//    @Override
//    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
//        // clientId (String)
//        int clientIdLength = in.readInt();
//        byte[] clientIdBytes = new byte[clientIdLength];
//        in.readBytes(clientIdBytes);
//        String clientId = new String(clientIdBytes, StandardCharsets.UTF_8);
//
//        // clientIp (String)
//        int clientIpLength = in.readInt();
//        byte[] clientIpBytes = new byte[clientIpLength];
//        in.readBytes(clientIpBytes);
//        String clientIp = new String(clientIpBytes, StandardCharsets.UTF_8);
//
//        // imageData (ByteBuf)
//        int imageDataLength = in.readInt();
//        ByteBuf imageData = in.readBytes(imageDataLength);
//
//        // RobotImageDto 객체 생성 및 추가
//        RobotImageDto robotImageDto = new RobotImageDto(clientId, clientIp, imageData);
//        out.add(robotImageDto);
//    }
//}