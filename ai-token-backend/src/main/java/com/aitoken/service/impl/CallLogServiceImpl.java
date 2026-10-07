package com.aitoken.service.impl;

import com.aitoken.dto.request.CallLogQuery;
import com.aitoken.dto.response.UserOverviewVO;
import com.aitoken.entity.CallLog;
import com.aitoken.entity.User;
import com.aitoken.mapper.CallLogMapper;
import com.aitoken.service.CallLogService;
import com.aitoken.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CallLogServiceImpl extends ServiceImpl<CallLogMapper, CallLog> implements CallLogService {

    @Resource
    private UserService userService;

    @Async
    @Override
    public void asyncSave(CallLog callLog) {
        try {
            save(callLog);
        } catch (Exception e) {
            log.error("异步保存调用日志失败", e);
        }
    }

    @Override
    public Page<CallLog> pageQuery(CallLogQuery query) {
        LambdaQueryWrapper<CallLog> wrapper = new LambdaQueryWrapper<CallLog>()
                .eq(query.getUserId() != null,    CallLog::getUserId,    query.getUserId())
                .eq(query.getChannelId() != null, CallLog::getChannelId, query.getChannelId())
                .eq(query.getModelName() != null && !query.getModelName().isEmpty(),
                        CallLog::getModelName, query.getModelName())
                .eq(query.getStatus() != null,    CallLog::getStatus,    query.getStatus())
                .ge(query.getStartTime() != null, CallLog::getCreatedAt, query.getStartTime())
                .le(query.getEndTime() != null,   CallLog::getCreatedAt, query.getEndTime())
                .orderByDesc(CallLog::getCreatedAt);
        Page<CallLog> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        // 补充用户名
        List<CallLog> logs = page.getRecords();
        if (!logs.isEmpty()) {
            Map<Long, String> userIdToNameMap = new HashMap<>();
            for (CallLog log : logs) {
                if (log.getUserId() != null && !userIdToNameMap.containsKey(log.getUserId())) {
                    User user = userService.getById(log.getUserId());
                    userIdToNameMap.put(log.getUserId(), user != null ? user.getUsername() : "unknown");
                }
            }
            logs.forEach(log -> log.setUserName(userIdToNameMap.get(log.getUserId())));
        }

        return page;
    }

    @Override
    public UserOverviewVO getUserOverview(Long userId) {
        CallLogMapper m = getBaseMapper();
        UserOverviewVO vo = new UserOverviewVO();
        vo.setTodayCalls(m.countTodayByUser(userId));
        vo.setTodayTokens(m.sumTodayTokensByUser(userId));
        BigDecimal todayCost = m.sumTodayCostByUser(userId);
        vo.setTodayCost(todayCost != null ? todayCost : BigDecimal.ZERO);
        vo.setTotalCalls(m.countTotalByUser(userId));
        vo.setTotalTokens(m.sumTotalTokensByUser(userId));
        BigDecimal totalCost = m.sumTotalCostByUser(userId);
        vo.setTotalCost(totalCost != null ? totalCost : BigDecimal.ZERO);
        return vo;
    }
}
