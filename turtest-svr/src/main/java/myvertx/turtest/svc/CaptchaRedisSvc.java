package myvertx.turtest.svc;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.redis.client.Response;
import myvertx.turtest.ra.CaptchaRedisGetRa;
import myvertx.turtest.to.CaptchaRedisSetTo;

@ProxyGen   // 生成服务代理
@VertxGen   // 生成多语言客户端
public interface CaptchaRedisSvc {
    String ADDR = "myvertx.turtest.svc.redis-svc";

    /**
     * 获取验证码
     */
    Future<CaptchaRedisGetRa> getCaptcha(String captchaId);

    /**
     * 设置验证码
     */
    Future<Response> setCaptcha(CaptchaRedisSetTo to);

}
