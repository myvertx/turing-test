package myvertx.turtest.to;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RedisSetCaptchaTo {
    private String captchaId;
    private Map<String, Object> map;
}
