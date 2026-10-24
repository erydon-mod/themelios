package com.oliver.erydonthemelios.mixin.client;

import com.oliver.erydonthemelios.client.finish.ThemeliosStoneFinishLabelsPack;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.List;

@Mixin(LifecycledResourceManagerImpl.class)
abstract class StoneFinishResourceManagerMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static List<ResourcePack> themelios$stoneFinishLabels(List<ResourcePack> packs,
                                                                ResourceType type, List<ResourcePack> original) {
        return ThemeliosStoneFinishLabelsPack.append(type, packs);
    }
}
