package myvertx.turtest.svc;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import myvertx.turtest.to.CaptchaVerifyTo;
import rebue.wheel.vertx.ro.Vro;
import rebue.wheel.vertx.util.EventBusUtils;

@ProxyGen // 生成服务代理
@VertxGen // 生成多语言客户端
public interface CaptchaSvc {
    String ADDR = EventBusUtils.getAddr(CaptchaSvc.class);

    /**
     * 生成验证码
     */
    Future<Vro> gen();

    /**
     * 校验验证码
     *
     * @param to 校验验证码的参数
     */
    Future<Vro> verify(CaptchaVerifyTo to);

}
