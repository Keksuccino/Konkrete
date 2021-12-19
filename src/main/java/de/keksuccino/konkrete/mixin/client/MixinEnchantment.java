package de.keksuccino.konkrete.mixin.client;

import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ArrowInfiniteEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class MixinEnchantment {

    @Inject(at = @At("HEAD"), method = "canEnchant", cancellable = true)
    private void onCanEnchant(ItemStack itemToEnchant, CallbackInfoReturnable<Boolean> info) {
        if (itemToEnchant.getItem() instanceof CrossbowItem) {
            Enchantment e = (Enchantment) ((Object)this);
            if (e instanceof ArrowInfiniteEnchantment) {
                info.setReturnValue(true);
            }
        }
    }

}
