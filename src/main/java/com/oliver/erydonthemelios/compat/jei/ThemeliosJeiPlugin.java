package com.oliver.erydonthemelios.compat.jei;

import com.oliver.erydonthemelios.ErydonThemelios;
import com.oliver.erydonthemelios.ModBlocks;
import com.oliver.erydonthemelios.client.migration.IdAliasSearchVocabulary;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;

/** Supplies generated old/current names and IDs to JEI search. */
@JeiPlugin
public final class ThemeliosJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_UID = new Identifier(ErydonThemelios.MOD_ID, "jei");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        for (Block block : ModBlocks.orderedBlocks()) {
            ItemStack stack = new ItemStack(block);
            List<String> aliases = IdAliasSearchVocabulary.terms(stack);
            if (!aliases.isEmpty()) {
                registration.addAliases(VanillaTypes.ITEM_STACK, stack, aliases);
            }
        }
    }
}
