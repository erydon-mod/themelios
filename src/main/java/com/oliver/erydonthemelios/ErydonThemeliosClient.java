package com.oliver.erydonthemelios;

import com.oliver.erydonthemelios.client.CollectionResourcePackNotice;
import com.oliver.erydonthemelios.client.model.SynapheiaModelLoadingPlugin;
import net.fabricmc.api.ClientModInitializer;

public final class ErydonThemeliosClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CollectionResourcePackNotice.register();
        SynapheiaModelLoadingPlugin.register();
        ErydonThemelios.LOGGER.info("[{}] Client ready", ErydonThemelios.MOD_ID);
    }
}
