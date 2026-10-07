package com.aitoken.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class GroupVO {
    private Long id;
    private String name;
    private String remark;
    private Long leaderId;
    private String leaderName;
    private Integer memberCount;
    private Date createdAt;
    /** 小组金额池 */
    private BigDecimal moneyPool;
    /** 小组成员余额总计（含组长） */
    private BigDecimal totalMoney;
    /** 周报类型: 1-需交周报(默认) 0-不需交 */
    private Integer reportType;
}
