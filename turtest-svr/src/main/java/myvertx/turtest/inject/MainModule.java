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

    @Singleton
    @Provides
    public MainProperties newMainProperties(@Named("config") final JsonObject config) {
        return config.getJsonObject("main").mapTo(MainProperties.class);
    }

    @Singleton
    @Provides
    public CaptchaRedisSvc getCaptchaRedisSvc(final MainProperties mainProperties) {
        return new CaptchaRedisSvcImpl(mainProperties.getCaptchaTimeout());
    }

    @Singleton
    @Provides
    public CaptchaSvc getCaptchaSvc() {
        return new CaptchaSvcImpl();
    }

    @Singleton
    @Provides
    public CaptchaApi getCaptchaApi() {
        return new CaptchaApiImpl();
    }

}
