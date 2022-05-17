package myvertx.turtest.verticle;

import static io.vertx.ext.web.validation.builder.Bodies.json;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.service.RouteToEBServiceHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import lombok.extern.slf4j.Slf4j;
import myvertx.turtest.api.CaptchaApi;
import myvertx.turtest.config.WebProperties;

@Slf4j
public class WebVerticle extends AbstractVerticle {
    public static final String EVENT_BUS_WEB_START = "myvertx.turtest.verticle.web.start";

    private WebProperties      webProperties;
    private HttpServer         httpServer;

    @Override
    public void start() {
        this.webProperties = config().mapTo(WebProperties.class);

        log.info("创建路由");
        final Router router      = Router.router(this.vertx);
        // 全局route
        final Route  globalRoute = router.route();
        // CORS
        if (this.webProperties.getIsCors()) {
            log.info("启用CORS");
            globalRoute.handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET));
        }

        log.info("创建Schema解析器");
        final SchemaParser schemaParser = SchemaParser.createDraft7SchemaParser(
                SchemaRouter.create(this.vertx, new SchemaRouterOptions()));

        log.info("配置路由");
        // 生成并获取验证码图像
        router.get("/captcha/gen")
                .handler(RouteToEBServiceHandler.build(
                        this.vertx.eventBus(),
                        CaptchaApi.ADDR,
                        "gen"));
        // 校验验证码
        router.post("/captcha/verify")
                .handler(LoggerHandler.create())
                .handler(BodyHandler.create())
                .handler(ValidationHandlerBuilder.create(schemaParser)
                        .body(json(objectSchema())).build())
                .handler(RouteToEBServiceHandler.build(
                        this.vertx.eventBus(),
                        CaptchaApi.ADDR,
                        "verify"));

        this.httpServer = this.vertx.createHttpServer().requestHandler(router);

        this.vertx.eventBus()
                .consumer(EVENT_BUS_WEB_START, this::handleStart)
                .completionHandler(this::handleStartCompletion);

        log.info("WebVerticle Started");
    }

    private void handleStart(final Message<Void> message) {
        this.httpServer.listen(this.webProperties.getPort(), res -> {
            if (res.succeeded()) {
                log.info("HTTP server started on port " + res.result().actualPort());
            } else {
                log.error("HTTP server start fail", res.cause());
            }
        });
    }

    private void handleStartCompletion(final AsyncResult<Void> res) {
        if (res.succeeded()) {
            log.info("Event Bus register success: web.start");
        } else {
            log.error("Event Bus register fail: web.start", res.cause());
        }
    }

}
