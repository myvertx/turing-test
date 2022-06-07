package myvertx.turtest.verticle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.inject.Module;

import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import lombok.extern.slf4j.Slf4j;
import myvertx.turtest.api.CaptchaApi;
import myvertx.turtest.inject.MainModule;
import myvertx.turtest.svc.CaptchaSvc;
import rebue.wheel.vertx.guice.RedisGuiceModule;
import rebue.wheel.vertx.verticle.AbstractMainVerticle;

@Slf4j
public class MainVerticle extends AbstractMainVerticle {

    @Inject
    private CaptchaSvc captchaSvc;
    @Inject
    private CaptchaApi captchaApi;

    @Override
    protected void addGuiceModules(final List<Module> guiceModules) {
        guiceModules.add(new RedisGuiceModule());
        guiceModules.add(new MainModule());
    }

    @Override
    protected void beforeDeploy(final JsonObject config) {
        log.info("注册服务");
        new ServiceBinder(this.vertx)
                .setAddress(CaptchaSvc.ADDR)
                .register(CaptchaSvc.class, this.captchaSvc);
        new ServiceBinder(this.vertx)
                .setAddress(CaptchaApi.ADDR)
                .register(CaptchaApi.class, this.captchaApi);
    }

    /**
     * 初始化要部署的Verticle类列表
     */
    @Override
    protected Map<String, Class<? extends Verticle>> getVerticleClasses() {
        final Map<String, Class<? extends Verticle>> result = new LinkedHashMap<>();
        result.put("web", WebVerticle.class);
        return result;
    }

}
