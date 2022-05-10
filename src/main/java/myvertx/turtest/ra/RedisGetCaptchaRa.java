package myvertx.turtest.ra;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RedisGetCaptchaRa {
    private Map<String, Object> map;
}
