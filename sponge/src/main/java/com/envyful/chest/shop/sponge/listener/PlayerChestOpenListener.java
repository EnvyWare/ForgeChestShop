package com.envyful.chest.shop.sponge.listener;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.UUID;

public class PlayerChestOpenListener {

    private static final String ADMIN_PERMISSION = "chestshop.admin";

    private static final Text CANNOT_OPEN = Text.of("§c§l(!) §cYou cannot open a chest with a sign shop that doesn't belong to you!");

    @Listener
    public void onChestOpen(InteractBlockEvent event, @Root Player player) {
        BlockSnapshot shopData = event.getTargetBlock();

        if (shopData.getState().getType() != BlockTypes.CHEST) {
            return;
        }

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
            player.sendMessage(CANNOT_OPEN);
        }
    }
}
