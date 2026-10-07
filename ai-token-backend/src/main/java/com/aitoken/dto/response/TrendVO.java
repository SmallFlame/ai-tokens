package com.aitoken.dto.response;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Token 每日趋势数据点
 */
@Data
public class TrendVO {

    private String statDate;
    private Long callCount;
    private Long successCount;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private BigDecimal totalCost;
}
