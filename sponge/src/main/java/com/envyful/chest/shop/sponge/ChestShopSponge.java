package com.envyful.chest.shop.sponge;

import com.envyful.api.forge.concurrency.ForgeUpdateBuilder;
import com.envyful.chest.shop.sponge.listener.PlayerChestOpenListener;
import com.envyful.chest.shop.sponge.listener.PlayerSignBreakListener;
import com.envyful.chest.shop.sponge.listener.PlayerSignInteractListener;
import com.envyful.chest.shop.sponge.listener.PlayerSignPlaceListener;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

@Plugin(
        id = "chestshop",
        name = "ChestShop Sponge",
        version = ChestShopSponge.VERSION,
        description = "Provides sign shops for sponge"
)
public class ChestShopSponge {

    protected static final String VERSION = "0.1.0";

    private static ChestShopSponge instance;

    @Inject private Logger logger;
    @Inject private Game game;
    @Inject private PluginContainer container;


    @Listener
    public void onInitialize(GameInitializationEvent event) {
        instance = this;

        Sponge.getEventManager().registerListeners(this, new PlayerChestOpenListener());
        Sponge.getEventManager().registerListeners(this, new PlayerSignBreakListener());
        Sponge.getEventManager().registerListeners(this, new PlayerSignInteractListener(this));
        Sponge.getEventManager().registerListeners(this, new PlayerSignPlaceListener());

        ForgeUpdateBuilder.instance()
                .name("ForgeChestShop")
                .requiredPermission("forgechestshop.update.notify")
                .owner("Pixelmon-Development")
                .repo("ForgeChestShop")
                .version(VERSION)
                .start();
    }

    public Logger getLogger() {
        return this.logger;
    }

    public Game getGame() {
        return this.game;
    }

    public PluginContainer getContainer() {
        return this.container;
    }
}
