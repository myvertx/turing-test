package myvertx.turtest.verticle;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.inject.Module;

import io.vertx.core.Verticle;
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

    /**
     * 添加guice模块
     *
     * @param guiceModules 添加guice模块到此列表
     */
    @Override
    protected void addGuiceModules(final List<Module> guiceModules) {
        guiceModules.add(new RedisGuiceModule());
        guiceModules.add(new MainModule());
    }

    /**
     * 部署前
     */
    @Override
    protected void beforeDeploy() {
        log.info("注册服务");
        new ServiceBinder(this.vertx)
                .setAddress(CaptchaSvc.ADDR)
                .register(CaptchaSvc.class, this.captchaSvc);
        new ServiceBinder(this.vertx)
                .setAddress(CaptchaApi.ADDR)
                .register(CaptchaApi.class, this.captchaApi);
    }

    /**
     * 添加要部署的Verticle类列表
     *
     * @param verticleClasses 添加Verticle类到此列表
     */
    @Override
    protected void addVerticleClasses(final Map<String, Class<? extends Verticle>> verticleClasses) {
        verticleClasses.put("web", WebVerticle.class);
    }

}
