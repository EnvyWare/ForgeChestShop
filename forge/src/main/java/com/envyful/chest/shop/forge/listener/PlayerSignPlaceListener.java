package com.envyful.chest.shop.forge.listener;

import com.envyful.api.type.UtilParse;
import com.envyful.chest.shop.forge.util.UtilChestShop;
import com.envyful.chest.shop.forge.util.UtilItemStack;
import com.google.common.collect.Sets;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Set;

public class PlayerSignPlaceListener {

    private static final Set<String> SIGN_TYPES = Sets.newHashSet("b", "s");

    private static final String INVALID_TYPE = "§c§l(!) §cThat item type doesn't exist!";
    private static final String INVALID_PRICE = "§c§l(!) §cPrice must be valid!";
    private static final String INVALID_AMOUNT = "§c§l(!) §cAmount must be valid!";
    private static final String SUCCESSFUL_CREATION = "§e§l(!) §eSuccessfully created a shop sign!";

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof EntityPlayerMP)) {
            return;
        }

        IBlockState placedBlock = event.getPlacedBlock();

        if (!(placedBlock instanceof BlockWallSign)) {
            return;
        }

        TileEntity entity = event.getEntity().world.getTileEntity(event.getPos());

        if (!(entity instanceof TileEntitySign)) {
            return;
        }

        TileEntitySign sign = (TileEntitySign) entity;

        if (!this.isShopSign(sign) || sign.signText.length != 4) {
            return;
        }

        TileEntity tileEntity = event.getEntity().world.getTileEntity(
                event.getPos().add(event.getPlacedAgainst().getValue(BlockHorizontal.FACING).getOpposite().getDirectionVec()));

        if (!(tileEntity instanceof TileEntityChest)) {
            return;
        }

        int amount = UtilParse.parseInteger(sign.signText[1].getUnformattedText()).orElse(-1);
        EntityPlayerMP player = (EntityPlayerMP) event.getEntity();

        if (amount <= 0) {
            event.setCanceled(true);
            player.sendMessage(new TextComponentString(INVALID_AMOUNT));
            return;
        }

        String[] thirdLineArgs = sign.signText[2].getUnformattedText().split(" ");

        if (thirdLineArgs.length != 2 || !SIGN_TYPES.contains(thirdLineArgs[0].toLowerCase())) {
            return;
        }

        double price = UtilParse.parseDouble(thirdLineArgs[1]).orElse(-1.0);

        if (price <= 0) {
            event.setCanceled(true);
            player.sendMessage(new TextComponentString(INVALID_PRICE));
            return;
        }

        ItemStack type = UtilItemStack.fromString(sign.signText[3].getUnformattedText());

        if (type == null) {
            event.setCanceled(true);
            player.sendMessage(new TextComponentString(INVALID_TYPE));
            return;
        }

        player.sendMessage(new TextComponentString(SUCCESSFUL_CREATION));
        sign.signText[0] = new TextComponentString("§f" + player.getName() + "'s Shop");


        this.setOwner(event.getPos(), (EntityPlayerMP) player);
        this.setOwner(tileEntity.getPos(), (EntityPlayerMP) player);
    }

    private boolean isShopSign(TileEntitySign sign) {
        return sign.signText[0].getUnformattedText().equals("[shop]");
    }

    private void setOwner(BlockPos pos, EntityPlayerMP player) {
        net.minecraft.tileentity.TileEntity tileEntity = player.getEntityWorld()
                .getTileEntity(pos);

        if (tileEntity == null) {
            return;
        }

        tileEntity.getTileData().setString(UtilChestShop.SHOP_NBT, player.getUniqueID().toString());
    }
}
