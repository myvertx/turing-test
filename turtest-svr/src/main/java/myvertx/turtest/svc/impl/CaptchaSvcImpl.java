package myvertx.turtest.svc.impl;

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
import io.vertx.core.Future;
import io.vertx.serviceproxy.ServiceException;
import lombok.extern.slf4j.Slf4j;
import myvertx.turtest.clone.MapStructRegister;
import myvertx.turtest.ra.CaptchaGenRa;
import myvertx.turtest.svc.CaptchaRedisSvc;
import myvertx.turtest.svc.CaptchaSvc;
import myvertx.turtest.to.CaptchaRedisSetTo;
import myvertx.turtest.to.CaptchaVerifyTo;
import rebue.wheel.api.err.ErrCode;

@Slf4j
public class CaptchaSvcImpl implements CaptchaSvc {
    // 验证码资源管理器
    private final SliderCaptchaResourceManager sliderCaptchaResourceManager = new DefaultSliderCaptchaResourceManager();
    // 负责计算一些数据存到缓存中，用于校验使用
    // SliderCaptchaValidator负责校验用户滑动滑块是否正确和生成滑块的一些校验数据; 比如滑块到凹槽的百分比值
    private final SliderCaptchaValidator       sliderCaptchaValidator       = new BasicCaptchaTrackValidator();
    private SliderCaptchaGenerator             sliderCaptchaGenerator;

    private final CaptchaRedisSvc              captchaRedisSvc;

    public CaptchaSvcImpl(final CaptchaRedisSvc redisSvc) {
        this.captchaRedisSvc = redisSvc;

        initCaptchaGenerator();

        loadCaptchaResource();
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
                new StandardSliderCaptchaGenerator(this.sliderCaptchaResourceManager, true),
                GenerateParam.builder()
                        .sliderFormatName("webp")
                        .backgroundFormatName("webp")
                        // 是否添加混淆滑块
                        .obfuscate(false)
                        .build(),
                10, 1000, 100);
        cacheSliderCaptchaGenerator.initSchedule();
        this.sliderCaptchaGenerator = cacheSliderCaptchaGenerator;
    }

    /**
     * 加载Captcha资源
     */
    private void loadCaptchaResource() {
        final ResourceStore resourceStore = this.sliderCaptchaResourceManager.getResourceStore();
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
    private void addSliderImage(final String imageName, final ResourceStore resourceStore) {
        // 添加模板
        final Map<String, Resource> template = new HashMap<>(4);
        template.put(SliderCaptchaConstant.TEMPLATE_FIXED_IMAGE_NAME, new Resource(
                ClassPathResourceProvider.NAME,
                "img/slider/" + imageName + "a.png"));
        template.put(SliderCaptchaConstant.TEMPLATE_ACTIVE_IMAGE_NAME, new Resource(
                ClassPathResourceProvider.NAME,
                "img/slider/" + imageName + "b.png"));
        template.put(SliderCaptchaConstant.TEMPLATE_MATRIX_IMAGE_NAME, new Resource(
                ClassPathResourceProvider.NAME,
                StandardSliderCaptchaGenerator.DEFAULT_SLIDER_IMAGE_TEMPLATE_PATH.concat("/2/matrix.png")));
        resourceStore.addTemplate(template);
    }

    /**
     * 生成验证码
     */
    @Override
    public Future<CaptchaGenRa> gen() {
        log.debug("captcha gen");
        // 生成滑块图片
        final SliderCaptchaInfo   slideImageInfo = this.sliderCaptchaGenerator.generateSlideImageInfo();

        final String              captchaId      = NanoIdUtils.randomNanoId();
        // 这个map数据应该存到缓存中，校验的时候需要用到该数据
        final Map<String, Object> map            = this.sliderCaptchaValidator.generateSliderCaptchaValidData(slideImageInfo);

        return this.captchaRedisSvc.setCaptcha(new CaptchaRedisSetTo(captchaId, map))
                .compose(res -> {
                    log.debug("redis.getCaptcha result: {}", res);
                    return Future.succeededFuture(CaptchaGenRa.builder()
                            .id(captchaId)
                            .backgroundImage(slideImageInfo.getBackgroundImage())
                            .sliderImage(slideImageInfo.getSliderImage())
                            .build());
                })
                .recover(Future::failedFuture);

    }

    /**
     * 校验验证码
     *
     * @param to 校验验证码的参数
     */
    @Override
    public Future<Boolean> verify(final CaptchaVerifyTo to) {
        log.debug("captcha verify: {}", to);
        if (to.getTrackList() == null || to.getTrackList().isEmpty()) {
            final String msg = "校验验证码参数错误";
            return Future.failedFuture(new ServiceException(ErrCode.ILLEGAL_ARGUMENT, msg));
        }

        return this.captchaRedisSvc.getCaptcha(to.getCaptchaId())
                .compose(ra -> {
                    if (ra == null) {
                        return Future.succeededFuture(false);
                    }

                    final Map<String, Object> map = ra.getMap();

                    // 用户传来的行为轨迹和进行校验
                    // - sliderCaptchaTrack为前端传来的滑动轨迹数据
                    // - map 为生成验证码时缓存的map数据
                    final SliderCaptchaTrack sliderCaptchaTrack = MapStructRegister.INSTANCE.toSliderCaptchaTrack(to);
                    final boolean    check              = this.sliderCaptchaValidator.valid(sliderCaptchaTrack, map);
                    return Future.succeededFuture(check);
                })
                .recover(Future::failedFuture);

    }

}
