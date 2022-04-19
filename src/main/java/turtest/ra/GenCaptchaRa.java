package turtest.ra;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenCaptchaRa {
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
}
