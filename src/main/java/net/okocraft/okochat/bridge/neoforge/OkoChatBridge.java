package net.okocraft.okochat.bridge.neoforge;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.okocraft.okochat.bridge.protocol.BlockPosition;
import net.okocraft.okochat.bridge.protocol.OkoChatProtocol;
import net.okocraft.okochat.bridge.protocol.ServerChatMessageData;
import net.okocraft.okochat.bridge.protocol.ServerSenderData;
import net.okocraft.okochat.integration.AffixProvider;
import net.okocraft.okochat.integration.luckperms.LuckPermsIntegration;
import org.slf4j.Logger;

@Mod(value = OkoChatBridge.ID, dist = Dist.DEDICATED_SERVER)
public final class OkoChatBridge {
    public static final String ID = "okochat_bridge";
    private static final Logger LOGGER = LogUtils.getLogger();
    private AffixProvider<ServerPlayer> affixProvider;

    public OkoChatBridge(IEventBus modBus) {
        modBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::onChat);
        LOGGER.info("OkoChat NeoForge bridge enabled on {}", OkoChatProtocol.CHANNEL);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Velocity consumes this packet. Players do not install a matching client mod.
        event.registrar(Byte.toString(OkoChatProtocol.VERSION)).optional()
                .playToClient(OkoChatPayload.TYPE, OkoChatPayload.CODEC);
    }

    private void onChat(ServerChatEvent event) {
        // Never fall back to local broadcast: a private-channel message must stay private on relay failure.
        event.setCanceled(true);
        ServerPlayer player = event.getPlayer();
        try {
            if (!NetworkRegistry.hasChannel(player.connection, OkoChatPayload.TYPE.id())) {
                LOGGER.warn("Cannot relay chat for {}: the proxy has not registered {}",
                        event.getUsername(), OkoChatProtocol.CHANNEL);
                sendFailure(player);
                return;
            }

            var affixes = this.affixes();
            var position = player.blockPosition();
            var data = new ServerChatMessageData(
                    new ServerSenderData(
                            player.getUUID(),
                            event.getUsername(),
                            NativeComponents.toAdventure(player.getDisplayName(), player.registryAccess()),
                            affixes.getPrefix(player),
                            affixes.getSuffix(player),
                            worldName(player),
                            new BlockPosition(position.getX(), position.getY(), position.getZ())
                    ),
                    NativeComponents.toAdventure(event.getMessage(), player.registryAccess())
            );
            byte[] bytes = OkoChatProtocol.encodeData(OkoChatProtocol.CHAT, data);
            player.connection.send(new ClientboundCustomPayloadPacket(new OkoChatPayload(bytes)));
            LOGGER.info("{}: {}", event.getUsername(), event.getMessage().getString());
        } catch (Exception error) {
            LOGGER.error("Failed to relay chat for {}", event.getUsername(), error);
            sendFailure(player);
        }
    }

    private AffixProvider<ServerPlayer> affixes() {
        if (this.affixProvider == null) {
            this.affixProvider = LuckPermsIntegration.canIntegrate()
                    ? LuckPermsIntegration.createAffixProvider(ServerPlayer::getUUID)
                    : AffixProvider.createVoid();
            LOGGER.info("Chat affix provider: {}", this.affixProvider.getProviderName().isEmpty()
                    ? "none" : this.affixProvider.getProviderName());
        }
        return this.affixProvider;
    }

    private static String worldName(ServerPlayer player) {
        String levelName = player.level().getServer().getWorldData().getLevelName();
        var dimension = player.level().dimension();
        if (dimension.equals(Level.OVERWORLD)) {
            return levelName;
        }
        if (dimension.equals(Level.NETHER)) {
            return levelName + "_nether";
        }
        if (dimension.equals(Level.END)) {
            return levelName + "_the_end";
        }
        return dimension.identifier().toString();
    }

    private static void sendFailure(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("チャットを送信できませんでした。").withStyle(ChatFormatting.RED));
    }
}
