package com.aitoken.service;

import com.aitoken.dto.request.ModelSaveRequest;
import com.aitoken.entity.AiModel;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AiModelService extends IService<AiModel> {

    AiModel saveModel(ModelSaveRequest request);

    AiModel updateModel(Long id, ModelSaveRequest request);

    void deleteModel(Long id);

    List<AiModel> listByChannel(Long channelId);

    /**
     * 根据模型名或别名查找所有启用的模型（可能有多个渠道）
     */
    List<AiModel> listByNameOrAlias(String name);

    /**
     * 根据模型名或别名查找启用的模型
     * @deprecated 请改用 listByNameOrAlias + 加权随机
     */
    @Deprecated
    AiModel getByNameOrAlias(String name);
}
