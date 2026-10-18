package com.oliver.erydonthemelios.compat.emi;

import com.oliver.erydonthemelios.ErydonThemelios;
import com.oliver.erydonthemelios.ModBlocks;
import com.oliver.erydonthemelios.client.migration.IdAliasSearchVocabulary;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

/** Supplies generated old/current names and IDs to EMI search. */
public final class ThemeliosEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        int termCount = 0;
        for (Block block : ModBlocks.orderedBlocks()) {
            ItemStack stack = new ItemStack(block);
            List<String> terms = IdAliasSearchVocabulary.terms(stack);
            if (terms.isEmpty()) continue;
            EmiStack ingredient = EmiStack.of(stack);
            for (String term : terms) {
                registry.addAlias(ingredient, Text.literal(term));
                termCount++;
            }
        }
        ErydonThemelios.LOGGER.info("[id-migration][emi] Registered {} Themelios search terms.", termCount);
    }
}
