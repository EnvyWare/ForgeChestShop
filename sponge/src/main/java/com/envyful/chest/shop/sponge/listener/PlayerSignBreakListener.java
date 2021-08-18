package com.envyful.chest.shop.sponge.listener;

import com.envyful.chest.shop.sponge.util.UtilBlock;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.UUID;

public class PlayerSignBreakListener {

    private static final String ADMIN_PERMISSION = "chestshop.admin";

    private static final Text CANNOT_BREAK = Text.of("§c§l(!) §cYou can only break chest shop signs you own!");

    @Listener
    public void onBlockBreak(ChangeBlockEvent.Break event, @Root Player player) {
        BlockSnapshot shopData = event.getTransactions().get(0).getOriginal();
        Location<World> location = shopData.getLocation().get();
        net.minecraft.tileentity.TileEntity tileEntity = ((EntityPlayerMP) player).getEntityWorld()
                .getTileEntity(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

        if (tileEntity == null) {
            return;
        }

        String ownerUuid = tileEntity.getTileData().getString(PlayerSignPlaceListener.SHOP_NBT);

        if (ownerUuid.isEmpty()) {
            return;
        }

        UUID owner = UUID.fromString(ownerUuid);

        if (!player.getUniqueId().equals(owner) && !player.hasPermission(ADMIN_PERMISSION)) {
            event.setCancelled(true);
            player.sendMessage(CANNOT_BREAK);
        } else {
            TileEntity block = UtilBlock.getPlacedOn(location, BlockTypes.CHEST);

            this.removeOwner(block.getLocation(), (EntityPlayerMP) player);
        }
    }


    private void removeOwner(Location<World> location, EntityPlayerMP player) {
        net.minecraft.tileentity.TileEntity tileEntity = player.getEntityWorld()
                .getTileEntity(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

        if (tileEntity == null) {
            return;
        }

        tileEntity.getTileData().removeTag(PlayerSignPlaceListener.SHOP_NBT);
    }
}
