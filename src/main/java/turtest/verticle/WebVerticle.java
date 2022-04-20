package turtest.verticle;

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
import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import lombok.extern.slf4j.Slf4j;
import turtest.config.WebProperties;
import turtest.ra.GenCaptchaRa;
import turtest.ra.RedisGetCaptchaRa;
import turtest.ro.Ro;
import turtest.to.RedisGetCaptchaTo;
import turtest.to.RedisSetCaptchaTo;

import java.util.HashMap;
import java.util.Map;

import static cloud.tianai.captcha.template.slider.generator.impl.StandardSliderCaptchaGenerator.DEFAULT_SLIDER_IMAGE_TEMPLATE_PATH;


@Slf4j
public class WebVerticle extends AbstractVerticle {

    // 验证码资源管理器
    private final SliderCaptchaResourceManager sliderCaptchaResourceManager = new DefaultSliderCaptchaResourceManager();
    // 负责计算一些数据存到缓存中，用于校验使用
    // SliderCaptchaValidator负责校验用户滑动滑块是否正确和生成滑块的一些校验数据; 比如滑块到凹槽的百分比值
    private final SliderCaptchaValidator sliderCaptchaValidator = new BasicCaptchaTrackValidator();
    private SliderCaptchaGenerator sliderCaptchaGenerator;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        // 使用 CacheSliderCaptchaTemplate 对滑块验证码进行缓存，使其提前生成滑块图片
        // 参数一: 真正实现 滑块的 SliderCaptchaTemplate
        // 参数二: 默认提前缓存多少个
        // 参数三: 出错后 等待xx时间再进行生成
        // 参数四: 检查时间间隔
        CacheSliderCaptchaGenerator cacheSliderCaptchaGenerator = new CacheSliderCaptchaGenerator(
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

        loadResource();

        WebProperties webProperties = config().mapTo(WebProperties.class);

        Router router = Router.router(vertx);

        // CORS
        router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET));

        // 生成并获取验证码图像
        router.get("/captcha/gen").handler(ctx -> {
            // 生成滑块图片
            SliderCaptchaInfo slideImageInfo = sliderCaptchaGenerator.generateSlideImageInfo();

            String captchaId = NanoIdUtils.randomNanoId();
            // 这个map数据应该存到缓存中，校验的时候需要用到该数据
            Map<String, Object> map = sliderCaptchaValidator.generateSliderCaptchaValidData(slideImageInfo);

            vertx.eventBus().request(RedisVerticle.EVENT_BUS_REDIS_SET_CAPTCHA, new RedisSetCaptchaTo(captchaId, map), res -> {
                if (res.succeeded()) {
                    ctx.response().putHeader("content-type", "application/json").end(Json.encode(Ro.newSuccess("获取验证码成功", new GenCaptchaRa(captchaId, slideImageInfo.getBackgroundImage(), slideImageInfo.getSliderImage()))));

                } else {
                    String msg = "获取验证码失败";
                    log.error(msg, res.cause());
                    ctx.response().putHeader("content-type", "application/json").end(Json.encode(Ro.newFail(msg, res.cause().toString())));
                }
            });

        });

        // 校验验证码
        router.post("/captcha/verify").handler(BodyHandler.create()).handler(ctx -> {
            String captchaId = ctx.request().getParam("id");
            SliderCaptchaTrack sliderCaptchaTrack = ctx.getBodyAsJson().mapTo(SliderCaptchaTrack.class);
            if (sliderCaptchaTrack.getTrackList() == null || sliderCaptchaTrack.getTrackList().isEmpty()) {
                Ro<?> ro = Ro.newWarn("校验验证码失败");
                String roStr = Json.encode(ro);
                ctx.response().putHeader("content-type", "application/json").end(roStr);
            }

            vertx.eventBus().<Ro<?>>request(RedisVerticle.EVENT_BUS_REDIS_GET_CAPTCHA, new RedisGetCaptchaTo(captchaId), res -> {
                Ro<?> ro;
                if (res.succeeded()) {
                    Ro<?> redisGetCaptchaRo = res.result().body();
                    if (redisGetCaptchaRo.isSuccess()) {
                        Map<String, Object> map = ((Ro<RedisGetCaptchaRa>) redisGetCaptchaRo).getExtra().getMap();

                        // 用户传来的行为轨迹和进行校验
                        // - sliderCaptchaTrack为前端传来的滑动轨迹数据
                        // - map 为生成验证码时缓存的map数据
                        boolean check = sliderCaptchaValidator.valid(sliderCaptchaTrack, map);

                        ro = check ? Ro.newSuccess("校验验证码成功") : Ro.newWarn("校验验证码失败");
                    } else {
                        ro = Ro.newFail("校验验证码失败", redisGetCaptchaRo.getDetail());
                    }
                } else {
                    ro = Ro.newFail("校验验证码失败", "调用Redis异常");
                }
                String roStr = Json.encode(ro);
                ctx.response().putHeader("content-type", "application/json").end(roStr);
            });
        });

        vertx.createHttpServer().requestHandler(router).listen(webProperties.getPort(), res -> {
            if (res.succeeded()) {
                log.info("HTTP server started on port " + res.result().actualPort());
                startPromise.complete();
            } else {
                log.error("HTTP server start fail", res.cause());
                startPromise.fail(res.cause());
            }
        });
    }

    /**
     * 加载资源
     */
    private void loadResource() {
        ResourceStore resourceStore = sliderCaptchaResourceManager.getResourceStore();
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
        Map<String, Resource> template = new HashMap<>(4);
        template.put(SliderCaptchaConstant.TEMPLATE_FIXED_IMAGE_NAME, new Resource(ClassPathResourceProvider.NAME, "img/slider/" + imageName + "a.png"));
        template.put(SliderCaptchaConstant.TEMPLATE_ACTIVE_IMAGE_NAME, new Resource(ClassPathResourceProvider.NAME, "img/slider/" + imageName + "b.png"));
        template.put(SliderCaptchaConstant.TEMPLATE_MATRIX_IMAGE_NAME, new Resource(ClassPathResourceProvider.NAME, DEFAULT_SLIDER_IMAGE_TEMPLATE_PATH.concat("/2/matrix.png")));
        resourceStore.addTemplate(template);
    }
}
