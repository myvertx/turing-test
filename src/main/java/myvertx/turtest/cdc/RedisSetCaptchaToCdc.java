package myvertx.turtest.cdc;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import myvertx.turtest.to.RedisSetCaptchaTo;

public class RedisSetCaptchaToCdc implements MessageCodec<RedisSetCaptchaTo, RedisSetCaptchaTo> {
    @Override
    public void encodeToWire(Buffer buffer, RedisSetCaptchaTo redisSetCaptchaTo) {
        // Encode object to string
        String jsonToStr = Json.encode(redisSetCaptchaTo);
        // Length of JSON: is NOT characters count
        int length = jsonToStr.getBytes().length;

        // Write data into given buffer
        buffer.appendInt(length);
        buffer.appendString(jsonToStr);
    }

    @Override
    public RedisSetCaptchaTo decodeFromWire(int position, Buffer buffer) {
        // My custom message starting from this *position* of buffer
        int _pos = position;

        // Length of JSON
        int length = buffer.getInt(_pos);

        // Get JSON string by it`s length
        // Jump 4 because getInt() == 4 bytes
        String jsonStr = buffer.getString(_pos += 4, _pos += length);
        JsonObject contentJson = new JsonObject(jsonStr);

        // We can finally create custom message object
        return contentJson.mapTo(RedisSetCaptchaTo.class);
    }

    @Override
    public RedisSetCaptchaTo transform(RedisSetCaptchaTo redisSetCaptchaTo) {
        // If a message is sent *locally* across the event bus.
        // This example sends message just as is
        return redisSetCaptchaTo;
    }

    @Override
    public String name() {
        // Each codec must have a unique name.
        // This is used to identify a codec when sending a message and for unregistering codecs.
        return this.getClass().getSimpleName();
    }

    @Override
    public byte systemCodecID() {
        // Always -1
        return -1;
    }
}
