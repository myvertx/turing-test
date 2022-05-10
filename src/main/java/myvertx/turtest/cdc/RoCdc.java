package myvertx.turtest.cdc;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import rebue.wheel.api.ro.Ro;

public class RoCdc implements MessageCodec<Ro, Ro> {
    @Override
    public void encodeToWire(Buffer buffer, Ro ro) {
        // Encode object to string
        final String jsonToStr = Json.encode(ro);
        // Length of JSON: is NOT characters count
        final int    length    = jsonToStr.getBytes().length;

        // Write data into given buffer
        buffer.appendInt(length);
        buffer.appendString(jsonToStr);
    }

    @Override
    public Ro decodeFromWire(int position, Buffer buffer) {
        // My custom message starting from this *position* of buffer
        int _pos = position;

        // Length of JSON
        final int length = buffer.getInt(_pos);

        // Get JSON string by it`s length
        // Jump 4 because getInt() == 4 bytes
        final String     jsonStr     = buffer.getString(_pos += 4, _pos += length);
        final JsonObject contentJson = new JsonObject(jsonStr);

        // We can finally create custom message object
        return contentJson.mapTo(Ro.class);
    }

    @Override
    public Ro transform(Ro ro) {
        // If a message is sent *locally* across the event bus.
        // This example sends message just as is
        return ro;
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
