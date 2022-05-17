package myvertx.turtest.config;

import lombok.Data;

@Data
public class WebProperties {
    /**
     * Web服务器监听的端口号
     */
    private Integer port   = 0;

    /**
     * 是否需要CORS
     */
    private Boolean isCors = false;

}
