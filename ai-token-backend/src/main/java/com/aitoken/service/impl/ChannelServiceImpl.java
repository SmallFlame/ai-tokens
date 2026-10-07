package com.aitoken.service.impl;

import cn.hutool.json.JSONUtil;
import com.aitoken.common.exception.BizException;
import com.aitoken.dto.request.ChannelSaveRequest;
import com.aitoken.entity.Channel;
import com.aitoken.mapper.ChannelMapper;
import com.aitoken.service.ChannelService;
import com.aitoken.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ChannelServiceImpl extends ServiceImpl<ChannelMapper, Channel> implements ChannelService {

    @Resource
    private AesUtil aesUtil;

    @Override
    public Channel saveChannel(ChannelSaveRequest request) {
        Channel channel = buildChannel(new Channel(), request);
        save(channel);
        return channel;
    }

    @Override
    public Channel updateChannel(Long id, ChannelSaveRequest request) {
        Channel channel = getById(id);
        if (channel == null) throw BizException.of("渠道不存在");
        buildChannel(channel, request);
        updateById(channel);
        return channel;
    }

    @Override
    public void deleteChannel(Long id) {
        removeById(id);
    }

    @Override
    public List<Channel> listEnabled() {
        return list(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getStatus, 1)
                .orderByDesc(Channel::getPriority)
                .orderByDesc(Channel::getWeight));
    }

    @Override
    public Channel selectByModel(String modelName) {
        // 查找所有启用且健康的渠道, 其 models JSON 包含目标模型名
        List<Channel> channels = list(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getStatus, 1)
                .eq(Channel::getHealthStatus, 1)
                .orderByDesc(Channel::getPriority)
                .orderByDesc(Channel::getWeight));
        for (Channel channel : channels) {
            if (channel.getModels() != null) {
                List<String> models = JSONUtil.toList(channel.getModels(), String.class);
                if (models.contains(modelName)) {
                    return channel;
                }
            }
        }
        throw BizException.of("无可用渠道支持模型: " + modelName);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        lambdaUpdate().eq(Channel::getId, id).set(Channel::getStatus, status).update();
    }

    @Override
    public boolean healthCheck(Long id) {
        Channel channel = getById(id);
        if (channel == null) return false;
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build();
            Request req = new Request.Builder()
                    .url(channel.getBaseUrl())
                    .get()
                    .build();
            try (Response response = client.newCall(req).execute()) {
                boolean healthy = response.code() < 500;
                baseMapper.updateHealthStatus(id, healthy ? 1 : 0);
                return healthy;
            }
        } catch (Exception e) {
            log.warn("渠道健康检测失败: id={}, err={}", id, e.getMessage());
            baseMapper.updateHealthStatus(id, 0);
            return false;
        }
    }

    private Channel buildChannel(Channel channel, ChannelSaveRequest req) {
        channel.setName(req.getName());
        channel.setType(req.getType());
        channel.setBaseUrl(req.getBaseUrl());
        channel.setApiKey(aesUtil.encrypt(req.getApiKey()));
        if (req.getModels() != null) {
            channel.setModels(JSONUtil.toJsonStr(req.getModels()));
        }
        channel.setWeight(req.getWeight() == null ? 1 : req.getWeight());
        channel.setPriority(req.getPriority() == null ? 0 : req.getPriority());
        channel.setTimeoutMs(req.getTimeoutMs() == null ? 300000 : req.getTimeoutMs());
        channel.setRemark(req.getRemark());
        channel.setStatus(1);
        return channel;
    }
}
