package myvertx.turtest.clone;

import cloud.tianai.captcha.template.slider.validator.common.model.dto.SliderCaptchaTrack;
import myvertx.turtest.to.CaptchaVerifyTo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MapStructRegister {
    MapStructRegister INSTANCE = Mappers.getMapper(MapStructRegister.class);

    SliderCaptchaTrack toSliderCaptchaTrack(CaptchaVerifyTo to);
}
