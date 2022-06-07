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
import lombok.extern.slf4j.Slf4j;
import myvertx.turtest.api.CaptchaApi;
import rebue.wheel.vertx.verticle.AbstractWebVerticle;

@Slf4j
public class WebVerticle extends AbstractWebVerticle {

    @Override
    protected void configRoute(final Router router) {
        log.info("创建Schema解析器");
        final SchemaParser schemaParser = SchemaParser.createDraft7SchemaParser(
                SchemaRouter.create(this.vertx, new SchemaRouterOptions()));

        // 生成并获取验证码图像
        router.get("/captcha/gen")
                .handler(RouteToEBServiceHandler.build(
                        this.vertx.eventBus(),
                        CaptchaApi.ADDR,
                        "gen"));
        // 校验验证码
        router.post("/captcha/verify")
                .handler(BodyHandler.create())
                .handler(ValidationHandlerBuilder.create(schemaParser)
                        .body(json(objectSchema())).build())
                .handler(RouteToEBServiceHandler.build(
                        this.vertx.eventBus(),
                        CaptchaApi.ADDR,
                        "verify"));

    }

}
