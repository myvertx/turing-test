package myvertx.turtest.verticle;

import static io.vertx.ext.web.validation.builder.Bodies.json;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.service.RouteToEBServiceHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import myvertx.turtest.api.CaptchaApi;
import rebue.wheel.vertx.verticle.AbstractWebVerticle;

@SuppressWarnings("deprecation")
public class WebVerticle extends AbstractWebVerticle {

    @Override
    protected void configRouter(final Router router) {
        final SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(
                SchemaRouter.create(this.vertx, new SchemaRouterOptions()));

        // 生成并获取验证码图像
        router.get("/turtest/captcha/gen")
                .handler(RouteToEBServiceHandler.build(
                        this.vertx.eventBus(),
                        CaptchaApi.ADDR,
                        "gen"));
        // 校验验证码
        router.post("/turtest/captcha/verify")
                .handler(BodyHandler.create())
                .handler(ValidationHandlerBuilder.create(schemaParser)
                        .body(json(objectSchema())).build())
                .handler(RouteToEBServiceHandler.build(
                        this.vertx.eventBus(),
                        CaptchaApi.ADDR,
                        "verify"));

    }

}
