package myvertx.turtest.config;

import lombok.Data;

@Data
public class MainProperties {

    private Boolean isMock         = false;

    /**
     * 验证码缓存超时时间(超过此时间会自动删除)
     * 单位：秒
     * 默认：3分钟
     */
    private Long    captchaTimeout = 180L;
}
