package com.getian;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 *@Author: sonicge
 *@CreateTime: 2026-07-05
 */

@Data
@AllArgsConstructor
public class TestDsClient {
    private String apiKey;
    private String baseUrl;
    private String model;

    public static void main(String[] args) {

        String baseUrl = "https://api.deepseek.com/anthropic";
        String model = "deepseek-v4-flash";
        String apiKey = "sk-64b3400ffa9944b7b15755e85c6d0b29"; //已经废弃

        JSONObject userPrompt = new JSONObject();
        userPrompt.put("role","user");
        userPrompt.put("content","请帮我搜索一下MacBook Air M5 512G 15英寸 最新的价格");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model",model);
        jsonObject.put("messages",List.of(userPrompt));
        String jsonString = jsonObject.toJSONString();

        System.out.println(jsonString);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/messages"))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonString, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            System.out.println(body);
//            JSONObject resp = JSONUtil.toBean(body, JSONObject.class);
//            System.out.println(resp.getString("role"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
