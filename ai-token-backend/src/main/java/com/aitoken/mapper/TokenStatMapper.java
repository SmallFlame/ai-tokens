package com.aitoken.mapper;

import com.aitoken.entity.TokenStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface TokenStatMapper extends BaseMapper<TokenStat> {

    /**
     * 将 t_call_log 中指定日期的数据聚合写入统计表
     * 使用 INSERT INTO ... ON DUPLICATE KEY UPDATE 保证幂等
     */
    int aggregateByDate(@Param("statDate") Date statDate);

    /**
     * 查询各模型 Token 分布(饼图)
     */
    List<Map<String, Object>> selectModelDistribution(@Param("days") int days);

    /** 仅统计组外用户（group_status=0）的模型分布 */
    List<Map<String, Object>> selectModelDistributionByOutGroup(@Param("days") int days);
}
