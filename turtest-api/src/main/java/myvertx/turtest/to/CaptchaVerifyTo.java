package myvertx.turtest.to;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * 校验验证码的参数
 *
 * @author zbz
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@DataObject
public class CaptchaVerifyTo {
    /**
     * 验证码ID
     */
    private String      captchaId;

    /**
     * 背景图片宽度.
     */
    private Integer     bgImageWidth;
    /**
     * 背景图片高度.
     */
    private Integer     bgImageHeight;
    /**
     * 滑块图片宽度.
     */
    private Integer     sliderImageWidth;
    /**
     * 滑块图片高度.
     */
    private Integer     sliderImageHeight;
    /**
     * 滑动开始时间.
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    private Date        startSlidingTime;
    /**
     * 滑动结束时间.
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    private Date        endSlidingTime;
    /**
     * 滑动的轨迹.
     */
    private List<Track> trackList;
    /** 扩展数据，用户传输加密数据等. */
    private Object      data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Track {
        /** x. */
        private Integer x;
        /** y. */
        private Integer y;
        /** 时间. */
        private Integer t;
        /** 类型. */
        private String  type;
    }

    @SneakyThrows
    public CaptchaVerifyTo(final JsonObject jsonObject) {
        log.debug("CaptchaVerifyTo JsonObject constructor: {}", jsonObject);
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        captchaId         = jsonObject.getString("captchaId");
        bgImageWidth      = jsonObject.getInteger("bgImageWidth");
        bgImageHeight     = jsonObject.getInteger("bgImageHeight");
        sliderImageWidth  = jsonObject.getInteger("sliderImageWidth");
        sliderImageHeight = jsonObject.getInteger("sliderImageHeight");
        startSlidingTime  = sdf.parse(jsonObject.getString("startSlidingTime"));
        endSlidingTime    = sdf.parse(jsonObject.getString("endSlidingTime"));
        trackList         = jsonObject.getJsonArray("trackList").stream().map(item -> {
                              JsonObject jo = (JsonObject) item;
                              return Track.builder()
                                      .x(jo.getInteger("x"))
                                      .y(jo.getInteger("y"))
                                      .t(jo.getInteger("t"))
                                      .type(jo.getString("type"))
                                      .build();
                          })
                .collect(Collectors.toList());
    }

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }

}
