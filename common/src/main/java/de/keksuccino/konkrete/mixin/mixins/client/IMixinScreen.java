package de.keksuccino.konkrete.mixin.mixins.client;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

@Mixin(Screen.class)
public interface IMixinScreen {

    @Accessor("narratables") List<NarratableEntry> get_Narratables_Konkrete();

    @Accessor("renderables") List<Renderable> getRenderablesKonkrete();

    @Accessor("children") List<GuiEventListener> getChildrenKonkrete();

    @Accessor("font") void setFontKonkrete(Font font);

//    @Invoker("addWidget") <T extends GuiEventListener & NarratableEntry> T invokeAddWidgetKonkrete(T p_96625_);

//    @Invoker("addRenderableWidget") <T extends GuiEventListener & Renderable & NarratableEntry> T invokeAddRenderableWidgetKonkrete(T p_169406_);

}
