package com.oliver.erydonthemelios.compat.rei;

import com.oliver.erydonthemelios.ErydonThemelios;
import com.oliver.erydonthemelios.ModBlocks;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.entry.CollapsibleEntryRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.subsets.SubsetsRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/** Optional catalogue registration only; no world, tick or rendering hooks. */
@SuppressWarnings("removal")
public final class ThemeliosReiClientPlugin implements REIClientPlugin {
    @Override
    public void registerEntries(EntryRegistry registry) {
        registry.removeEntryIf(entry -> entry.getValue() instanceof ItemStack stack
                && stack.getItem() instanceof BlockItem
                && ErydonThemelios.MOD_ID.equals(Registries.ITEM.getId(stack.getItem()).getNamespace()));
        registry.addEntries(Catalogue.ITEMS.stream()
                .<EntryStack<?>>map(EntryStacks::of)
                .toList());
    }

    @Override
    public void registerCollapsibleEntries(CollapsibleEntryRegistry registry) {
        for (ThemeliosReiGroup group : ThemeliosReiGroup.values()) {
            List<EntryStack<?>> entries = entries(group);
            if (entries.size() > 1) {
                registry.group(new Identifier(ErydonThemelios.MOD_ID, "rei_group_" + group.id()), Text.translatable(group.translationKey()), entries);
            }
        }
    }

    @Override
    public void registerSubsets(SubsetsRegistry registry) {
        for (ThemeliosReiGroup group : ThemeliosReiGroup.values()) {
            List<EntryStack<?>> entries = entries(group);
            if (!entries.isEmpty()) {
                registry.registerPathEntries(group.subsetPath(), entries);
            }
        }
    }

    private static List<EntryStack<?>> entries(ThemeliosReiGroup group) {
        return Catalogue.ITEMS.stream()
                .filter(item -> group.matches(Registries.ITEM.getId(item).getPath()))
                .<EntryStack<?>>map(EntryStacks::of)
                .toList();
    }

    // Registry items are fixed after startup. Reuse the creative ordering without
    // retaining REI stacks or resource-dependent render data across reloads.
    private static final class Catalogue {
        private static final List<Item> ITEMS = ModBlocks.orderedBlocks().stream().map(Block::asItem).toList();
    }
}
