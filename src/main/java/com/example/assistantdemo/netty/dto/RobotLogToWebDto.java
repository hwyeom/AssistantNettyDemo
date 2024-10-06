package com.example.assistantdemo.netty.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RobotLogToWebDto {
    private String type;
    private List<RobotLogEntryDto> logs;
    private RobotMetaInfoDto meta;
}
