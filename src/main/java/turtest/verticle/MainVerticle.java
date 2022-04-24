package turtest.verticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import turtest.cdc.RedisGetCaptchaToCdc;
import turtest.cdc.RedisSetCaptchaToCdc;
import turtest.cdc.RoCdc;
import turtest.config.MainConfig;
import turtest.config.MainProperties;
import turtest.config.RedisConfig;
import turtest.config.WebConfig;
import turtest.ro.Ro;
import turtest.to.RedisGetCaptchaTo;
import turtest.to.RedisSetCaptchaTo;

@Slf4j
public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        ConfigRetriever retriever = ConfigRetriever.create(vertx);
        retriever.getConfig(res -> {
            log.info("config result: {}", res.result());

            if (res.failed()) {
                log.warn("Get config failed", res.cause());
                startPromise.fail(res.cause());
            }

            JsonObject config = res.result();
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
            MainProperties mainConfig = config.getJsonObject(MainConfig.PREFIX).mapTo(MainProperties.class);

            // 部署verticle
            Future<String> redisVerticleFuture = vertx.deployVerticle(
                    RedisVerticle.class,
                    new DeploymentOptions()
                            .setInstances(mainConfig.getRedisInstances())
                            .setWorkerPoolName("vertx-redis-thread")
                            .setConfig(config.getJsonObject(RedisConfig.PREFIX)));
            Future<String> webVerticleFuture = vertx.deployVerticle(
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
