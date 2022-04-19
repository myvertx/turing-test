package turtest.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 反序列化时忽略没有的字段
public class MainProperties {
    private Integer webInstances = 1;
    private Integer redisInstances = 1;
}
