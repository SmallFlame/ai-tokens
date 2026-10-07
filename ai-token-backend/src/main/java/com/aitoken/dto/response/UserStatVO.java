package com.aitoken.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UserStatVO {
    private Long       userId;
    private String     username;
    private Long       totalRequests;
    private Long       successRequests;
    private Long       failRequests;
    private Long       totalTokens;
    private BigDecimal totalCost;
    private Integer    avgDurationMs;
}
