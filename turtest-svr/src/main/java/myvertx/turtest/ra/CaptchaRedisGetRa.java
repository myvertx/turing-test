package myvertx.turtest.ra;

import java.util.Map;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
@DataObject
public class CaptchaRedisGetRa {
    private Map<String, Object> map;

    public CaptchaRedisGetRa(final JsonObject jsonObject) {
        setMap(jsonObject.getJsonObject("map").getMap());
    }

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }

}
