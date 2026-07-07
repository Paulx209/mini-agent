package com.getian.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ConfigUtils {
    private ConfigUtils() {
    }

    /**
     * 从 classpath 路径中加载资源
     * @param resourceName
     * @return
     */
    public static Properties loadPropertiesFromResource(String resourceName) {
        Properties properties = new Properties();
        ClassLoader classLoader = ConfigUtils.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new RuntimeException("配置资源文件不存在: " + resourceName);
            }
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            return properties;
        } catch (IOException e) {
            throw new RuntimeException("加载配置资源文件失败: " + resourceName, e);
        }
    }
}
