package net.okocraft.okochat.bridge.neoforge;

import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.FriendlyByteBuf;
import net.okocraft.okochat.bridge.protocol.BlockPosition;
import net.okocraft.okochat.bridge.protocol.OkoChatProtocol;
import net.okocraft.okochat.bridge.protocol.ServerChatMessageData;
import net.okocraft.okochat.bridge.protocol.ServerSenderData;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProxyProtocolTest {
    @Test
    void customPayloadDoesNotAddMinecraftLengthFraming() {
        byte[] wire = {1, 1, 0, 0, 0, 42, (byte) 255};
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            OkoChatPayload.CODEC.encode(buffer, new OkoChatPayload(wire));
            byte[] actual = new byte[buffer.readableBytes()];
            buffer.getBytes(buffer.readerIndex(), actual);
            assertArrayEquals(wire, actual);
            assertArrayEquals(wire, OkoChatPayload.CODEC.decode(buffer).data());
            assertEquals(0, buffer.readableBytes());
            assertEquals("okochat:messaging", OkoChatPayload.TYPE.id().toString());
        } finally {
            buffer.release();
        }
    }

    @Test
    void chatMatchesTheExistingProxyDataInputLayout() throws Exception {
        var id = UUID.fromString("b2677dd7-14d0-47b7-a646-015ed165a494");
        var sender = new ServerSenderData(id, "lazy_gon", Component.text("旅人", NamedTextColor.GOLD),
                "&6[冒険者]", "&r", "world", new BlockPosition(-17, 64, 257));
        byte[] bytes = OkoChatProtocol.encodeData(OkoChatProtocol.CHAT,
                new ServerChatMessageData(sender, Component.text("Z:こんにちは\u0000😀")));
        try (var input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            assertEquals(1, input.readUnsignedByte());
            assertEquals(1, input.readUnsignedByte());
            assertEquals(0xb2677dd714d047b7L, input.readLong());
            assertEquals(0xa646015ed165a494L, input.readLong());
            assertEquals("lazy_gon", input.readUTF());
            var display = JsonParser.parseString(input.readUTF()).getAsJsonObject();
            assertEquals("旅人", display.get("text").getAsString());
            assertEquals("gold", display.get("color").getAsString());
            assertEquals("&6[冒険者]", input.readUTF());
            assertEquals("&r", input.readUTF());
            assertEquals("world", input.readUTF());
            assertTrue(input.readBoolean());
            assertEquals(-17, input.readInt());
            assertEquals(64, input.readInt());
            assertEquals(257, input.readInt());
            var message = JsonParser.parseString(input.readUTF());
            assertEquals("Z:こんにちは\u0000😀", message.isJsonPrimitive()
                    ? message.getAsString() : message.getAsJsonObject().get("text").getAsString());
            assertEquals(-1, input.read());
        }
    }
}
