package myvertx.turtest.ra;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
@DataObject
public class CaptchaGenRa {
    /**
     * 验证码ID
     */
    private String id;

    /**
     * 背景图像
     */
    private String backgroundImage;

    /**
     * 滑块图像
     */
    private String sliderImage;

    public CaptchaGenRa(final JsonObject jsonObject) {
        setId(jsonObject.getString("id"));
        setBackgroundImage(jsonObject.getString("backgroundImage"));
        setSliderImage(jsonObject.getString("sliderImage"));
    }

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }
}
