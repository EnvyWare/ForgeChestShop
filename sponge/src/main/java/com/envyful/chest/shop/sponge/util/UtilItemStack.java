package com.envyful.chest.shop.sponge.util;

import com.envyful.api.type.UtilParse;
import com.google.common.collect.Lists;
import com.pixelmonmod.pixelmon.items.PixelmonItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.List;
import java.util.Optional;

public class UtilItemStack {

    public static List<String> getLore(ItemStack itemStack) {
        List<String> lore = Lists.newArrayList();

        if (itemStack == null) {
            return lore;
        }

        NBTTagList currentLore = itemStack.getOrCreateSubCompound("display").getTagList("Lore", 8);

        for (NBTBase nbtBase : currentLore) {
            if (nbtBase instanceof NBTTagString) {
                lore.add(((NBTTagString) nbtBase).getString());
            }
        }

        return lore;
    }

    public static ItemStack fromString(String args) {
        Item item = getItem(args);

        if (item == null) {
            return null;
        }

        if (args.contains(":")) {
            String[] arguments = args.split(":");
            Optional<Integer> data = UtilParse.parseInteger(arguments[arguments.length - 1]);

            if (data.isPresent()) {

                return new ItemStack(item, 1, data.get());
            }
        }

        return new ItemStack(item);
    }

    private static Item getItem(String itemDescription) {
        String[] args = itemDescription.split(":");
        String type;

        if (args.length == 3) {
            type = args[1];
        } else if (args.length == 2) {
            if (itemDescription.startsWith("minecraft:") || itemDescription.startsWith("pixelmon:")) {
                type = args[1];
            } else {
                type = args[0];
            }
        } else {
            type = args[0];
        }

        if (itemDescription.startsWith("pixelmon:")) {
            return PixelmonItem.getByNameOrId("pixelmon:" + type);
        } else if (itemDescription.startsWith("minecraft:")) {
            return Item.getByNameOrId("minecraft:" + type);
        }

        Item item = Item.getByNameOrId(type);

        if (item == null) {
            item = PixelmonItem.getByNameOrId(type);
        }

        return item;
    }
}
