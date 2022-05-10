package myvertx.turtest.verticle;

import static cloud.tianai.captcha.template.slider.generator.impl.StandardSliderCaptchaGenerator.DEFAULT_SLIDER_IMAGE_TEMPLATE_PATH;

import java.util.HashMap;
import java.util.Map;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import cloud.tianai.captcha.template.slider.generator.SliderCaptchaGenerator;
import cloud.tianai.captcha.template.slider.generator.common.constant.SliderCaptchaConstant;
import cloud.tianai.captcha.template.slider.generator.common.model.dto.GenerateParam;
import cloud.tianai.captcha.template.slider.generator.common.model.dto.SliderCaptchaInfo;
import cloud.tianai.captcha.template.slider.generator.impl.CacheSliderCaptchaGenerator;
import cloud.tianai.captcha.template.slider.generator.impl.StandardSliderCaptchaGenerator;
import cloud.tianai.captcha.template.slider.resource.ResourceStore;
import cloud.tianai.captcha.template.slider.resource.SliderCaptchaResourceManager;
import cloud.tianai.captcha.template.slider.resource.common.model.dto.Resource;
import cloud.tianai.captcha.template.slider.resource.impl.DefaultSliderCaptchaResourceManager;
import cloud.tianai.captcha.template.slider.resource.impl.provider.ClassPathResourceProvider;
import cloud.tianai.captcha.template.slider.validator.SliderCaptchaValidator;
import cloud.tianai.captcha.template.slider.validator.common.model.dto.SliderCaptchaTrack;
import cloud.tianai.captcha.template.slider.validator.impl.BasicCaptchaTrackValidator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import lombok.extern.slf4j.Slf4j;
import myvertx.turtest.config.WebProperties;
import myvertx.turtest.ra.GenCaptchaRa;
import myvertx.turtest.ra.RedisGetCaptchaRa;
import myvertx.turtest.to.RedisGetCaptchaTo;
import myvertx.turtest.to.RedisSetCaptchaTo;
import rebue.wheel.api.ro.Ro;

@Slf4j
public class WebVerticle extends AbstractVerticle {

    // 验证码资源管理器
    private final SliderCaptchaResourceManager sliderCaptchaResourceManager = new DefaultSliderCaptchaResourceManager();
    // 负责计算一些数据存到缓存中，用于校验使用
    // SliderCaptchaValidator负责校验用户滑动滑块是否正确和生成滑块的一些校验数据; 比如滑块到凹槽的百分比值
    private final SliderCaptchaValidator       sliderCaptchaValidator       = new BasicCaptchaTrackValidator();
    private SliderCaptchaGenerator             sliderCaptchaGenerator;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        initCaptchaGenerator();

        loadCaptchaResource();

        final WebProperties webProperties = config().mapTo(WebProperties.class);

        final Router        router        = Router.router(vertx);

