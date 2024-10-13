package com.example.assistantdemo.netty.dto;

import com.example.assistantdemo.netty.enums.WebSocketMessageType;
import com.example.assistantdemo.netty.vo.MouseCursorInfo;
import com.example.assistantdemo.netty.vo.RobotMetaInfo;
import com.example.assistantdemo.netty.vo.UiPathProcessInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RobotMonitorDto implements Serializable {
    private WebSocketMessageType type;
    private RobotMetaInfo robotMeta;
    private UiPathProcessInfo uiPathProcessInfo;
    private MouseCursorInfo cursorInfo;
}
