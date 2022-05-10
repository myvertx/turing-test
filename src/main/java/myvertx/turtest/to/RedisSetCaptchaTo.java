package myvertx.turtest.to;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class RedisSetCaptchaTo {
    private String captchaId;
    private Map<String, Object> map;
}
