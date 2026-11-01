package net.okocraft.okochat.bridge.neoforge;

import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;

final class NativeComponents {
    static net.kyori.adventure.text.Component toAdventure(Component component, HolderLookup.Provider registries) {
        var json = ComponentSerialization.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), component)
                .getOrThrow();
        return GsonComponentSerializer.gson().deserializeFromTree(json);
    }

    private NativeComponents() {
    }
}
