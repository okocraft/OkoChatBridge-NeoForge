package net.okocraft.okochat.bridge.neoforge;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeComponentsTest {
    @Test
    void preservesJapaneseTextColorAndDecoration() {
        var nativeText = Component.literal("冒険者").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("こんにちは").withStyle(ChatFormatting.AQUA));
        var expected = net.kyori.adventure.text.Component.text("冒険者", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .append(net.kyori.adventure.text.Component.text("こんにちは", NamedTextColor.AQUA));
        assertEquals(expected, NativeComponents.toAdventure(nativeText, RegistryAccess.EMPTY));
    }

    @Test
    void preservesTranslateKeysAndArguments() {
        var nativeText = Component.translatable("chat.type.text", Component.literal("lazy_gon"), Component.literal("hello"));
        var expected = net.kyori.adventure.text.Component.translatable("chat.type.text",
                net.kyori.adventure.text.Component.text("lazy_gon"),
                net.kyori.adventure.text.Component.text("hello"));
        assertEquals(expected, NativeComponents.toAdventure(nativeText, RegistryAccess.EMPTY));
    }
}
