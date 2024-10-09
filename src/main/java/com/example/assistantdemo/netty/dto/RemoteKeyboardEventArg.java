package com.example.assistantdemo.netty.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RemoteKeyboardEventArg {
    private String type;
    private String eventType;
    private String channelId;
    private String key;
    private String code;
}
