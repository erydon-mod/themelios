package com.oliver.erydonthemelios.mixin.client.compat.continuity;

import com.oliver.erydonthemelios.ErydonThemelios;
import com.oliver.erydonthemelios.client.model.SynapheiaModelLoadingPlugin;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
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
    private static final Identifier THEMELIOS$CTM_OWNED_MODEL =
            new Identifier(ErydonThemelios.MOD_ID, "synapheia_owned_model");

    // Change only Continuity's local CTM lookup key. Its emissive pass still receives
    // the original baked model, and ERYDON's separate ownership redirect can coexist.
    @ModifyVariable(method = "wrap", at = @At("HEAD"), argsOnly = true,
            require = 0, remap = false)
    private Identifier themelios$keepSynapheiaCtmSinglePass(Identifier modelId) {
        if (modelId instanceof ModelIdentifier placedModel
                && SynapheiaModelLoadingPlugin.ownsCtmModel(placedModel)) {
            if (THEMELIOS$OWNERSHIP_LOGGED.compareAndSet(false, true)) {
                ErydonThemelios.LOGGER.info(
                        "[{}] Continuity compatibility active: Synapheia owns Themelios CTM; "
                                + "Continuity remains active for other models and emissive rendering.",
                        ErydonThemelios.MOD_ID);
            }
            return THEMELIOS$CTM_OWNED_MODEL;
        }
        return modelId;
    }
}
