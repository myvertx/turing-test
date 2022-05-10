package myvertx.turtest.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 反序列化时忽略没有的字段
public class WebProperties {
    /**
     * Web服务器监听的端口号
     */
    private Integer port = 0;
}
