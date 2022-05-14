package myvertx.turtest.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 校验验证码的参数
 *
 * @author zbz
 */
@Data
@DataObject
@AllArgsConstructor
@Slf4j
public class CaptchaVerifyTo {
    /**
     * 验证码ID
     */
    private String captchaId;

    /**
     * 背景图片宽度.
     */
    private Integer bgImageWidth;
    /**
     * 背景图片高度.
     */
    private Integer bgImageHeight;
    /**
     * 滑块图片宽度.
     */
    private Integer sliderImageWidth;
    /**
     * 滑块图片高度.
     */
    private Integer sliderImageHeight;
    /**
     * 滑动开始时间.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "GMT+8")
    private Date startSlidingTime;
    /**
     * 滑动结束时间.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "GMT+8")
    private Date entSlidingTime;
    /**
     * 滑动的轨迹.
     */
    private List<Track> trackList;

    @SneakyThrows
    public CaptchaVerifyTo(final JsonObject jsonObject) {
        log.debug("CaptchaVerifyTo JsonObject constructor: {}", jsonObject);
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        captchaId = jsonObject.getString("captchaId");
        bgImageWidth = jsonObject.getInteger("bgImageWidth");
        bgImageHeight = jsonObject.getInteger("bgImageHeight");
        sliderImageWidth = jsonObject.getInteger("sliderImageWidth");
        sliderImageHeight = jsonObject.getInteger("sliderImageHeight");
        startSlidingTime = sdf.parse(jsonObject.getString("startSlidingTime"));
        entSlidingTime = sdf.parse(jsonObject.getString("entSlidingTime"));
        trackList = jsonObject.getJsonArray("trackList").stream().map(item -> {
            JsonObject jo = (JsonObject) item;
            return Track.builder()
                    .x(jo.getInteger("x"))
                    .y(jo.getInteger("y"))
                    .t(jo.getInteger("t"))
                    .build();
        }).collect(Collectors.toList());
    }

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Track {
        private Integer x;
        private Integer y;
        private Integer t;
    }

}
