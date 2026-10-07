package com.aitoken.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

@Data
public class ApiKeyCreateRequest {

    private String name;

    /** Key 类型: 1-Token额度型 2-金额消费型(默认) */
    private Integer keyType = 2;

    /** 绑定渠道ID，null 表示走负载均衡 */
    private Long channelId;

    /** 总 Token 额度, -1 不限制 */
    private Long totalQuota = -1L;

    /** 过期时间, null 永不过期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date expiredAt;
}
