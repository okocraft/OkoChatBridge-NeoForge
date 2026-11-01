package net.okocraft.okochat.bridge.neoforge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.okocraft.okochat.bridge.protocol.OkoChatProtocol;

record OkoChatPayload(byte[] data) implements CustomPacketPayload {
    static final Type<OkoChatPayload> TYPE = new Type<>(Identifier.parse(OkoChatProtocol.CHANNEL));
    // OkoChat already frames its payload. Adding a Minecraft byte-array length would break the proxy protocol.
    static final StreamCodec<FriendlyByteBuf, OkoChatPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBytes(payload.data()),
            buffer -> {
                byte[] data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
                return new OkoChatPayload(data);
            }
    );

    @Override
    public Type<OkoChatPayload> type() {
        return TYPE;
    }
}
