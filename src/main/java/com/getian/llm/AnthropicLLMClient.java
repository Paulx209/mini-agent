package com.getian.llm;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getian.core.*;
import com.getian.tool.ToolDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnthropicLLMClient implements LLMClient {
    private final AnthropicConfig config;

    public AnthropicLLMClient(AnthropicConfig config) {
        this.config = config;
    }


    @Override
    public AssistantMessage chat(List<Message> messageList, List<ToolDefinition> tools) {
        //1.请求地址
        String url = messageUrl();
        //2.请求体
        String requestBody = JSON.toJSONString(toRequestJson(messageList, tools));
        HttpRequest request = HttpRequest.post(url).body(requestBody);
        //3.请求头
        Map<String, String> headersMap = requestHeaders();
        for (Map.Entry<String, String> entry : headersMap.entrySet()) {
            request.header(entry.getKey(), entry.getValue());
        }
        //4.执行
        HttpResponse response = request.execute();

        //5.解析response
        return parseResponse(response.body());
    }

    public AssistantMessage parseResponse(String responseBody) {
        JSONObject resp = JSON.parseObject(responseBody);
        String stopReason = resp.getString("stop_reason");
        JSONArray content = resp.getJSONArray("content");
        List<ContentBlock> contentBlocks = new ArrayList<>();
        if (content != null) {
            for (int i = 0; i < content.size(); i++) {
                JSONObject block = content.getJSONObject(i);
                String type = block.getString("type");
                if ("text".equals(type)) {
                    contentBlocks.add(new TextBlock(valueOrEmpty(block, "text")));
                } else if ("thinking".equals(type)) {
                    String thinking = valueOrEmpty(block, "thinking");
                    String signature = valueOrEmpty(block, "signature");
                    contentBlocks.add(new ThinkingBlock(thinking, signature));
                } else if ("tool_use".equals(type)) {
                    JSONObject input = block.getJSONObject("input");
                    contentBlocks.add(new ToolUseBlock(valueOrEmpty(block, "id"), valueOrEmpty(block, "name"), input));
                }
            }
        }
        return new AssistantMessage(contentBlocks, stopReason);
    }

    private String valueOrEmpty(JSONObject jsonObject, String key) {
        return jsonObject.getString(key) == null ? "" : jsonObject.getString(key);
    }

    /**
     * 构建请求头  apiKey | contentType即可
     *
     * @return header
     */
    public Map<String, String> requestHeaders() {
        Map<String, String> header = new LinkedHashMap<>();
        header.put("x-api-key", config.getApiKey());
        header.put("content-type", "application/json");
        return header;
    }

    /**
     * 构建请求路径
     *
     * @return
     */
    public String messageUrl() {
        String baseUrl = config.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "v1/messages";
        }
        baseUrl = baseUrl + "/v1/messages";
        return baseUrl;
    }

    public JSONObject toRequestJson(List<Message> messageList, List<ToolDefinition> tools) {
        JSONObject request = new JSONObject();
        request.put("model", config.getModel());
        request.put("max_tokens", config.getMaxTokens());
        request.put("messages", toMessageListJson(messageList));
        if (config.getSystemPrompt() != null && !config.getSystemPrompt().isEmpty()) {
            request.put("system", config.getSystemPrompt());
        }
        if (tools != null && !tools.isEmpty()) {
            request.put("tools", toToolsJson(tools));
        }
        return request;
    }

    private JSONArray toMessageListJson(List<Message> messageList) {
        JSONArray messageArray = new JSONArray();
        for (Message message : messageList) {
            messageArray.fluentAdd(new JSONObject()
                    .fluentPut("role", message.getRole())
                    .fluentPut("content", toContentJson(message.getContent())));
        }
        return messageArray;
    }

    private JSONArray toContentJson(List<ContentBlock> content) {
        JSONArray contentJson = new JSONArray();
        for (ContentBlock contentBlock : content) {
            JSONObject object = new JSONObject().fluentPut("type", contentBlock.getType());
            if (contentBlock instanceof TextBlock) {
                object.put("text", ((TextBlock) contentBlock).getText());
            } else if (contentBlock instanceof ThinkingBlock) {
                ThinkingBlock thinkingBlock = (ThinkingBlock) contentBlock;
                object.fluentPut("thinking", thinkingBlock.getThinking())
                        .fluentPut("signature", thinkingBlock.getSignature());
            } else if (contentBlock instanceof ToolResultBlock) {
                ToolResultBlock toolResultBlock = (ToolResultBlock) contentBlock;
                object.fluentPut("tool_use_id", toolResultBlock.getToolUseId())
                        .fluentPut("content", toolResultBlock.getContent());
            } else if (contentBlock instanceof ToolUseBlock) {
                ToolUseBlock toolUseBlock = (ToolUseBlock) contentBlock;
                object.fluentPut("id", toolUseBlock.getId())
                        .fluentPut("name", toolUseBlock.getName())
                        .fluentPut("input", toolUseBlock.getInput());
            } else if (contentBlock instanceof UnknownBlock) {
                object.fluentPut("raw", ((UnknownBlock) contentBlock).getRaw());
            }
            contentJson.add(object);
        }
        return contentJson;
    }

    private JSONArray toToolsJson(List<ToolDefinition> tools) {
        JSONArray toolsArray = new JSONArray();
        for (ToolDefinition tool : tools) {
            JSONObject jsonObject = new JSONObject()
                    .fluentPut("name", tool.getName())
                    .fluentPut("description", tool.getDescription())
                    .fluentPut("input_schema", tool.getInputSchema());
            toolsArray.add(jsonObject);
        }
        return toolsArray;
    }
}
