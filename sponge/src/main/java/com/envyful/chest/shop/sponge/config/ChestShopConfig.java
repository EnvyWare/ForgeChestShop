package com.envyful.chest.shop.sponge.config;

import com.envyful.api.config.data.ConfigPath;
import com.envyful.api.config.yaml.AbstractYamlConfig;
import com.google.common.collect.Lists;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.List;

@ConfigPath("config/ForgeChestShop/config.yml")
@ConfigSerializable
public class ChestShopConfig extends AbstractYamlConfig {

    private List<String> signLines = Lists.newArrayList(
            "%player%'s Chest Shop",
            "%signtype% %amount% for $%price%",
            "%itemname_lineone%",
            "%itemname_linetwo%"
    );

    public ChestShopConfig() {
        super();
    }

    public List<String> getSignLines() {
        return this.signLines;
    }
}
