package com.aitoken.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ChannelStatVO {
    private Long       channelId;
    private String     channelName;
    private Long       totalRequests;
    private Long       successRequests;
    private Long       failRequests;
    private Long       totalTokens;
    private BigDecimal totalCost;
    private Integer    avgDurationMs;
}
