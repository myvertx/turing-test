package myvertx.turtest.to;

import java.util.Map;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@DataObject
@AllArgsConstructor
public class CaptchaRedisSetTo {
    private String              captchaId;
    private Map<String, Object> map;

    public CaptchaRedisSetTo(final JsonObject jsonObject) {
        setCaptchaId(jsonObject.getString("captchaId"));
        setMap(jsonObject.getJsonObject("map").getMap());
    }

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }
}
