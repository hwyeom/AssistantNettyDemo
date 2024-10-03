package com.example.assistantdemo.netty.dto;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RobotImageDto implements Serializable {
    private String clientId;
    private String clientIp;
    private byte[] imageData;  // ByteBuf를 byte[]로 변환하여 전송

    // ByteBuf를 byte[]로 변환하는 도우미 메서드
    public RobotImageDto(String clientId, String clientIp, ByteBuf byteBuf) {
        this.clientId = clientId;
        this.clientIp = clientIp;
        this.imageData = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(this.imageData); // ByteBuf 내용을 byte[]로 복사
    }
}
