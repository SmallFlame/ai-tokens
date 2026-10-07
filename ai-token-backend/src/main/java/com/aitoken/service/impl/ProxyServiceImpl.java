package com.aitoken.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aitoken.common.exception.BizException;
import com.aitoken.entity.AiModel;
import com.aitoken.entity.ApiKey;
import com.aitoken.entity.CallLog;
import com.aitoken.entity.Channel;
import com.aitoken.entity.Role;
import com.aitoken.service.*;
import com.aitoken.util.AesUtil;import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ProxyServiceImpl implements ProxyService {

    private final OkHttpClient baseClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .build();

    @Resource private ApiKeyService   apiKeyService;
    @Resource private ChannelService  channelService;
    @Resource private AiModelService  aiModelService;
    @Resource private CallLogService  callLogService;
    @Resource private UserService     userService;
    @Resource private RoleService     roleService;
    @Resource private AesUtil         aesUtil;
    @Resource private com.aitoken.mapper.ChannelMapper channelMapper;

    // ================================================================
    //  OpenAI  /v1/chat/completions  代理
    // ================================================================

    @Override
    public void forward(String apiKeyValue, String requestBody,
                        HttpServletResponse response, Long channelId, String clientIp) throws IOException {
        long startTime = System.currentTimeMillis();
        ApiKey apiKey = apiKeyService.getValidKey(apiKeyValue);
        checkMoneyBalance(apiKey);
        checkRealNameRegistration(apiKey);

        JSONObject reqJson  = JSONUtil.parseObj(requestBody);
        String modelName    = reqJson.getStr("model", "");
        boolean isStream    = reqJson.getBool("stream", false);
        if (modelName.isEmpty()) throw BizException.of(400, "请求体缺少 model 字段");

        // 优先级：X-Channel-Id 头 > API Key 绑定渠道 > 负载均衡
        Long effectiveChannelId = channelId != null ? channelId : apiKey.getChannelId();
        List<AiModel> candidates = effectiveChannelId != null
                ? getFixedChannelCandidates(modelName, effectiveChannelId)
                : getCandidates(modelName);

        ChannelFailException lastFail = null;
        for (int i = 0; i < candidates.size(); i++) {
            AiModel aiModel  = candidates.get(i);
            Channel channel  = channelService.getById(aiModel.getChannelId());
            boolean isLast   = (i == candidates.size() - 1);

            // 别名映射 + 流式时注入 stream_options 以获取 usage 统计
            String realModel = aiModel.getModelName();
            String body = requestBody;
            if (!modelName.equals(realModel) || isStream) {
                JSONObject copy = JSONUtil.parseObj(requestBody);
                if (!modelName.equals(realModel)) copy.set("model", realModel);
                if (isStream) {
                    JSONObject opts = copy.getJSONObject("stream_options");
                    if (opts == null) opts = new JSONObject();
                    opts.set("include_usage", true);
                    copy.set("stream_options", opts);
                }
                body = copy.toString();
            }

            CallLog callLog = buildCallLog(apiKey, channel, aiModel, modelName, realModel, isStream, clientIp);

            String channelKey = aesUtil.decrypt(channel.getApiKey());
            String url = channel.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
            Request okReq = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(body, MediaType.parse("application/json; charset=utf-8")))
                    .header("Authorization", "Bearer " + channelKey)
                    .header("Content-Type", "application/json")
                    .build();
            OkHttpClient client = baseClient.newBuilder()
                    .readTimeout(channel.getTimeoutMs(), TimeUnit.MILLISECONDS)
                    .writeTimeout(channel.getTimeoutMs(), TimeUnit.MILLISECONDS)
                    .build();

            try {
                if (isStream) doStream(client, okReq, response, callLog, apiKey, aiModel, startTime);
                else          doSync  (client, okReq, response, callLog, apiKey, aiModel, startTime);
                return; // 成功
            } catch (ChannelFailException e) {
                lastFail = e;
                log.warn("渠道[{}]返回 {}，{}", channel.getName(), e.status,
                        isLast ? "已无备用渠道" : "切换到下一优先级渠道");
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.error("渠道[{}]转发异常，{}", channel.getName(),
                        isLast ? "已无备用渠道" : "切换下一渠道", e);
                fillFailLog(callLog, startTime, e.getMessage());
                callLogService.asyncSave(callLog);
                if (isLast) throw new IOException(e.getMessage(), e);
            }
        }

        // 所有候选都失败，写最后一次的错误响应给客户端
        if (lastFail != null && !response.isCommitted()) {
            response.setStatus(lastFail.status);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write(lastFail.body);
        }
    }

    // ================================================================
    //  Anthropic  /v1/messages  代理
    // ================================================================

    @Override
    public void forwardAnthropic(String apiKeyValue, String requestBody,
                                  HttpServletResponse response, Long channelId, String clientIp) throws IOException {
        long startTime = System.currentTimeMillis();
        ApiKey apiKey = apiKeyService.getValidKey(apiKeyValue);
        checkMoneyBalance(apiKey);
        checkRealNameRegistration(apiKey);

        JSONObject reqJson = JSONUtil.parseObj(requestBody);
        String modelName   = reqJson.getStr("model", "");
        boolean isStream   = reqJson.getBool("stream", false);
        if (modelName.isEmpty()) throw BizException.of(400, "请求体缺少 model 字段");

        // 优先级：X-Channel-Id 头 > API Key 绑定渠道 > 负载均衡
        Long effectiveChannelId = channelId != null ? channelId : apiKey.getChannelId();
        List<AiModel> candidates = effectiveChannelId != null
                ? getFixedChannelCandidates(modelName, effectiveChannelId)
                : getCandidates(modelName);

        ChannelFailException lastFail = null;
        for (int i = 0; i < candidates.size(); i++) {
            AiModel aiModel  = candidates.get(i);
            Channel channel  = channelService.getById(aiModel.getChannelId());
            boolean isLast   = (i == candidates.size() - 1);

            String realModel = aiModel.getModelName();
            String body = requestBody;
            if (!modelName.equals(realModel)) {
                JSONObject copy = JSONUtil.parseObj(requestBody);
                copy.set("model", realModel);
                body = copy.toString();
            }

            CallLog callLog = buildCallLog(apiKey, channel, aiModel, modelName, realModel, isStream, clientIp);

            String channelKey = aesUtil.decrypt(channel.getApiKey());
            String url = channel.getBaseUrl().replaceAll("/+$", "") + "/v1/messages";
            Request okReq = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(body, MediaType.parse("application/json; charset=utf-8")))
                    .header("x-api-key", channelKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .build();
            OkHttpClient client = baseClient.newBuilder()
                    .readTimeout(channel.getTimeoutMs(), TimeUnit.MILLISECONDS)
                    .writeTimeout(channel.getTimeoutMs(), TimeUnit.MILLISECONDS)
                    .build();

            // 调试：记录实际转发用的模型名
            log.info("Anthropic 转发 → {} | 请求模型={} | 实际转发模型={} | stream={}",
                    url, modelName, realModel, isStream);

            try {
                if (isStream) doAnthropicStream(client, okReq, response, callLog, apiKey, aiModel, startTime);
                else          doAnthropicSync  (client, okReq, response, callLog, apiKey, aiModel, startTime);
                return;
            } catch (ChannelFailException e) {
                lastFail = e;
                log.warn("Anthropic 渠道[{}]返回 {}，{} | 上游响应: {}",
                        channel.getName(), e.status,
                        isLast ? "已无备用渠道" : "切换到下一优先级渠道",
                        truncate(e.body, 1000));
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.error("Anthropic 渠道[{}]转发异常，{}", channel.getName(),
                        isLast ? "已无备用渠道" : "切换下一渠道", e);
                fillFailLog(callLog, startTime, e.getMessage());
                callLogService.asyncSave(callLog);
                if (isLast) throw new IOException(e.getMessage(), e);
            }
        }

        if (lastFail != null && !response.isCommitted()) {
            response.setStatus(lastFail.status);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write(lastFail.body);
        }
    }

    // ================================================================
    //  OpenAI Responses  /v1/responses  代理（Codex 使用）
    // ================================================================
    @Override
    public void forwardResponses(String apiKeyValue, String requestBody,
                                  HttpServletResponse response, Long channelId, String clientIp) throws IOException {
        long startTime = System.currentTimeMillis();
        ApiKey apiKey = apiKeyService.getValidKey(apiKeyValue);
        checkMoneyBalance(apiKey);
        checkRealNameRegistration(apiKey);

        JSONObject reqJson = JSONUtil.parseObj(requestBody);
        String modelName   = reqJson.getStr("model", "");
        boolean isStream   = reqJson.getBool("stream", false);
        if (modelName.isEmpty()) throw BizException.of(400, "请求体缺少 model 字段");

        // 优先级：X-Channel-Id 头 > API Key 绑定渠道 > 负载均衡
        Long effectiveChannelId = channelId != null ? channelId : apiKey.getChannelId();
        List<AiModel> candidates = effectiveChannelId != null
                ? getFixedChannelCandidates(modelName, effectiveChannelId)
                : getCandidates(modelName);

        ChannelFailException lastFail = null;
        for (int i = 0; i < candidates.size(); i++) {
            AiModel aiModel  = candidates.get(i);
            Channel channel  = channelService.getById(aiModel.getChannelId());
            boolean isLast   = (i == candidates.size() - 1);

            String realModel = aiModel.getModelName();
            String body = requestBody;
            if (!modelName.equals(realModel)) {
                JSONObject copy = JSONUtil.parseObj(requestBody);
                copy.set("model", realModel);
                body = copy.toString();
            }

            CallLog callLog = buildCallLog(apiKey, channel, aiModel, modelName, realModel, isStream, clientIp);

            String channelKey = aesUtil.decrypt(channel.getApiKey());
            String url = channel.getBaseUrl().replaceAll("/+$", "") + "/responses";
            Request okReq = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(body, MediaType.parse("application/json; charset=utf-8")))
                    .header("Authorization", "Bearer " + channelKey)
                    .header("Content-Type", "application/json")
                    .build();
            OkHttpClient client = baseClient.newBuilder()
                    .readTimeout(channel.getTimeoutMs(), TimeUnit.MILLISECONDS)
                    .writeTimeout(channel.getTimeoutMs(), TimeUnit.MILLISECONDS)
                    .build();

            // 调试（仅 debug 级别）：记录转发目标与请求体摘要
            log.debug("Responses 转发 → {} | model={} → {} | stream={} | bodyLen={}",
                    url, modelName, realModel, isStream, body.length());
            log.debug("Responses 请求体: {}", body);

            try {
                if (isStream) doResponsesStream(client, okReq, response, callLog, apiKey, aiModel, startTime);
                else          doResponsesSync  (client, okReq, response, callLog, apiKey, aiModel, startTime);
                return;
            } catch (ChannelFailException e) {
                lastFail = e;
                // 记录上游原始报错，便于定位是模型名错、认证错还是上游限流
                log.warn("Responses 渠道[{}]返回 {}，{} | 上游响应: {}",
                        channel.getName(), e.status,
                        isLast ? "已无备用渠道" : "切换到下一优先级渠道",
                        truncate(e.body, 1000));
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.error("Responses 渠道[{}]转发异常，{}", channel.getName(),
                        isLast ? "已无备用渠道" : "切换下一渠道", e);
                fillFailLog(callLog, startTime, e.getMessage());
                callLogService.asyncSave(callLog);
                if (isLast) throw new IOException(e.getMessage(), e);
            }
        }

        if (lastFail != null && !response.isCommitted()) {
            response.setStatus(lastFail.status);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write(lastFail.body);
        }
    }

    // ----------------------------------------------------------------
    //  OpenAI 非流式
    // ----------------------------------------------------------------
    private void doSync(OkHttpClient client, Request okReq,
                        HttpServletResponse response,
                        CallLog callLog, ApiKey apiKey, AiModel aiModel,
                        long startTime) throws IOException {
        try (Response okResp = client.newCall(okReq).execute()) {
            callLog.setHttpStatus(okResp.code());
            callLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
            String body = okResp.body() != null ? okResp.body().string() : "{}";

            if (!okResp.isSuccessful()) {
                callLog.setStatus(0);
                callLog.setErrorMsg(truncate(body, 400));
                callLogService.asyncSave(callLog);
                throw new ChannelFailException(okResp.code(), body); // 触发重试
            }

            parseAndFillUsage(body, callLog, aiModel);
            callLog.setStatus(1);
            updateQuota(apiKey, callLog.getTotalTokens(), callLog.getCost(), callLog.getChannelId());
            callLogService.asyncSave(callLog);

            response.setStatus(okResp.code());
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write(body);
        }
    }

    // ----------------------------------------------------------------
    //  OpenAI 流式
    // ----------------------------------------------------------------
    private void doStream(OkHttpClient client, Request okReq,
                          HttpServletResponse response,
                          CallLog callLog, ApiKey apiKey, AiModel aiModel,
                          long startTime) throws IOException {
        try (Response okResp = client.newCall(okReq).execute()) {
            callLog.setHttpStatus(okResp.code());

            if (!okResp.isSuccessful()) {
                // 流式建连后未输出任何数据，仍可重试
                String errorBody = okResp.body() != null ? okResp.body().string() : "{}";
                callLog.setStatus(0);
                callLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
                callLog.setErrorMsg(truncate(errorBody, 400));
                callLogService.asyncSave(callLog);
                throw new ChannelFailException(okResp.code(), errorBody);
            }

            // 开始向客户端写数据，之后无法重试
            response.setStatus(okResp.code());
            response.setContentType("text/event-stream; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("X-Accel-Buffering", "no");

            if (okResp.body() == null) { callLogService.asyncSave(callLog); return; }

            PrintWriter writer = response.getWriter();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(okResp.body().byteStream(), StandardCharsets.UTF_8));

            int promptTokens = 0, completionTokens = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                writer.println(line);
                writer.flush();
                if (line.startsWith("data:") && !line.contains("[DONE]")) {
                    try {
                        JSONObject chunk = JSONUtil.parseObj(line.substring(5).trim());
                        JSONObject usage = chunk.getJSONObject("usage");
                        if (usage != null) {
                            promptTokens     = usage.getInt("prompt_tokens", 0);
                            completionTokens = usage.getInt("completion_tokens", 0);
                        }
                    } catch (Exception ignored) {}
                }
            }

            callLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
            callLog.setPromptTokens(promptTokens);
            callLog.setCompletionTokens(completionTokens);
            callLog.setTotalTokens(promptTokens + completionTokens);
            callLog.setStatus(1);
            if (callLog.getTotalTokens() > 0) callLog.setCost(calcCost(aiModel, promptTokens, completionTokens));
            updateQuota(apiKey, callLog.getTotalTokens(), callLog.getCost(), callLog.getChannelId());
            callLogService.asyncSave(callLog);
        }
    }

    // ----------------------------------------------------------------
    //  Anthropic 非流式
    // ----------------------------------------------------------------
    private void doAnthropicSync(OkHttpClient client, Request okReq,
                                  HttpServletResponse response,
                                  CallLog callLog, ApiKey apiKey, AiModel aiModel,
                                  long startTime) throws IOException {
        try (Response okResp = client.newCall(okReq).execute()) {
            callLog.setHttpStatus(okResp.code());
            callLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
            String body = okResp.body() != null ? okResp.body().string() : "{}";

            if (!okResp.isSuccessful()) {
                callLog.setStatus(0);
                callLog.setErrorMsg(truncate(body, 400));
                callLogService.asyncSave(callLog);
                throw new ChannelFailException(okResp.code(), body);
            }

            parseAndFillAnthropicUsage(body, callLog, aiModel);
            callLog.setStatus(1);
            updateQuota(apiKey, callLog.getTotalTokens(), callLog.getCost(), callLog.getChannelId());
            callLogService.asyncSave(callLog);

            response.setStatus(okResp.code());
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write(body);
        }
    }

    // ----------------------------------------------------------------
    //  Anthropic 流式
    // ----------------------------------------------------------------
    private void doAnthropicStream(OkHttpClient client, Request okReq,
                                    HttpServletResponse response,
                                    CallLog callLog, ApiKey apiKey, AiModel aiModel,
                                    long startTime) throws IOException {
        try (Response okResp = client.newCall(okReq).execute()) {
            callLog.setHttpStatus(okResp.code());

            if (!okResp.isSuccessful()) {
                String errorBody = okResp.body() != null ? okResp.body().string() : "{}";
                callLog.setStatus(0);
                callLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
                callLog.setErrorMsg(truncate(errorBody, 400));
                callLogService.asyncSave(callLog);
                throw new ChannelFailException(okResp.code(), errorBody);
            }

            response.setStatus(okResp.code());
            response.setContentType("text/event-stream; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("X-Accel-Buffering", "no");

            if (okResp.body() == null) { callLogService.asyncSave(callLog); return; }

            PrintWriter writer = response.getWriter();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(okResp.body().byteStream(), StandardCharsets.UTF_8));

            int inputTokens = 0, outputTokens = 0, cacheCreationTokens = 0, cacheReadTokens = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                writer.println(line);
                writer.flush();
                if (line.startsWith("data:")) {
                    try {
                        JSONObject chunk = JSONUtil.parseObj(line.substring(5).trim());
                        String type = chunk.getStr("type");
                        if ("message_start".equals(type)) {
                            JSONObject msg = chunk.getJSONObject("message");
                            if (msg != null) {
                                JSONObject usage = msg.getJSONObject("usage");
                                if (usage != null) {
                                    inputTokens         = usage.getInt("input_tokens", 0);
                                    cacheCreationTokens = usage.getInt("cache_creation_input_tokens", 0);
                                    cacheReadTokens     = usage.getInt("cache_read_input_tokens", 0);
                                }
                            }
                        } else if ("message_delta".equals(type)) {
                            JSONObject usage = chunk.getJSONObject("usage");
                            if (usage != null) outputTokens = usage.getInt("output_tokens", 0);
                        }
                    } catch (Exception ignored) {}
                }
            }

            callLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
            callLog.setPromptTokens(inputTokens);
            callLog.setCompletionTokens(outputTokens);
            callLog.setCacheCreationTokens(cacheCreationTokens);
            callLog.setCacheReadTokens(cacheReadTokens);
            callLog.setTotalTokens(inputTokens + cacheCreationTokens + cacheReadTokens + outputTokens);
            callLog.setStatus(1);
            if (callLog.getTotalTokens() > 0) callLog.setCost(calcCost(aiModel, inputTokens, cacheCreationTokens, cacheReadTokens, outputTokens));
            updateQuota(apiKey, callLog.getTotalTokens(), callLog.getCost(), callLog.getChannelId());
            callLogService.asyncSave(callLog);
        }
    }

    // ----------------------------------------------------------------
    //  Responses 非流式
    // ----------------------------------------------------------------
    private void doResponsesSync(OkHttpClient client, Request okReq,
                                  HttpServletResponse response,
                                  CallLog callLog, ApiKey apiKey, AiModel aiModel,
                                  long startTime) throws IOException {
        try (Response okResp = client.newCall(okReq).execute()) {
            callLog.setHttpStatus(okResp.code());
            callLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
            String body = okResp.body() != null ? okResp.body().string() : "{}";

            if (!okResp.isSuccessful()) {
                callLog.setStatus(0);
                callLog.setErrorMsg(truncate(body, 400));
                callLogService.asyncSave(callLog);
                throw new ChannelFailException(okResp.code(), body);
            }

            parseAndFillResponsesUsage(body, callLog, aiModel);
            callLog.setStatus(1);
            updateQuota(apiKey, callLog.getTotalTokens(), callLog.getCost(), callLog.getChannelId());
            callLogService.asyncSave(callLog);

            response.setStatus(okResp.code());
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write(body);
        }
    }

    // ----------------------------------------------------------------
    //  Responses 流式（SSE 带 event 行，usage 在 response.completed 事件里）
    // ----------------------------------------------------------------
    private void doResponsesStream(OkHttpClient client, Request okReq,
                                    HttpServletResponse response,
                                    CallLog callLog, ApiKey apiKey, AiModel aiModel,
                                    long startTime) throws IOException {
        try (Response okResp = client.newCall(okReq).execute()) {
            callLog.setHttpStatus(okResp.code());

            if (!okResp.isSuccessful()) {
                String errorBody = okResp.body() != null ? okResp.body().string() : "{}";
                callLog.setStatus(0);
                callLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
                callLog.setErrorMsg(truncate(errorBody, 400));
                callLogService.asyncSave(callLog);
                throw new ChannelFailException(okResp.code(), errorBody);
            }

            response.setStatus(okResp.code());
            response.setContentType("text/event-stream; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("X-Accel-Buffering", "no");

            if (okResp.body() == null) { callLogService.asyncSave(callLog); return; }

            PrintWriter writer = response.getWriter();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(okResp.body().byteStream(), StandardCharsets.UTF_8));

            int inputTokens = 0, outputTokens = 0, cacheReadTokens = 0;
            boolean streamError = false;
            String line;
            while ((line = reader.readLine()) != null) {
                writer.println(line);   // 含 event: 行，原样透传
                writer.flush();
                if (line.startsWith("data:")) {
                    try {
                        JSONObject chunk = JSONUtil.parseObj(line.substring(5).trim());
                        String type = chunk.getStr("type", "");
                        if ("response.completed".equals(type)) {
                            JSONObject resp = chunk.getJSONObject("response");
                            if (resp != null) {
                                JSONObject usage = resp.getJSONObject("usage");
                                if (usage != null) {
                                    inputTokens  = usage.getInt("input_tokens", 0);
                                    outputTokens = usage.getInt("output_tokens", 0);
                                    JSONObject details = usage.getJSONObject("input_tokens_details");
                                    if (details != null) cacheReadTokens = details.getInt("cached_tokens", 0);
                                }
                            }
                        } else if (type.contains("error") || type.contains("failed")
                                || chunk.get("error") != null) {
                            // 上游以 200 + error 事件的方式报错，这里记下来便于排查
                            streamError = true;
                            log.warn("Responses 流内错误事件: {}", truncate(line, 1000));
                        }
                    } catch (Exception ignored) {}
                }
            }

            // 缓存命中部分单独计费，普通输入需扣掉缓存部分
            int normalInput = Math.max(0, inputTokens - cacheReadTokens);
            callLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
            callLog.setPromptTokens(normalInput);
            callLog.setCompletionTokens(outputTokens);
            callLog.setCacheReadTokens(cacheReadTokens);
            callLog.setTotalTokens(normalInput + cacheReadTokens + outputTokens);
            callLog.setStatus(streamError ? 0 : 1);
            if (callLog.getTotalTokens() > 0)
                callLog.setCost(calcCost(aiModel, normalInput, 0, cacheReadTokens, outputTokens));
            updateQuota(apiKey, callLog.getTotalTokens(), callLog.getCost(), callLog.getChannelId());
            callLogService.asyncSave(callLog);
            log.debug("Responses 流式完成 | model={} | in={} out={} cache={} | 耗时={}ms | {}",
                    aiModel.getModelName(), normalInput, outputTokens, cacheReadTokens,
                    System.currentTimeMillis() - startTime, streamError ? "流内有错误" : "正常");
        }
    }

    // ================================================================
    //  路由：优先级分组 + 组内加权随机
    // ================================================================

    /**
     * 按渠道 priority 降序分组，组内加权随机选一个，返回有序候选列表。
     * 调用方依次尝试，失败则切换下一个（跨优先级降级）。
     */
    private List<AiModel> getCandidates(String modelName) {
        List<AiModel> all = aiModelService.listByNameOrAlias(modelName);
        if (all.isEmpty()) throw BizException.of(400, "模型未配置，请在模型管理中添加: " + modelName);

        // 按渠道 priority 降序分组（priority 越大越优先）
        TreeMap<Integer, List<AiModel>> byPriority = new TreeMap<>(Comparator.reverseOrder());
        for (AiModel m : all) {
            Channel c = channelService.getById(m.getChannelId());
            if (c != null && c.getStatus() == 1) {
                int p = c.getPriority() != null ? c.getPriority() : 0;
                byPriority.computeIfAbsent(p, k -> new ArrayList<>()).add(m);
            }
        }

        if (byPriority.isEmpty()) throw BizException.of(503, "模型对应的所有渠道均不可用: " + modelName);

        // 每个优先级组内加权随机选一个，组成有序候选列表
        List<AiModel> ordered = new ArrayList<>();
        for (List<AiModel> group : byPriority.values()) {
            ordered.add(weightedRandom(group));
        }
        return ordered;
    }

    /** 组内按 weight 加权随机选一条，weight <= 0 视为 1 */
    private AiModel weightedRandom(List<AiModel> models) {
        int total = models.stream()
                .mapToInt(m -> (m.getWeight() != null && m.getWeight() > 0) ? m.getWeight() : 1)
                .sum();
        int rand = ThreadLocalRandom.current().nextInt(total);
        int cum = 0;
        for (AiModel m : models) {
            cum += (m.getWeight() != null && m.getWeight() > 0) ? m.getWeight() : 1;
            if (rand < cum) return m;
        }
        return models.get(models.size() - 1);
    }

    /**
     * 用户指定渠道时，直接在该渠道内匹配模型，不进行跨渠道负载均衡。
     * 若渠道不存在/已禁用，或该渠道不支持所请求的模型，则抛出 BizException。
     */
    private List<AiModel> getFixedChannelCandidates(String modelName, Long channelId) {
        Channel channel = channelService.getById(channelId);
        if (channel == null || channel.getStatus() != 1) {
            throw BizException.of(400, "指定渠道不存在或已禁用，渠道ID: " + channelId);
        }

        List<AiModel> models = aiModelService.listByChannel(channelId);
        AiModel found = models.stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                .filter(m -> modelName.equals(m.getModelName()) || modelName.equals(m.getAlias()))
                .findFirst()
                .orElseThrow(() -> BizException.of(400,
                        "渠道[" + channel.getName() + "]不支持该模型: " + modelName));

        return Collections.singletonList(found);
    }

    // ================================================================
    //  工具方法
    // ================================================================

    private CallLog buildCallLog(ApiKey apiKey, Channel channel, AiModel aiModel,
                                  String modelName, String realModel, boolean isStream, String clientIp) {
        CallLog cl = new CallLog();
        cl.setUserId(apiKey.getUserId());
        cl.setApiKeyId(apiKey.getId());
        cl.setChannelId(channel.getId());
        cl.setChannelName(channel.getName());
        cl.setModelName(modelName);
        cl.setRealModel(realModel);
        cl.setRequestId(IdUtil.simpleUUID());
        cl.setClientIp(clientIp);
        cl.setIsStream(isStream ? 1 : 0);
        cl.setPromptTokens(0);
        cl.setCompletionTokens(0);
        cl.setCacheCreationTokens(0);
        cl.setCacheReadTokens(0);
        cl.setTotalTokens(0);
        cl.setCost(BigDecimal.ZERO);
        return cl;
    }

    private void parseAndFillUsage(String body, CallLog callLog, AiModel aiModel) {
        try {
            JSONObject usage = JSONUtil.parseObj(body).getJSONObject("usage");
            if (usage != null) {
                int pt = usage.getInt("prompt_tokens", 0);
                int ct = usage.getInt("completion_tokens", 0);
                callLog.setPromptTokens(pt);
                callLog.setCompletionTokens(ct);
                callLog.setTotalTokens(pt + ct);
                if (aiModel != null) callLog.setCost(calcCost(aiModel, pt, ct));
            }
        } catch (Exception e) { log.warn("解析 OpenAI usage 失败", e); }
    }

    /** Responses API usage: input_tokens / output_tokens，缓存命中在 input_tokens_details.cached_tokens */
    private void parseAndFillResponsesUsage(String body, CallLog callLog, AiModel aiModel) {
        try {
            JSONObject usage = JSONUtil.parseObj(body).getJSONObject("usage");
            if (usage != null) {
                int it = usage.getInt("input_tokens", 0);
                int ot = usage.getInt("output_tokens", 0);
                int cr = 0;
                JSONObject details = usage.getJSONObject("input_tokens_details");
                if (details != null) cr = details.getInt("cached_tokens", 0);

                int normalInput = Math.max(0, it - cr);
                callLog.setPromptTokens(normalInput);
                callLog.setCompletionTokens(ot);
                callLog.setCacheReadTokens(cr);
                callLog.setTotalTokens(normalInput + cr + ot);
                if (aiModel != null) callLog.setCost(calcCost(aiModel, normalInput, 0, cr, ot));
            }
        } catch (Exception e) { log.warn("解析 Responses usage 失败", e); }
    }

    private void parseAndFillAnthropicUsage(String body, CallLog callLog, AiModel aiModel) {
        try {
            JSONObject usage = JSONUtil.parseObj(body).getJSONObject("usage");
            if (usage != null) {
                int pt = usage.getInt("input_tokens", 0);
                int ct = usage.getInt("output_tokens", 0);
                int cc = usage.getInt("cache_creation_input_tokens", 0);
                int cr = usage.getInt("cache_read_input_tokens", 0);
                callLog.setPromptTokens(pt);
                callLog.setCompletionTokens(ct);
                callLog.setCacheCreationTokens(cc);
                callLog.setCacheReadTokens(cr);
                callLog.setTotalTokens(pt + cc + cr + ct);
                if (aiModel != null) callLog.setCost(calcCost(aiModel, pt, cc, cr, ct));
            }
        } catch (Exception e) { log.warn("解析 Anthropic usage 失败", e); }
    }

    private BigDecimal calcCost(AiModel model, int promptTokens, int completionTokens) {
        return calcCost(model, promptTokens, 0, 0, completionTokens);
    }

    /**
     * 四类 token 独立计费：
     *   - 普通输入：inputPrice
     *   - 缓存创建：cacheCreationPrice（null → inputPrice）
     *   - 缓存命中：cacheReadPrice（null → inputPrice × 0.1）
     *   - 输出：outputPrice
     */
    private BigDecimal calcCost(AiModel model, int promptTokens, int cacheCreationTokens,
                                 int cacheReadTokens, int completionTokens) {
        BigDecimal inputP = model.getInputPrice();
        BigDecimal cacheCreationP = model.getCacheCreationPrice() != null
                ? model.getCacheCreationPrice() : inputP;
        BigDecimal cacheReadP = model.getCacheReadPrice() != null
                ? model.getCacheReadPrice() : inputP.multiply(new BigDecimal("0.1"));
        BigDecimal outputP = model.getOutputPrice();

        BigDecimal inCost = inputP
                .multiply(BigDecimal.valueOf(promptTokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
        BigDecimal cacheCreationCost = cacheCreationP
                .multiply(BigDecimal.valueOf(cacheCreationTokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
        BigDecimal cacheReadCost = cacheReadP
                .multiply(BigDecimal.valueOf(cacheReadTokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
        BigDecimal outCost = outputP
                .multiply(BigDecimal.valueOf(completionTokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
        return inCost.add(cacheCreationCost).add(cacheReadCost).add(outCost);
    }

    private void checkMoneyBalance(ApiKey apiKey) {
        // Token额度型 Key 不做余额检查
        if (Integer.valueOf(1).equals(apiKey.getKeyType())) return;

        // 金额消费型：检查用户余额
        com.aitoken.entity.User user = userService.getById(apiKey.getUserId());
        if (user == null) return;
        // 超级管理员和导师不做余额检查（API Key 调用没有登录 Session，直接从数据库查角色）
        if (user.getRoleId() != null) {
            Role role = roleService.getById(user.getRoleId());
            if (role != null && ("super_admin".equals(role.getCode()) || "advisor".equals(role.getCode()))) return;
        }
        BigDecimal balance = user.getMoneyQuota();
        // null 或 -1 表示不限制，跳过
        if (balance == null || balance.compareTo(new BigDecimal("-1")) == 0) return;
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.of(429, "余额不足，请联系管理员充值");
        }
    }

    private void checkRealNameRegistration(ApiKey apiKey) {
        com.aitoken.entity.User user = userService.getById(apiKey.getUserId());
        if (user == null) return;
        // 超级管理员和导师不做实名检查（API Key 调用没有登录 Session，直接从数据库查角色）
        if (user.getRoleId() != null) {
            Role role = roleService.getById(user.getRoleId());
            if (role != null && ("super_admin".equals(role.getCode()) || "advisor".equals(role.getCode()))) return;
        }
        // 检查真实姓名和邮箱是否已填写
        if (user.getNickname() == null || user.getNickname().trim().isEmpty() ||
            user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw BizException.of(403, "账号未完成实名注册，无法调用 Token。请登录后进入【个人中心】填写真实姓名和邮箱。");
        }
    }

    private void updateQuota(ApiKey apiKey, int totalTokens, BigDecimal cost, Long channelId) {
        boolean isTokenType = Integer.valueOf(1).equals(apiKey.getKeyType());
        if (isTokenType) {
            // Token额度型: 只累加 usedQuota
            if (totalTokens > 0) {
                try { apiKeyService.addUsedQuota(apiKey.getId(), totalTokens); }
                catch (Exception e) { log.warn("更新 API Key Token 用量失败 id={}", apiKey.getId(), e); }
            }
        } else {
            // 金额消费型: 扣减用户余额 + 累加统计（unlimited 用户 deduct 不生效但统计仍记录）
            if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
                try { userService.deductMoneyQuota(apiKey.getUserId(), cost); }
                catch (Exception e) { log.warn("扣减用户余额失败 userId={}", apiKey.getUserId(), e); }
                try { userService.addUsedMoney(apiKey.getUserId(), cost); }
                catch (Exception e) { log.warn("更新用户金额统计失败 userId={}", apiKey.getUserId(), e); }
                try { apiKeyService.addUsedMoney(apiKey.getId(), cost); }
                catch (Exception e) { log.warn("更新 API Key 金额统计失败 id={}", apiKey.getId(), e); }
            }
        }
        // 无论哪种 Key 类型，均累加渠道已用金额
        if (channelId != null && cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
            try { channelMapper.addUsedMoney(channelId, cost); }
            catch (Exception e) { log.warn("更新渠道用量失败 channelId={}", channelId, e); }
        }
    }

    private void fillFailLog(CallLog callLog, long startTime, String errorMsg) {
        callLog.setStatus(0);
        callLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
        callLog.setErrorMsg(truncate(errorMsg, 400));
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    // ----------------------------------------------------------------
    //  内部异常：渠道级失败，可被上层捕获后重试
    // ----------------------------------------------------------------
    private static class ChannelFailException extends RuntimeException {
        final int    status;
        final String body;
        ChannelFailException(int status, String body) {
            super("upstream " + status);
            this.status = status;
            this.body   = body;
        }
    }
}
