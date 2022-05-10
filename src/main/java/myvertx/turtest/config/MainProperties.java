package myvertx.turtest.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 反序列化时忽略没有的字段
public class MainProperties {
    /**
     * 创建Web服务的实例数量
     */
    private Integer webInstances = 1;
    /**
     * 创建Redis服务的实例数量
     */
    private Integer redisInstances = 1;
}
