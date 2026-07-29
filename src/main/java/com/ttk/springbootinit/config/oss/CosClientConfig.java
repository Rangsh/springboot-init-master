package com.ttk.springbootinit.config.oss;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 客户端（仅 cos.enabled=true 时装配）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cos.client")
@ConditionalOnProperty(prefix = "cos", name = "enabled", havingValue = "true")
public class CosClientConfig {

    private String accessKey;

    private String secretKey;

    private String region;

    private String bucket;

    @Bean
    public COSClient cosClient() {
        COSCredentials cred = new BasicCOSCredentials(accessKey, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        return new COSClient(cred, clientConfig);
    }
}
