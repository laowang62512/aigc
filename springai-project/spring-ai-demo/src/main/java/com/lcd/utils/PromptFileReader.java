package com.lcd.utils;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PromptFileReader {

    private final ResourceLoader resourceLoader;

    // Spring自动注入资源加载器
    public PromptFileReader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 读取resources下txt提示词文件
     * @param filePath 文件相对路径，例如 prompt/main-system.txt
     * @return 文件完整文本
     */
    public String readPromptFile(String filePath) {
        // classpath: 固定前缀代表读取resources目录
        Resource resource = resourceLoader.getResource("classpath:" + filePath);
        if (!resource.exists()) {
            throw new RuntimeException("提示词文件不存在：" + filePath);
        }
        try {
            // 读取文件全部内容，UTF-8编码防止中文乱码
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取提示词文件失败：" + filePath, e);
        }
    }
}