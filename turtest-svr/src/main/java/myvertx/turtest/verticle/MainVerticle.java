package myvertx.turtest.verticle;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.redis.client.RedisAPI;
import io.vertx.serviceproxy.ServiceBinder;
import lombok.extern.slf4j.Slf4j;
import myvertx.turtest.api.CaptchaApi;
import myvertx.turtest.api.impl.CaptchaApiImpl;
import myvertx.turtest.config.MainProperties;
import myvertx.turtest.svc.CaptchaRedisSvc;
import myvertx.turtest.svc.CaptchaSvc;
import myvertx.turtest.svc.impl.CaptchaRedisSvcImpl;
import myvertx.turtest.svc.impl.CaptchaSvcImpl;
import rebue.wheel.vertx.util.RedisUtils;

@SuppressWarnings("deprecation")
@Slf4j
public class MainVerticle extends AbstractVerticle {
    static {
        // 初始化jackson的功能
        DatabindCodec.mapper()
                .disable(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES   // 忽略没有的字段
                )
                .enable(
                        MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES    // 忽略字段和属性的大小写
                )
                .setSerializationInclusion(Include.NON_NULL)
                .registerModule(new JavaTimeModule());  // 支持Java8的LocalDate/LocalDateTime类型
    }

    @Override
    public void start(final Promise<Void> startPromise) {
        final ConfigRetriever retriever = ConfigRetriever.create(this.vertx);
        retriever.getConfig(configRes -> {
            log.info("config result: {}", configRes.result());

            if (configRes.failed()) {
                log.warn("Get config failed", configRes.cause());
                startPromise.fail(configRes.cause());
            }

            final JsonObject config = configRes.result();
            if (config == null || config.isEmpty()) {
                startPromise.fail("Get config is empty");
                return;
            }

            // 读取main的配置
            final MainProperties mainProperties = config.getJsonObject("main").mapTo(MainProperties.class);

            log.info("创建服务实例");
            final RedisAPI        redisClient     = RedisUtils.createRedisClient(this.vertx, config.getJsonObject("redis"));
            final CaptchaRedisSvc captchaRedisSvc = new CaptchaRedisSvcImpl(redisClient, mainProperties.getCaptchaTimeout());
            final CaptchaSvc      captchaSvc      = new CaptchaSvcImpl(captchaRedisSvc);
            final CaptchaApi      captchaApi      = new CaptchaApiImpl(captchaSvc);

            log.info("注册服务");
            new ServiceBinder(this.vertx)
                    .setAddress(CaptchaSvc.ADDR)
                    .register(CaptchaSvc.class, captchaSvc);
            new ServiceBinder(this.vertx)
                    .setAddress(CaptchaApi.ADDR)
                    .register(CaptchaApi.class, captchaApi);

            log.info("部署verticle");
            final Future<String> webVerticleFuture = deployVerticle("web", WebVerticle.class, config);

            // 部署成功或失败事件
            webVerticleFuture
                    .onSuccess(handle -> {
                        log.info("部署Verticle完成");
                        this.vertx.eventBus().publish(WebVerticle.EVENT_BUS_WEB_START, null);
                        log.info("启动完成.");
                        startPromise.complete();
                    })
                    .onFailure(err -> {
                        log.error("启动失败.", err);
                        startPromise.fail(err);
                        this.vertx.close();
                    });
        });
    }

    /**
     * 部署Verticle
     *
     * @param verticleName  Verticle的名称
     * @param verticleClass Verticle类
     * @param config        当前的配置对象
     *
     * @return Future
     */
    private Future<String> deployVerticle(final String verticleName, final Class<? extends Verticle> verticleClass, final JsonObject config) {
        return this.vertx.deployVerticle(verticleClass,
                new DeploymentOptions(config.getJsonObject(verticleName)));
    }

}
