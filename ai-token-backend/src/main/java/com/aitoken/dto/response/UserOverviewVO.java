package com.aitoken.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UserOverviewVO {
    private long todayCalls;
    private long todayTokens;
    private BigDecimal todayCost;
    private long totalCalls;
    private long totalTokens;
    private BigDecimal totalCost;
    private long apiKeyCount;

    /** 金额总额度（所有 Key 中有限额的之和），null 表示全部无限制 */
    private BigDecimal moneyQuota;
    /** 已用金额 */
    private BigDecimal usedMoney;
    /** 剩余金额 */
    private BigDecimal remainingMoney;
}
