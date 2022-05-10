package myvertx.turtest.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisConnection;
import io.vertx.redis.client.RedisOptions;
import lombok.extern.slf4j.Slf4j;
import myvertx.turtest.config.RedisProperties;
import myvertx.turtest.ra.RedisGetCaptchaRa;
import myvertx.turtest.ro.Ro;
import myvertx.turtest.to.RedisGetCaptchaTo;
import myvertx.turtest.to.RedisSetCaptchaTo;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class RedisVerticle extends AbstractVerticle {

    public static final String EVENT_BUS_REDIS_GET_CAPTCHA = "turtest.captcha.redis.get-captcha";
    public static final String EVENT_BUS_REDIS_SET_CAPTCHA = "turtest.captcha.redis.set-captcha";
    /**
     * Captcha的Key的前缀
     * 后面跟captchaId拼接成Key
     * Value为自动生成的State的值
     */
    private static final String REDIS_KEY_CAPTCHA_PREFIX = "turtest.captcha::";
    private static final int MAX_RECONNECT_RETRIES = 16;

    private final AtomicBoolean CONNECTING = new AtomicBoolean();

    private RedisProperties redisProperties;
    private RedisOptions redisOptions;
    private RedisConnection redisConn;
    private RedisAPI redis;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        redisProperties = config().mapTo(RedisProperties.class);
        redisOptions = new RedisOptions(config());

        createRedisClient().onSuccess(conn -> {
            log.info("connect Redis success!");
            redis = RedisAPI.api(conn);
            vertx.<RedisSetCaptchaTo>eventBus()
                    .consumer(EVENT_BUS_REDIS_SET_CAPTCHA, this::handleSetCaptcha)
                    .completionHandler(this::handleSetCaptchaCompletion);
            vertx.<RedisGetCaptchaTo>eventBus()
                    .consumer(EVENT_BUS_REDIS_GET_CAPTCHA, this::handleGetCaptcha)
                    .completionHandler(this::handleGetCaptchaCompletion);
        });
    }

    private void handleSetCaptcha(Message<RedisSetCaptchaTo> message) {
        log.debug("handleSetCaptcha");
        RedisSetCaptchaTo to = message.body();
        redis.setex(REDIS_KEY_CAPTCHA_PREFIX + to.getCaptchaId(),
                        String.valueOf(redisProperties.getCaptchaTimeout()),
                        Json.encode(to.getMap()))
                .onSuccess(handle -> {
                    message.reply(Ro.newSuccess("Redis设置Captcha成功"));
                })
                .onFailure(handle -> {
                    String msg = "Redis设置Captcha失败";
                    log.error(msg, handle.getCause());
                    message.reply(Ro.newFail(msg, handle.getCause().toString()));
                });

    }

    private void handleGetCaptcha(Message<RedisGetCaptchaTo> message) {
        log.debug("handleGetCaptcha");
        RedisGetCaptchaTo to = message.body();
        redis.getdel(REDIS_KEY_CAPTCHA_PREFIX + to.getCaptchaId())
                .onSuccess(value -> {
                    if (value == null) {
                        message.reply(Ro.newWarn("查不到此ID的验证码"));
                        return;
                    }
                    Map<String, Object> map = Json.decodeValue(value.toBuffer(), Map.class);
                    message.reply(Ro.newSuccess(new RedisGetCaptchaRa(map)));
                })
                .onFailure(handle -> {
                    String msg = "Redis获取Captcha异常";
                    log.error(msg, handle);
                    message.reply(Ro.newFail(msg, handle.toString()));
                });
    }

    private void handleSetCaptchaCompletion(AsyncResult<Void> res) {
        if (res.succeeded()) {
            log.info("Event Bus register success: RedisSetCaptcha");
        } else {
            log.error("Event Bus register fail: RedisSetCaptcha", res.cause());
        }
    }

    private void handleGetCaptchaCompletion(AsyncResult<Void> res) {
        if (res.succeeded()) {
            log.info("Event Bus register success: RedisGetCaptcha");
        } else {
            log.error("Event Bus register fail: RedisGetCaptcha", res.cause());
        }
    }


    /**
     * Will create a redis client and setup a reconnect handler when there is
     * an exception in the connection.
     */
    private Future<RedisConnection> createRedisClient() {
        Promise<RedisConnection> promise = Promise.promise();

        if (CONNECTING.compareAndSet(false, true)) {
            Redis.createClient(vertx, redisOptions)
                    .connect()
                    .onSuccess(conn -> {

                        // make sure to invalidate old connection if present
                        if (redisConn != null) {
                            redisConn.close();
                        }

                        redisConn = conn;

                        // make sure the client is reconnected on error
                        conn.exceptionHandler(e -> {
                            // attempt to reconnect,
                            // if there is an unrecoverable error
                            attemptReconnect(0);
                        });

                        // allow further processing
                        promise.complete(conn);
                        CONNECTING.set(false);
                    }).onFailure(t -> {
                        promise.fail(t);
                        CONNECTING.set(false);
                    });
        } else {
            promise.complete();
        }

        return promise.future();
    }

    /**
     * Attempt to reconnect up to MAX_RECONNECT_RETRIES
     */
    private void attemptReconnect(int retry) {
        if (retry > MAX_RECONNECT_RETRIES) {
            // we should stop now, as there's nothing we can do.
            CONNECTING.set(false);
        } else {
            // retry with backoff up to 10240 ms
            long backoff = (long) (Math.pow(2, Math.min(retry, 10)) * 10);

            vertx.setTimer(backoff, timer -> {
                createRedisClient()
                        .onFailure(t -> attemptReconnect(retry + 1));
            });
        }
    }
}
