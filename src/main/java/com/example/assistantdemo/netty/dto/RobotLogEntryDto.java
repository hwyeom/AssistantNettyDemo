package com.example.assistantdemo.netty.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RobotLogEntryDto {
    @JsonProperty("message")
    private String message;
    @JsonProperty("level")
    private String level;
    @JsonProperty("logType")
    private String logType;
    @JsonProperty("timeStamp")
    private String timeStamp;
    @JsonProperty("fingerprint")
    private String fingerprint;
    @JsonProperty("windowsIdentity")
    private String windowsIdentity;
    @JsonProperty("machineName")
    private String machineName;
    @JsonProperty("fileName")
    private String fileName;
    @JsonProperty("jobId")
    private String jobId;
    @JsonProperty("robotName")
    private String robotName;
    @JsonProperty("machineId")
    private int machineId;
    @JsonProperty("processName")
    private String processName;
    @JsonProperty("processVersion")
    private String processVersion;
    @JsonProperty("organizationUnitId")
    private int organizationUnitId;
    @JsonProperty("BusinessOperationId")
    private String businessOperationId;
}
