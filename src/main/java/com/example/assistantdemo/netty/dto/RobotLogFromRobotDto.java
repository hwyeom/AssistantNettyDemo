package com.example.assistantdemo.netty.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RobotLogFromRobotDto {
    private String type;
    private String message;
    private RobotMetaInfoDto meta;
}
