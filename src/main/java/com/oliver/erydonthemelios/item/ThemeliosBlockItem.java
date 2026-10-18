package com.oliver.erydonthemelios.item;

import com.oliver.erydonthemelios.ErydonThemelios;
import com.oliver.erydonthemelios.client.migration.IdAliasSearchVocabulary;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Adds generated legacy/current vocabulary to creative-search indexing. */
public final class ThemeliosBlockItem extends BlockItem {
    public ThemeliosBlockItem(Block block, Item.Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        if (world != null || !context.isCreative()) {
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
