package com.getian.mcp;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

/**
 *@Author: sonicge
 *@CreateTime: 2026-08-01
 */

public class MockMcpServers {
    private static final Map<String, Supplier<McpClient>> FACTORIES = new LinkedHashMap<>();

    static {
        FACTORIES.put("time", MockMcpServers::timeServer);
        FACTORIES.put("weather", MockMcpServers::weatherServer);
    }

    private MockMcpServers() {
    }

    public static List<String> availableNames() {
        return new ArrayList<>(FACTORIES.keySet());
    }

    public static McpClient create(String name) {
        Supplier<McpClient> mcpClientSupplier = FACTORIES.get(name);
        return mcpClientSupplier == null ? null : mcpClientSupplier.get();
    }

    private static McpClient timeServer() {
        McpClient client = new McpClient("time");
        List<McpToolDefinition> definitions = new ArrayList<>();
        Map<String, McpClient.Handler> handlerMap = new LinkedHashMap<>();

        //get_current_time
        //McpToolDefinition
        String mcpToolName = "get_current_time";
        String description = "Get current time for a timezone. (readOnly)";

        JSONObject properties = new JSONObject()
                .fluentPut("timezone", new JSONObject()
                        .fluentPut("type", "string")
                        .fluentPut("description", "IANA timezone, for example Asia/Shanghai"));
        JSONObject inputSchema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray());
        McpToolDefinition mcpToolDefinition = new McpToolDefinition(mcpToolName, description, inputSchema, true);
        definitions.add(mcpToolDefinition);

        //handler
        handlerMap.put(mcpToolName, new McpClient.Handler() {
            @Override
            public String handler(JSONObject input) {
                String timeZone = input == null ? "" : input.getString("timezone");
                try {
                    ZoneId zone = (timeZone == null || timeZone.isBlank()) ? ZoneId.systemDefault() : ZoneId.of(timeZone);
                    String now = ZonedDateTime.now(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    return "[time]" + now + "(" + zone + ")";
                } catch (DateTimeException e) {
                    return "Error: invalid timezone '" + timeZone + "'";
                }
            }
        });

        client.register(definitions, handlerMap);
        return client;
    }

    private static McpClient weatherServer() {
        McpClient client = new McpClient("weather");
        String toolName = "get_current_weather";
        String description = "Get mock current weather for a city. (readOnly)";
        JSONObject properties = new JSONObject().
                fluentPut("city", new JSONObject()
                        .fluentPut("type", "string")
                        .fluentPut("description", "City name, for example Shanghai"));
        JSONObject inputSchema = new JSONObject()
                .fluentPut("type", "object")
                .fluentPut("properties", properties)
                .fluentPut("required", new JSONArray()
                        .fluentAdd("city"));
        McpToolDefinition toolDefinition = new McpToolDefinition(toolName, description, inputSchema, true);
        List<McpToolDefinition> toolDefinitionList = new ArrayList<>();
        toolDefinitionList.add(toolDefinition);

        Map<String, String> mockData = new LinkedHashMap<>();
        mockData.put("beijing", "Beijing: 27C, sunny, north wind");
        mockData.put("shanghai", "Shanghai: 26C, cloudy, light breeze");
        mockData.put("hangzhou", "Hangzhou: 25C, light rain, humid");
        mockData.put("san francisco", "San Francisco: 18C, foggy, west wind");


        Map<String, McpClient.Handler> handlerMap = new LinkedHashMap<>();
        handlerMap.put(toolName, input -> {
            String city = input == null ? "" : input.getString("city");
            if (city == null || city.isBlank()) {
                return "Mcp Error : city is required";
            }
            String key = city.trim().toLowerCase();
            if (!mockData.containsKey(key)) {
                return "Mcp Error: city is invalid";
            }
            return "[weather] " + mockData.getOrDefault(key,
                    city.trim() + ": 22C, partly cloudy, mock weather");
        });

        client.register(toolDefinitionList, handlerMap);
        return client;
    }
}
