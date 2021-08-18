package com.envyful.chest.shop.forge.util;

import com.envyful.api.forge.items.ItemBuilder;
import com.envyful.api.type.UtilParse;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Optional;

public class UtilItemStack {

    public static ItemStack fromString(String args) {
        Item item = getItem(args);

        if (item == null) {
            return null;
        }

        if (args.contains(":")) {
            String[] arguments = args.split(":");
            Optional<Integer> data = UtilParse.parseInteger(arguments[arguments.length - 1]);

            if (data.isPresent()) {
                return new ItemBuilder().type(item).damage(data.get()).build();
            }
        }

        return new ItemStack(item);
    }

    private static Item getItem(String itemDescription) {
        String[] args = itemDescription.split(":");
        String type;

        if (args.length == 3) {
            type = args[0] + args[1];
        } else if (args.length == 2) {
            try {
                Integer.parseInt(args[1]);
                type = args[0];
            } catch (NumberFormatException e) {
                type = args[1];
            }
        } else {
            type = args[0];
        }

        return Item.getByNameOrId(type);
    }
}
