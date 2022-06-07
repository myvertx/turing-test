package myvertx.turtest.inject;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import io.vertx.core.json.JsonObject;
import myvertx.turtest.api.CaptchaApi;
import myvertx.turtest.api.impl.CaptchaApiImpl;
import myvertx.turtest.config.MainProperties;
import myvertx.turtest.svc.CaptchaRedisSvc;
import myvertx.turtest.svc.CaptchaSvc;
import myvertx.turtest.svc.impl.CaptchaRedisSvcImpl;
import myvertx.turtest.svc.impl.CaptchaSvcImpl;

public class MainModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(CaptchaRedisSvc.class).to(CaptchaRedisSvcImpl.class).in(Singleton.class);
        bind(CaptchaSvc.class).to(CaptchaSvcImpl.class).in(Singleton.class);
        bind(CaptchaApi.class).to(CaptchaApiImpl.class).in(Singleton.class);
    }

    @Singleton
    @Provides
    public MainProperties getMainProperties(@Named("config") final JsonObject config) {
        return config.getJsonObject("main").mapTo(MainProperties.class);
    }

    @Singleton
    @Provides
    @Named("captchaTimeout")
    public Long getCaptchaTimeout(final MainProperties mainProperties) {
        return mainProperties.getCaptchaTimeout();
    }

}
