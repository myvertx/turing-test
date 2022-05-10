package myvertx.turtest.verticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import myvertx.turtest.cdc.RedisGetCaptchaToCdc;
import myvertx.turtest.cdc.RedisSetCaptchaToCdc;
import myvertx.turtest.cdc.RoCdc;
import myvertx.turtest.config.MainConfig;
import myvertx.turtest.config.MainProperties;
import myvertx.turtest.config.RedisConfig;
import myvertx.turtest.config.WebConfig;
import myvertx.turtest.to.RedisGetCaptchaTo;
import myvertx.turtest.to.RedisSetCaptchaTo;
import rebue.wheel.api.ro.Ro;

@Slf4j
public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        final ConfigRetriever retriever = ConfigRetriever.create(vertx);
        retriever.getConfig(res -> {
            log.info("config result: {}", res.result());

            if (res.failed()) {
                log.warn("Get config failed", res.cause());
                startPromise.fail(res.cause());
            }

            final JsonObject config = res.result();
            if (config == null || config.isEmpty()) {
                startPromise.fail("Get config is empty");
                return;
            }

            // 注册服务调用参数及返回值的解码器
            vertx.eventBus()
                    .registerDefaultCodec(Ro.class, new RoCdc())
                    .registerDefaultCodec(RedisSetCaptchaTo.class, new RedisSetCaptchaToCdc())
                    .registerDefaultCodec(RedisGetCaptchaTo.class, new RedisGetCaptchaToCdc());

            // 读取main的配置
            final MainProperties mainConfig = config.getJsonObject(MainConfig.PREFIX).mapTo(MainProperties.class);

            // 部署verticle
            final Future<String> redisVerticleFuture = vertx.deployVerticle(
                    RedisVerticle.class,
                    new DeploymentOptions()
                            .setInstances(mainConfig.getRedisInstances())
                            .setWorkerPoolName("vertx-redis-thread")
                            .setConfig(config.getJsonObject(RedisConfig.PREFIX)));
            final Future<String> webVerticleFuture   = vertx.deployVerticle(
                    WebVerticle.class,
                    new DeploymentOptions()
                            .setInstances(mainConfig.getWebInstances())
                            .setWorkerPoolName("vertx-web-thread")
                            .setConfig(config.getJsonObject(WebConfig.PREFIX)));

            CompositeFuture.all(redisVerticleFuture, webVerticleFuture)
                    .onFailure(t -> {
                        startPromise.fail(t);
                        vertx.close();
                    })
                    .onSuccess(f -> {
                        startPromise.complete();
                    });
        });

    }

}
