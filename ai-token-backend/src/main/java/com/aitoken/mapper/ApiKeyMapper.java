package com.aitoken.mapper;

import com.aitoken.entity.ApiKey;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.math.BigDecimal;

@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {

    /**
     * 原子累加已用 Token 数
     */
    @Update("UPDATE t_api_key SET used_quota = used_quota + #{tokens} WHERE id = #{id}")
    int addUsedQuota(@Param("id") Long id, @Param("tokens") long tokens);

    /**
     * 累加 Key 已用金额（统计用）
     */
    @Update("UPDATE t_api_key SET used_money = used_money + #{amount} WHERE id = #{id}")
    int addUsedMoney(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
