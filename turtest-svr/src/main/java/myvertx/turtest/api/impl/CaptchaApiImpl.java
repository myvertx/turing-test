package myvertx.turtest.api.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import lombok.extern.slf4j.Slf4j;
import myvertx.turtest.api.CaptchaApi;
import myvertx.turtest.svc.CaptchaSvc;
import myvertx.turtest.svc.CaptchaSvcVertxEBProxy;
import myvertx.turtest.to.CaptchaVerifyTo;
import rebue.wheel.api.ro.Ro;

@Slf4j
public class CaptchaApiImpl implements CaptchaApi {

    private final CaptchaSvc captchaSvc;

    public CaptchaApiImpl(final Vertx vertx) {
        this.captchaSvc = new CaptchaSvcVertxEBProxy(vertx, CaptchaSvc.ADDR);
    }

    /**
     * 生成验证码
     */
    @Override
    public Future<ServiceResponse> gen(final ServiceRequest request) {
        return this.captchaSvc.gen()
                .compose(ra -> Future.succeededFuture(ServiceResponse.completedWithJson(JsonObject.mapFrom(
                        Ro.success("生成验证码成功", ra)))))
                .recover(err -> {
                    final String msg = "生成验证码失败";
                    log.error(msg, err);
                    return Future.succeededFuture(ServiceResponse.completedWithJson(JsonObject.mapFrom(
                            Ro.fail(msg, err.getMessage()))));
                });
    }

    /**
     * 校验验证码
     *
     * @param body 校验验证码的参数
     *             XXX 请求参数的变量名必须是body，否则会接收不到参数
     */
    @Override
    public Future<ServiceResponse> verify(final CaptchaVerifyTo body, final ServiceRequest request) {
        return this.captchaSvc.verify(body)
                .compose(ra -> Future.succeededFuture(ServiceResponse.completedWithJson(JsonObject.mapFrom(
                        ra ? Ro.success("校验验证码成功") : Ro.warn("校验验证码失败")))))
                .recover(err -> {
                    final String msg = "校验验证码失败";
                    log.error(msg, err);
                    return Future.succeededFuture(ServiceResponse.completedWithJson(JsonObject.mapFrom(
                            Ro.fail(msg, err.getMessage()))));
                });
    }

}
