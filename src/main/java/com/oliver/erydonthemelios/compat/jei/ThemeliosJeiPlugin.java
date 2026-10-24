package com.oliver.erydonthemelios.compat.jei;

import com.oliver.erydonthemelios.ErydonThemelios;
import com.oliver.erydonthemelios.ModBlocks;
import com.oliver.erydonthemelios.client.migration.IdAliasSearchVocabulary;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/** Supplies shared search aliases and shape guidance through JEI's information pages. */
@JeiPlugin
public final class ThemeliosJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_UID = Identifier.of(ErydonThemelios.MOD_ID, "jei");

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

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ItemStack> orderedStacks = ModBlocks.orderedBlocks().stream().map(ItemStack::new).toList();
        for (ThemeliosJeiInfo page : ThemeliosJeiInfo.values()) {
            List<ItemStack> stacks = orderedStacks.stream()
                    .filter(stack -> page.matches(Registries.ITEM.getId(stack.getItem()).getPath()))
                    .toList();
            if (!stacks.isEmpty()) {
                registration.addItemStackInfo(stacks, Text.translatable(page.translationKey()));
            }
        }
    }
}
