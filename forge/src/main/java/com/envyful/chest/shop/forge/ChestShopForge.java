package com.envyful.chest.shop.forge;

import com.envyful.api.forge.command.ForgeCommandFactory;
import com.envyful.api.forge.concurrency.ForgeUpdateBuilder;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.bstats.forge.Metrics;

import java.nio.file.Paths;

@Mod(
        modid = "chestshop",
        name = "ChestShops Forge",
        version = ChestShopForge.VERSION,
        acceptableRemoteVersions = "*"
)
public class ChestShopForge {

    protected static final String VERSION = "0.1.0";

    private static ChestShopForge instance;

    private ForgeCommandFactory commandFactory = new ForgeCommandFactory();

    @Mod.EventHandler
    public void onServerStarting(FMLPreInitializationEvent event) {
        instance = this;



        Metrics metrics = new Metrics(
                Loader.instance().activeModContainer(),
                event.getModLog(),
                Paths.get("config/"),
                12472
        );

        ForgeUpdateBuilder.instance()
                .name("ForgeChestShop")
                .requiredPermission("forgechestshop.update.notify")
                .owner("Pixelmon-Development")
                .repo("ForgeChestShop")
                .version(VERSION)
                .start();
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {

    }

    public static ChestShopForge getInstance() {
        return instance;
    }
}
