package myvertx.turtest.svc;

import io.vertx.core.Future;
import io.vertx.redis.client.Response;
import myvertx.turtest.ra.CaptchaRedisGetRa;
import myvertx.turtest.to.CaptchaRedisSetTo;

public interface CaptchaRedisSvc {

    /**
     * 获取验证码
     */
    Future<CaptchaRedisGetRa> getCaptcha(String captchaId);

    /**
     * 设置验证码
     */
    Future<Response> setCaptcha(CaptchaRedisSetTo to);

}
