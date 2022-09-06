package myvertx.turtest.clone;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import myvertx.turtest.to.CaptchaVerifyTo;

@Mapper
public interface MapStructRegister {
    MapStructRegister INSTANCE = Mappers.getMapper(MapStructRegister.class);

    ImageCaptchaTrack toImageCaptchaTrack(CaptchaVerifyTo to);
}
