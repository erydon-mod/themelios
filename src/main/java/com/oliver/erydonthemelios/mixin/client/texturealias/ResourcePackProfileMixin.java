package com.oliver.erydonthemelios.mixin.client.texturealias;

import com.oliver.erydonthemelios.client.texturealias.CollectionResourcePackCompatibility;
import com.oliver.erydonthemelios.client.texturealias.FamilyTextureAliasCoordinator;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackCompatibility;
import net.minecraft.resource.ResourcePackProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ResourcePackProfile.class, priority = 900)
abstract class ResourcePackProfileMixin {
    @Unique
    private Boolean themelios$legacyCollectionPack;

    @Inject(method = "getCompatibility", at = @At("HEAD"), cancellable = true)
    private void themelios$markLegacyCollectionPackTooOld(
            CallbackInfoReturnable<ResourcePackCompatibility> info
    ) {
        if (!FamilyTextureAliasCoordinator.isLeader()) {
            return;
        }

        ResourcePackProfile profile = (ResourcePackProfile) (Object) this;
        if (themelios$legacyCollectionPack == null) {
            themelios$legacyCollectionPack =
                    CollectionResourcePackCompatibility.shouldMarkTooOld(
                            profile.getId(),
                            profile.getDescription().getString()
                    );
        }
        if (themelios$legacyCollectionPack) {
            info.setReturnValue(ResourcePackCompatibility.TOO_OLD);
        }
    }

    @Inject(method = "createResourcePack", at = @At("RETURN"), cancellable = true)
    private void themelios$wrapExactTextureAliasPack(
            CallbackInfoReturnable<ResourcePack> info
    ) {
        if (!FamilyTextureAliasCoordinator.isLeader()) {
            return;
        }

        ResourcePack original = info.getReturnValue();
        ResourcePackProfile profile = (ResourcePackProfile) (Object) this;
        ResourcePack pack = CollectionResourcePackCompatibility.disableIfLegacy(
                profile.getId(),
                profile.getDescription().getString(),
                original
        );
        ResourcePack wrapped =
                FamilyTextureAliasCoordinator.wrapFamilyNamespaces(pack);
        if (wrapped != pack) {
            info.setReturnValue(wrapped);
        } else if (pack != original) {
            info.setReturnValue(pack);
        }
    }
}
