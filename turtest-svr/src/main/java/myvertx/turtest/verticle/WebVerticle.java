package myvertx.turtest.verticle;

import static io.vertx.ext.web.validation.builder.Bodies.json;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;

import javax.inject.Inject;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.service.RouteToEBServiceHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;
import io.vertx.json.schema.SchemaParser;
import myvertx.turtest.api.CaptchaApi;
import rebue.wheel.vertx.verticle.AbstractWebVerticle;

@SuppressWarnings("deprecation")
public class WebVerticle extends AbstractWebVerticle {

    @Inject
    private SchemaParser schemaParser;

    @Override
    protected void configRouter(final Router router) {
        // 生成并获取验证码图像
        router.get("/captcha/gen")
                .handler(RouteToEBServiceHandler.build(
                        this.vertx.eventBus(),
                        CaptchaApi.ADDR,
                        "gen"));
        // 校验验证码
        router.post("/captcha/verify")
                .handler(BodyHandler.create())
                .handler(ValidationHandlerBuilder.create(this.schemaParser)
                        .body(json(objectSchema())).build())
                .handler(RouteToEBServiceHandler.build(
                        this.vertx.eventBus(),
                        CaptchaApi.ADDR,
                        "verify"));

    }

}
