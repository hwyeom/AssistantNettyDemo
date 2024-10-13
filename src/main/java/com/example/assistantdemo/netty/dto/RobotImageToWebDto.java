package com.example.assistantdemo.netty.dto;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RobotImageToWebDto implements Serializable {
    private RobotMonitorDto robotInfo;
    private byte[] imageData;  // ByteBuf를 byte[]로 변환하여 전송

    // ByteBuf를 byte[]로 변환하는 도우미 메서드
    @Builder
    public RobotImageToWebDto(RobotMonitorDto robotInfo, ByteBuf byteBuf) {
        this.robotInfo = robotInfo;
        this.imageData = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(this.imageData); // ByteBuf 내용을 byte[]로 복사
    }

    // 직렬화 메소드
    public byte[] serializeToByteArray() {
        // JSON으로 직렬화
        String json = new Gson().toJson(this);
        return json.getBytes(StandardCharsets.UTF_8);
    }
}
