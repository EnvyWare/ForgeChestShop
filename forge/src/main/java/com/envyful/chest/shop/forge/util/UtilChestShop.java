package com.envyful.chest.shop.forge.util;

import com.envyful.api.type.UtilParse;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class UtilChestShop {

    public static final String SHOP_NBT = "CHEST_SHOP_OWNER";

    public static String getOwnerUuid(BlockPos location, EntityPlayerMP player) {
        TileEntity tileEntity = player.getEntityWorld().getTileEntity(location);

        if (tileEntity == null) {
            return "";
        }

        return tileEntity.getTileData().getString(SHOP_NBT);
    }

    public static ItemStack getSignItem(TileEntitySign signData) {
        ItemStack itemStack = UtilItemStack.fromString(signData.signText[3].getUnformattedText());

        if (itemStack == null) {
            return null;
        }

        return itemStack;
    }

    public static int getSignAmount(TileEntitySign signData) {
        return UtilParse.parseInteger(signData.signText[1].getUnformattedText()).orElse(-1);
    }

    public static int getSignWorthPer(TileEntitySign signData) {
        return UtilParse.parseInteger(signData.signText[2].getUnformattedText().split(" ")[1]).orElse(-1);
    }

    public static boolean getSignType(TileEntitySign signData) {
        return signData.signText[2].getUnformattedText().split(" ")[0].equalsIgnoreCase("b");
    }

    public static TileEntityChest getChest(Vec3d location, EntityPlayerMP player) {
        net.minecraft.tileentity.TileEntity tileEntity = player.getEntityWorld()
                .getTileEntity(new BlockPos(location.x, location.y, location.z));

        return (TileEntityChest) tileEntity;
    }
}
