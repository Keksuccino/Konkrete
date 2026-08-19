package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.konkrete.util.resource.PackResourcesRootEnumeration;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FilePackResources.class)
public class MixinFilePackResources {

    /** @reason MC 26.2 creates a double-slash prefix when the generic pack API enumerates an archive namespace root, which otherwise hides every root and nested resource in that namespace. */
    @WrapOperation(method = "listResources", at = @At(value = "INVOKE", target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z"))
    private boolean wrap_listResourcesPrefix_Konkrete(String resourcePath, String prefix, Operation<Boolean> original, PackType type, String namespace, String directory, PackResources.ResourceOutput output) {
        // MC appends a slash to an already slash-terminated namespace root for directory "".
        return original.call(resourcePath, PackResourcesRootEnumeration.normalizeArchivePrefix(prefix, directory));
    }

}
