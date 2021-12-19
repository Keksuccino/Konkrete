package de.keksuccino.konkrete.mixin.client;

import de.keksuccino.konkrete.Konkrete;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CrossbowItem.class)
public abstract class MixinCrossbowItem {

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getProjectile(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"), method = "use")
    private ItemStack onGetProjectileInUse(Player instance, ItemStack bow) {
        Konkrete.LOGGER.info("GET PROJECTILE in USE");
        int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, bow);
        if (i > 0) {
            return new ItemStack(Items.ARROW);
        }
        return instance.getProjectile(bow);
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CrossbowItem;loadProjectile(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;ZZ)Z"), method = "tryLoadProjectiles")
    private static boolean onLoadProjectileInTryLoadProjectiles(LivingEntity entity, ItemStack bow, ItemStack projectile, boolean p_40866_, boolean p_40867_) {
        Konkrete.LOGGER.info("LOAD PROJECTILE in TRY LOAD PROJECTILES");
        if (entity instanceof Player) {
            int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, bow);
            if (i > 0) {
                addChargedProjectile(bow, new ItemStack(Items.ARROW));
                return true;
            }
        }
        return loadProjectile(entity, bow, projectile, p_40866_, p_40867_);
    }

    @Shadow
    protected static void addChargedProjectile(ItemStack p_40929_, ItemStack p_40930_) {
    }

    @Shadow
    protected static boolean loadProjectile(LivingEntity p_40863_, ItemStack p_40864_, ItemStack p_40865_, boolean p_40866_, boolean p_40867_) {
        return false;
    }

}
