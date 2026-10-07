package com.aitoken.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BatchKeyAssignRequest {
    private List<Long> userIds;    // 选中的用户 ID 列表
    private String name;           // Key 名称（新建模式使用）
    private Long totalQuota;       // 已换算好的 token 额度
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date expiredAt;        // 过期时间（null=永不过期）
    private String mode;           // "create"=新建Key / "update"=更新已有Key
}
