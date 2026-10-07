package com.aitoken.controller;

import com.aitoken.common.Result;
import com.aitoken.dto.response.ChannelInfoVO;
import com.aitoken.entity.AiModel;
import com.aitoken.entity.Channel;
import com.aitoken.entity.SysConfig;
import com.aitoken.mapper.SysConfigMapper;
import com.aitoken.service.AiModelService;
import com.aitoken.service.ChannelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端：查看可用渠道及模型价格（不暴露 API Key / Base URL）
 */
@Tag(name = "用户渠道")
@RestController
@RequestMapping("/api/user/channels")
public class UserChannelController {

    @Resource
    private ChannelService channelService;

    @Resource
    private AiModelService aiModelService;

    @Resource
    private SysConfigMapper sysConfigMapper;

    private static final String DEFAULT_CHANNEL_KEY = "default_channel_id";

    /**
     * 获取所有启用渠道的公开信息和模型价格列表。
     * 用户可据此选择渠道，并在请求时附带 X-Channel-Id 头。
     */
    @Operation(summary = "获取可用渠道及价格列表")
    @GetMapping
    public Result<List<ChannelInfoVO>> list() {
        List<Channel> channels = channelService.listEnabled();
        List<ChannelInfoVO> result = new ArrayList<>();

        for (Channel channel : channels) {
            List<AiModel> models = aiModelService.listByChannel(channel.getId());
            // 仅展示启用状态的模型
            List<ChannelInfoVO.ModelItem> modelItems = models.stream()
                    .filter(m -> Integer.valueOf(1).equals(m.getStatus()))
                    .map(m -> {
                        ChannelInfoVO.ModelItem item = new ChannelInfoVO.ModelItem();
                        item.setModelId(m.getId());
                        item.setModelName(m.getModelName());
                        item.setAlias(m.getAlias());
                        item.setInputPrice(m.getInputPrice());
                        item.setOutputPrice(m.getOutputPrice());
                        item.setMaxTokens(m.getMaxTokens());
                        return item;
                    })
                    .collect(Collectors.toList());

            ChannelInfoVO vo = new ChannelInfoVO();
            vo.setChannelId(channel.getId());
            vo.setChannelName(channel.getName());
            vo.setModels(modelItems);
            result.add(vo);
        }

        return Result.ok(result);
    }

    @Operation(summary = "获取默认渠道ID（用于创建Key时预填）")
    @GetMapping("/default")
    public Result<Long> getDefault() {
        SysConfig cfg = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, DEFAULT_CHANNEL_KEY));
        if (cfg == null || cfg.getConfigValue() == null) return Result.ok(null);
        try { return Result.ok(Long.parseLong(cfg.getConfigValue())); }
        catch (NumberFormatException e) { return Result.ok(null); }
    }
}
