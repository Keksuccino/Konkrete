package de.keksuccino.konkrete;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.konkrete.command.ClientExecutor;
import de.keksuccino.konkrete.command.CommandUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class TestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("konk").then(Commands.argument("menu_identifier", StringArgumentType.string())
                .executes((stack) -> {
                    return openGui(stack.getSource(), StringArgumentType.getString(stack, "menu_identifier"));
                })
                .suggests((context, provider) -> {
                    return CommandUtils.getStringSuggestions(provider, "this", "is", "a_test");
                })
        ));
    }

    private static int openGui(CommandSourceStack stack, String menuIdentifierOrCustomGuiName) {
        ClientExecutor.execute(() -> {
            try {
                Minecraft.getInstance().setScreen(new PauseScreen(true));
                stack.sendSuccess(new TextComponent("SUCCESS: " + menuIdentifierOrCustomGuiName), false);
            } catch (Exception e) {
                stack.sendFailure(new TextComponent(Locals.localize("fancymenu.commands.openguiscreen.error")));
                e.printStackTrace();
            }
        });
        return 1;
    }

}