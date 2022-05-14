package myvertx.turtest.svc;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import myvertx.turtest.ra.CaptchaGenRa;
import myvertx.turtest.to.CaptchaVerifyTo;

@ProxyGen   // 生成服务代理
@VertxGen   // 生成多语言客户端
public interface CaptchaSvc {
    String ADDR = "myvertx.turtest.svc.captcha-svc";

    /**
     * 生成验证码
     */
    Future<CaptchaGenRa> gen();

    /**
     * 校验验证码
     *
     * @param to 校验验证码的参数
     */
    Future<Boolean> verify(CaptchaVerifyTo to);

}
