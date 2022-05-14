package myvertx.turtest.to;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@DataObject
@AllArgsConstructor
public class CaptchaRedisGetTo {
    private String captchaId;

    public CaptchaRedisGetTo(final JsonObject jsonObject) {
        setCaptchaId(jsonObject.getString("captchaId"));
    }

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }

}
