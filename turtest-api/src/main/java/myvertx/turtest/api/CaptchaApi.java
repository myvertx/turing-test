package myvertx.turtest.api;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import io.vertx.ext.web.api.service.WebApiServiceGen;
import myvertx.turtest.to.CaptchaVerifyTo;
import rebue.wheel.vertx.util.EventBusUtils;

@WebApiServiceGen   // 生成WebApi服务
@VertxGen           // 生成多语言客户端
public interface CaptchaApi {

    String ADDR = EventBusUtils.getAddr(CaptchaApi.class);

    /**
     * 生成验证码
     *
     * @param request 请求
     *
     * @return 响应
     */
    Future<ServiceResponse> gen(ServiceRequest request);

    /**
     * 校验验证码
     *
     * @param body 校验验证码的参数
     *             XXX 请求参数的变量名必须是body，否则会接收不到参数
     */
    Future<ServiceResponse> verify(CaptchaVerifyTo body, ServiceRequest request);

}
