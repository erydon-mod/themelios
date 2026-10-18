package com.oliver.erydonthemelios.item;

import com.oliver.erydonthemelios.ErydonThemelios;
import com.oliver.erydonthemelios.client.migration.IdAliasSearchVocabulary;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/** Adds generated legacy/current vocabulary to creative-search indexing. */
public final class ThemeliosBlockItem extends BlockItem {
    public ThemeliosBlockItem(Block block, Item.Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context,
                              List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        if (!type.isCreative()) {
            return;
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        if (itemId == null || !ErydonThemelios.MOD_ID.equals(itemId.getNamespace())) {
            return;
        }
        for (String term : IdAliasSearchVocabulary.terms(itemId)) {
            tooltip.add(Text.literal(term));
        }
    }
}
