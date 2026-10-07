package com.aitoken.service.impl;

import com.aitoken.common.exception.BizException;
import com.aitoken.dto.request.ModelSaveRequest;
import com.aitoken.entity.AiModel;
import com.aitoken.mapper.AiModelMapper;
import com.aitoken.service.AiModelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiModelServiceImpl extends ServiceImpl<AiModelMapper, AiModel> implements AiModelService {

    @Override
    public AiModel saveModel(ModelSaveRequest request) {
        AiModel model = new AiModel();
        fillModel(model, request);
        save(model);
        return model;
    }

    @Override
    public AiModel updateModel(Long id, ModelSaveRequest request) {
        AiModel model = getById(id);
        if (model == null) throw BizException.of("模型不存在");
        fillModel(model, request);
        updateById(model);
        return model;
    }

    @Override
    public void deleteModel(Long id) {
        removeById(id);
    }

    @Override
    public List<AiModel> listByChannel(Long channelId) {
        return list(new LambdaQueryWrapper<AiModel>()
                .eq(channelId != null, AiModel::getChannelId, channelId)
                .orderByAsc(AiModel::getModelName));
    }

    @Override
    public List<AiModel> listByNameOrAlias(String name) {
        return list(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getStatus, 1)
                .and(w -> w.eq(AiModel::getModelName, name)
                            .or()
                            .eq(AiModel::getAlias, name)));
    }

    @Override
    public AiModel getByNameOrAlias(String name) {
        return getOne(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getStatus, 1)
                .and(w -> w.eq(AiModel::getModelName, name)
                            .or()
                            .eq(AiModel::getAlias, name))
                .last("LIMIT 1"));
    }

    private void fillModel(AiModel model, ModelSaveRequest req) {
        model.setChannelId(req.getChannelId());
        model.setModelName(req.getModelName());
        model.setAlias(req.getAlias());
        model.setInputPrice(req.getInputPrice());
        model.setOutputPrice(req.getOutputPrice());
        model.setCacheCreationPrice(req.getCacheCreationPrice());
        model.setCacheReadPrice(req.getCacheReadPrice());
        model.setMaxTokens(req.getMaxTokens());
        model.setWeight(req.getWeight() != null && req.getWeight() > 0 ? req.getWeight() : 1);
        model.setStatus(1);
    }
}
