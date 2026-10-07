package com.aitoken.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ModelSaveRequest {

    @NotNull(message = "渠道ID不能为空")
    private Long channelId;

    @NotBlank(message = "模型名不能为空")
    private String modelName;

    /** 可选别名 */
    private String alias;

    private BigDecimal inputPrice = BigDecimal.ZERO;

    private BigDecimal outputPrice = BigDecimal.ZERO;

    /** 缓存创建单价，留空则等于 inputPrice */
    private BigDecimal cacheCreationPrice;

    /** 缓存命中单价，留空则等于 inputPrice × 0.1 */
    private BigDecimal cacheReadPrice;

    private Integer maxTokens;

    /** 权重, 用于同模型多渠道加权随机路由, 默认 1 */
    private Integer weight = 1;
}
