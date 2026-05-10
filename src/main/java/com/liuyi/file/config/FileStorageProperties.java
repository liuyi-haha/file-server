package com.liuyi.file.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;

@ConfigurationProperties(prefix = "file.storage")
@Component
@Data
@Slf4j
public class FileStorageProperties {
    private String path;

    @PostConstruct
    public void init() {
        System.out.println("=== FileStorageProperties.init() 被调用 ===");
        System.out.println("配置的路径: " + path);
        File dir = new File(path);
        if (!dir.exists()) {
            System.out.println("目录不存在，尝试创建...");
            dir.mkdirs();
            System.out.println("创建后目录是否存在: " + dir.exists());
        }
    }
}
