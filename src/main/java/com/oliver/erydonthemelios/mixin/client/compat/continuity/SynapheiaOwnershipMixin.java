package com.oliver.erydonthemelios.mixin.client.compat.continuity;

import com.oliver.erydonthemelios.ErydonThemelios;
import com.oliver.erydonthemelios.client.model.SynapheiaModelLoadingPlugin;
import net.minecraft.client.util.ModelIdentifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.concurrent.atomic.AtomicBoolean;

/** Leaves one CTM owner per placed model while retaining Continuity's emissive pass. */
@Pseudo
@Mixin(targets = "me.pepperbell.continuity.client.resource.ModelWrappingHandler", remap = false)
public abstract class SynapheiaOwnershipMixin {
    private static final AtomicBoolean THEMELIOS$OWNERSHIP_LOGGED = new AtomicBoolean();

    // The top-level ID is nullable in Continuity's 1.21 API. Its separate resource
    // ID and baked model remain intact for the emissive pass and other mods' hooks.
    @ModifyVariable(method = "wrap", at = @At("HEAD"), argsOnly = true,
            require = 0, remap = false)
    private ModelIdentifier themelios$keepSynapheiaCtmSinglePass(ModelIdentifier modelId) {
        if (SynapheiaModelLoadingPlugin.ownsCtmModel(modelId)) {
            if (THEMELIOS$OWNERSHIP_LOGGED.compareAndSet(false, true)) {
                ErydonThemelios.LOGGER.info(
                        "[{}] Continuity compatibility active: Synapheia owns Themelios CTM; "
                                + "Continuity remains active for other models and emissive rendering.",
                        ErydonThemelios.MOD_ID);
            }
            return null;
        }
        return modelId;
    }
}
