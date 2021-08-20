package com.envyful.chest.shop.sponge;

import com.envyful.api.config.yaml.YamlConfigFactory;
import com.envyful.api.forge.concurrency.ForgeUpdateBuilder;
import com.envyful.chest.shop.sponge.config.ChestShopConfig;
import com.envyful.chest.shop.sponge.listener.PlayerChestOpenListener;
import com.envyful.chest.shop.sponge.listener.PlayerSignBreakListener;
import com.envyful.chest.shop.sponge.listener.PlayerSignInteractListener;
import com.envyful.chest.shop.sponge.listener.PlayerSignPlaceListener;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;

import java.io.IOException;

@Plugin(
        id = "chestshop",
        name = "ChestShop Sponge",
        version = ChestShopSponge.VERSION,
        description = "Provides sign shops for sponge"
)
public class ChestShopSponge {

    protected static final String VERSION = "0.1.0";

    private static ChestShopSponge instance;

    private ChestShopConfig config;

    @Listener
    public void onInitialize(GameInitializationEvent event) {
        instance = this;

        this.loadConfig();

        Sponge.getEventManager().registerListeners(this, new PlayerChestOpenListener());
        Sponge.getEventManager().registerListeners(this, new PlayerSignBreakListener());
        Sponge.getEventManager().registerListeners(this, new PlayerSignInteractListener(this));
        Sponge.getEventManager().registerListeners(this, new PlayerSignPlaceListener(this));

        ForgeUpdateBuilder.instance()
                .name("ForgeChestShop")
                .requiredPermission("forgechestshop.update.notify")
                .owner("Pixelmon-Development")
                .repo("ForgeChestShop")
                .version(VERSION)
                .start();
    }

    private void loadConfig() {
        try {
            this.config = YamlConfigFactory.getInstance(ChestShopConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ChestShopConfig getConfig() {
        return this.config;
    }
}
