package myvertx.turtest.ra;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class RedisGetCaptchaRa {
    private Map<String, Object> map;
}
