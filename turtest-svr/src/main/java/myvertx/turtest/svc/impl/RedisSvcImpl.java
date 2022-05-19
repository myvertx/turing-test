package myvertx.turtest.svc.impl;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import lombok.extern.slf4j.Slf4j;
import myvertx.turtest.ra.CaptchaRedisGetRa;
import myvertx.turtest.svc.RedisSvc;
import myvertx.turtest.to.CaptchaRedisSetTo;

@Slf4j
public class RedisSvcImpl implements RedisSvc {

    /**
     * Captcha的Key的前缀
     * 后面跟captchaId拼接成Key
     * Value为自动生成的State的值
     */
    private static final String REDIS_KEY_CAPTCHA_PREFIX = "myvertx.turtest.captcha::";

    private final RedisAPI      redis;

    private final Long          captchaTimeout;

    public RedisSvcImpl(final RedisAPI redis, final Long captchaTimeout) {
        this.redis          = redis;
        this.captchaTimeout = captchaTimeout;
    }

    @Override
    public Future<CaptchaRedisGetRa> getCaptcha(final String captchaId) {
        log.debug("redis.getCaptcha params: captchaId-{}", captchaId);
        return this.redis.getdel(REDIS_KEY_CAPTCHA_PREFIX + captchaId)
                .compose(res -> {
                    log.debug("redis.getCaptcha result: {}", res);

                    if (res == null) {
                        log.debug("找不到验证码缓存的信息，可能已失效");
                        return Future.succeededFuture();
                    }

                    @SuppressWarnings("unchecked")
                    final Map<String, Object> map = Json.decodeValue(res.toBuffer(), Map.class);
                    return Future.succeededFuture(new CaptchaRedisGetRa(map));
                }).recover(err -> {
                    final String msg = "获取缓存中的验证码失败";
                    log.error(msg, err);
                    return Future.failedFuture(err);
                });
    }

    @Override
    public Future<Response> setCaptcha(final CaptchaRedisSetTo to) {
        log.debug("redis.setCaptcha: to-{}", to);
        return this.redis.setex(REDIS_KEY_CAPTCHA_PREFIX + to.getCaptchaId(),
                this.captchaTimeout.toString(),
                Json.encode(to.getMap()));
    }

}
