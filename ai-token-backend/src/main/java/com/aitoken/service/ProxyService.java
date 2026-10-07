package com.aitoken.service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface ProxyService {

    /**
     * 转发请求到第三方 AI 渠道（OpenAI 兼容格式）。
     *
     * @param channelId 用户指定的渠道 ID（从 X-Channel-Id 请求头读取）；
     *                  传 null 时走原有负载均衡逻辑。
     */
    void forward(String apiKeyValue, String requestBody, HttpServletResponse response,
                 Long channelId, String clientIp) throws IOException;

    /**
     * 转发请求到 Anthropic 渠道（/v1/messages 格式）。
     *
     * @param channelId 用户指定的渠道 ID；传 null 时走原有负载均衡逻辑。
     */
    void forwardAnthropic(String apiKeyValue, String requestBody, HttpServletResponse response,
                          Long channelId, String clientIp) throws IOException;

    /**
     * 转发请求到 Responses API 渠道（/v1/responses 格式，Codex 使用）。
     * usage 结构与 Chat Completions 不同：input_tokens / output_tokens，
     * 流式时藏在 response.completed 事件的 response.usage 下。
     *
     * @param channelId 用户指定的渠道 ID；传 null 时走原有负载均衡逻辑。
     */
    void forwardResponses(String apiKeyValue, String requestBody, HttpServletResponse response,
                          Long channelId, String clientIp) throws IOException;
}
