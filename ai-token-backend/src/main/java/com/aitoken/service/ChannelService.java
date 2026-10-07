package com.aitoken.service;

import com.aitoken.dto.request.ChannelSaveRequest;
import com.aitoken.entity.Channel;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ChannelService extends IService<Channel> {

    Channel saveChannel(ChannelSaveRequest request);

    Channel updateChannel(Long id, ChannelSaveRequest request);

    void deleteChannel(Long id);

    List<Channel> listEnabled();

    /**
     * 根据模型名选择合适的渠道 (代理层路由用)
     */
    Channel selectByModel(String modelName);

    /**
     * 单独更新渠道启用/禁用状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 触发单个渠道健康检测
     */
    boolean healthCheck(Long id);
}
