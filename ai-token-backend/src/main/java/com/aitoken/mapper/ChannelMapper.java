package com.aitoken.mapper;

import com.aitoken.entity.Channel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.math.BigDecimal;

@Mapper
public interface ChannelMapper extends BaseMapper<Channel> {

    @Update("UPDATE t_channel SET health_status = #{healthStatus}, last_check_at = NOW() WHERE id = #{id}")
    int updateHealthStatus(@Param("id") Long id, @Param("healthStatus") Integer healthStatus);

    /** 原子累加渠道已用金额 */
    @Update("UPDATE t_channel SET used_money = used_money + #{amount} WHERE id = #{id}")
    void addUsedMoney(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 原子充值渠道总预算（delta 可为负数减少） */
    @Update("UPDATE t_channel SET total_money = total_money + #{delta} WHERE id = #{id}")
    void rechargeTotal(@Param("id") Long id, @Param("delta") BigDecimal delta);
}