        // CORS
        router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET));

        // 生成并获取验证码图像
        router.get("/captcha/gen").handler(this::handleCaptchaGen);

        // 校验验证码
        router.post("/captcha/verify").handler(BodyHandler.create()).handler(this::handleCaptchaVerify);

        vertx.createHttpServer().requestHandler(router).listen(webProperties.getPort(), res -> {
            if (res.succeeded()) {
                log.info("HTTP server started on port " + res.result().actualPort());
                startPromise.complete();
            }
            else {
                log.error("HTTP server start fail", res.cause());
                startPromise.fail(res.cause());
            }
        });
    }

    /**
     * 处理验证码生成
     *
     * @param ctx 路由上下文
     */
    private void handleCaptchaGen(RoutingContext ctx) {
        log.debug("handleCaptchaGen");
        // 生成滑块图片
        final SliderCaptchaInfo   slideImageInfo = sliderCaptchaGenerator.generateSlideImageInfo();

        final String              captchaId      = NanoIdUtils.randomNanoId();
        // 这个map数据应该存到缓存中，校验的时候需要用到该数据
        final Map<String, Object> map            = sliderCaptchaValidator.generateSliderCaptchaValidData(slideImageInfo);

        vertx.eventBus().request(RedisVerticle.EVENT_BUS_REDIS_SET_CAPTCHA, new RedisSetCaptchaTo(captchaId, map), res -> {
            if (res.succeeded()) {
                ctx.response().end(Json.encode(
                        Ro.success("获取验证码成功",
                                new GenCaptchaRa(
                                        captchaId,
                                        slideImageInfo.getBackgroundImage(),
                                        slideImageInfo.getSliderImage()))));

            }
            else {
                final String msg = "获取验证码失败";
                log.error(msg, res.cause());
                ctx.response().end(Json.encode(Ro.fail(msg, res.cause().toString())));
            }
        });
    }

    /**
     * 处理验证码校验
     *
     * @param ctx 路由上下文
     */
    private void handleCaptchaVerify(RoutingContext ctx) {
        log.debug("handleCaptchaVerify");
        final String             captchaId          = ctx.request().getParam("id");
        final SliderCaptchaTrack sliderCaptchaTrack = ctx.getBodyAsJson().mapTo(SliderCaptchaTrack.class);
        if (sliderCaptchaTrack.getTrackList() == null || sliderCaptchaTrack.getTrackList().isEmpty()) {
            final Ro<?>  ro    = Ro.warn("校验验证码失败");
            final String roStr = Json.encode(ro);
            ctx.response().end(roStr);
            return;
        }

        vertx.eventBus().<Ro<?>>request(RedisVerticle.EVENT_BUS_REDIS_GET_CAPTCHA, new RedisGetCaptchaTo(captchaId), res -> {
            Ro<?> ro;
            if (res.succeeded()) {
                final Ro<?> redisGetCaptchaRo = res.result().body();
                if (redisGetCaptchaRo.isSuccess()) {
                    final Map<String, Object> map = ((Ro<RedisGetCaptchaRa>) redisGetCaptchaRo).getExtra().getMap();

                    // 用户传来的行为轨迹和进行校验
                    // - sliderCaptchaTrack为前端传来的滑动轨迹数据
                    // - map 为生成验证码时缓存的map数据
                    final boolean check = sliderCaptchaValidator.valid(sliderCaptchaTrack, map);

                    ro = check ? Ro.success("校验验证码成功") : Ro.warn("校验验证码失败");
                }
                else {
                    ro = Ro.fail("校验验证码失败", redisGetCaptchaRo.getMsg());
                }
            }
            else {
                ro = Ro.fail("校验验证码失败", "调用Redis异常");
            }
            final String roStr = Json.encode(ro);
            ctx.response().end(roStr);
        });
    }

    /**
     * 初始化Captcha生成器
     */
    private void initCaptchaGenerator() {
        // 使用 CacheSliderCaptchaTemplate 对滑块验证码进行缓存，使其提前生成滑块图片
        // 参数一: 真正实现 滑块的 SliderCaptchaTemplate
        // 参数二: 默认提前缓存多少个
        // 参数三: 出错后 等待xx时间再进行生成
        // 参数四: 检查时间间隔
        final CacheSliderCaptchaGenerator cacheSliderCaptchaGenerator = new CacheSliderCaptchaGenerator(
                new StandardSliderCaptchaGenerator(sliderCaptchaResourceManager, true),
                GenerateParam.builder()
                        .sliderFormatName("webp")
                        .backgroundFormatName("webp")
                        // 是否添加混淆滑块
                        .obfuscate(false)
                        .build(),
                10, 1000, 100);
        cacheSliderCaptchaGenerator.initSchedule();
        sliderCaptchaGenerator = cacheSliderCaptchaGenerator;
    }

    /**
     * 加载Captcha资源
     */
    private void loadCaptchaResource() {
        final ResourceStore resourceStore = sliderCaptchaResourceManager.getResourceStore();
        // 清除内置的背景图片
        resourceStore.clearResources();
        // 添加自定义背景图片
        resourceStore.addResource(new Resource("classpath", "img/bg/01.png"));
        // 添加滑块图像
        addSliderImage("01", resourceStore);
        addSliderImage("02", resourceStore);
        addSliderImage("03", resourceStore);
        addSliderImage("04", resourceStore);
        addSliderImage("05", resourceStore);
        addSliderImage("06", resourceStore);
        addSliderImage("07", resourceStore);
        addSliderImage("08", resourceStore);
        addSliderImage("09", resourceStore);
        addSliderImage("10", resourceStore);
        addSliderImage("11", resourceStore);
        addSliderImage("12", resourceStore);

    }

    /**
     * 添加滑块图像
     *
     * @param imageName     图像名称
     * @param resourceStore 资源库
     */
    private void addSliderImage(String imageName, ResourceStore resourceStore) {
        // 添加模板
        final Map<String, Resource> template = new HashMap<>(4);
        template.put(SliderCaptchaConstant.TEMPLATE_FIXED_IMAGE_NAME, new Resource(ClassPathResourceProvider.NAME, "img/slider/" + imageName + "a.png"));
        template.put(SliderCaptchaConstant.TEMPLATE_ACTIVE_IMAGE_NAME, new Resource(ClassPathResourceProvider.NAME, "img/slider/" + imageName + "b.png"));
        template.put(SliderCaptchaConstant.TEMPLATE_MATRIX_IMAGE_NAME, new Resource(ClassPathResourceProvider.NAME, DEFAULT_SLIDER_IMAGE_TEMPLATE_PATH.concat("/2/matrix.png")));
        resourceStore.addTemplate(template);
    }
}
