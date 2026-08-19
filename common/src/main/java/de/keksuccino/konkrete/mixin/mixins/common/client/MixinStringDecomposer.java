package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.konkrete.util.rendering.text.color.TextColorFormatter;
import de.keksuccino.konkrete.util.rendering.text.color.TextColorFormatterRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayDeque;

@Mixin(StringDecomposer.class)
public class MixinStringDecomposer {

    @Unique private static final char NO_FORMATTING_CODE_KONKRETE = '\0';
    @Unique private static final ThreadLocal<ArrayDeque<Character>> FORMATTING_CODES_KONKRETE = ThreadLocal.withInitial(ArrayDeque::new);

    /** @reason The formatting-code wrapper needs invocation-local state; a per-thread stack keeps nested text callbacks and concurrent extraction from corrupting each other. */
    @WrapMethod(method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z")
    private static boolean wrap_iterateFormatted_Konkrete(String text, int offset, Style style, Style resetStyle, FormattedCharSink sink, Operation<Boolean> original) {
        ArrayDeque<Character> formattingCodes = FORMATTING_CODES_KONKRETE.get();
        formattingCodes.addLast(NO_FORMATTING_CODE_KONKRETE);
        try {
            return original.call(text, offset, style, resetStyle, sink);
        } finally {
            formattingCodes.removeLast();
            if (formattingCodes.isEmpty()) FORMATTING_CODES_KONKRETE.remove();
        }
    }

    @WrapOperation(method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/ChatFormatting;getByCode(C)Lnet/minecraft/ChatFormatting;"))
    private static ChatFormatting wrap_getByCode_Konkrete(char code, Operation<ChatFormatting> original) {
        ArrayDeque<Character> formattingCodes = FORMATTING_CODES_KONKRETE.get();
        if (!formattingCodes.isEmpty()) {
            formattingCodes.removeLast();
            formattingCodes.addLast(code);
        }
        ChatFormatting formatting = original.call(code);
        return formatting != null || TextColorFormatterRegistry.getByCode(code) == null ? formatting : ChatFormatting.WHITE;
    }

    @WrapOperation(method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Style;applyLegacyFormat(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/Style;"))
    private static Style wrap_applyLegacyFormat_Konkrete(Style style, ChatFormatting formatting, Operation<Style> original) {
        ArrayDeque<Character> formattingCodes = FORMATTING_CODES_KONKRETE.get();
        if (formattingCodes.isEmpty()) return original.call(style, formatting);
        char code = formattingCodes.getLast();
        if (code == NO_FORMATTING_CODE_KONKRETE || ChatFormatting.getByCode(code) != null) return original.call(style, formatting);
        TextColorFormatter formatter = TextColorFormatterRegistry.getByCode(code);
        return formatter != null ? formatter.getStyle() : original.call(style, formatting);
    }

}
