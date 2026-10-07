package com.aitoken.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 看板统计数据
 */
@Data
public class DashboardVO {

    /** 今日调用次数 */
    private Long todayCallCount;

    /** 今日成功次数 */
    private Long todaySuccessCount;

    /** 今日 Token 总数 */
    private Long todayTotalTokens;

    /** 今日费用(元) */
    private BigDecimal todayCost;

    /** 总调用次数 */
    private Long totalCallCount;

    /** 总 Token 数 */
    private Long totalTokens;

    /** 平均响应时长(ms) */
    private Double avgDurationMs;

    /** 渠道健康数 / 总渠道数 */
    private Integer healthChannels;
    private Integer totalChannels;

    /** 近 7 天趋势 */
    private List<TrendVO> trend;

    /** 各模型 Token 占比 */
    private List<Map<String, Object>> modelDistribution;
}
