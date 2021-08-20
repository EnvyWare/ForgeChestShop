package com.envyful.chest.shop.sponge.listener;

import com.envyful.api.type.UtilParse;
import com.envyful.chest.shop.sponge.util.UtilBlock;
import com.envyful.chest.shop.sponge.util.UtilItemStack;
import com.google.common.collect.Sets;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Set;

public class PlayerSignPlaceListener {

    public static final DataQuery SHOP_QUERY = DataQuery.of("chestshop", "owner");
    public static final String SHOP_NBT = "CHEST_SHOP_OWNER";
    public static final String SHOP_PRICE_NBT = "CHEST_SHOP_PRICE";
    public static final String SHOP_AMOUNT_NBT = "CHEST_SHOP_AMOUNT";
    public static final String SHOP_ITEM_NBT = "CHEST_SHOP_ITEM";
    public static final String SHOP_TYPE_NBT = "CHEST_SHOP_TYPE";

    private static final Set<String> SIGN_TYPES = Sets.newHashSet("b", "s");

    private static final Text INVALID_TYPE = Text.of("§c§l(!) §cThat item type doesn't exist!");
    private static final Text INVALID_PRICE = Text.of("§c§l(!) §cPrice must be valid!");
    private static final Text INVALID_AMOUNT = Text.of("§c§l(!) §cAmount must be valid!");
    private static final Text SUCCESSFUL_CREATION = Text.of("§e§l(!) §eSuccessfully created a shop sign!");

    @Listener
    public void onSignEdit(ChangeSignEvent event, @Root Player player) {
        if (!this.isShopSign(event) || event.getText().lines().size() != 4) {
            return;
        }

        TileEntity block = UtilBlock.getPlacedOn(event.getTargetTile().getLocation(), BlockTypes.CHEST);

        if (block == null) {
            return;
        }

        int amount = UtilParse.parseInteger(event.getText().get(1).get().toPlainSingle()).orElse(-1);

        if (amount <= 0) {
            event.setCancelled(true);
            player.sendMessage(INVALID_AMOUNT);
            return;
        }

        String[] thirdLineArgs = event.getText().get(2).get().toPlainSingle().split(" ");

        if (thirdLineArgs.length != 2 || !SIGN_TYPES.contains(thirdLineArgs[0].toLowerCase())) {
            return;
        }

        double price = UtilParse.parseDouble(thirdLineArgs[1]).orElse(-1.0);

        if (price <= 0) {
            event.setCancelled(true);
            player.sendMessage(INVALID_PRICE);
            return;
        }

        ItemStack type = UtilItemStack.fromString(event.getText().get(3).get().toPlainSingle());

        if (type == null) {
            event.setCancelled(true);
            player.sendMessage(INVALID_TYPE);
            return;
        }

        boolean buySign = thirdLineArgs[0].equalsIgnoreCase("b");

        player.sendMessage(SUCCESSFUL_CREATION);
        event.getText().setElement(0, Text.of(player.getName() + "'s Shop"));
        event.getText().setElement(1, Text.of(
                (buySign ? "Buy " : "Sell ") + amount
                        + " for $" + price)
        );

        String name = type.getDisplayName();

        if (name.length() > 15) {
            event.getText().setElement(2, Text.of(name.substring(0, 16)));
            event.getText().setElement(3, Text.of(name.substring(16)));
        } else {
            event.getText().setElement(2, Text.of(name));
            event.getText().setElement(3, Text.of(""));
        }

        this.setNBT(block.getLocation(), buySign, type, price, amount, (EntityPlayerMP) player);
        this.setNBT(event.getTargetTile().getLocation(), buySign, type, price, amount, (EntityPlayerMP) player);
    }

    private boolean isShopSign(ChangeSignEvent event) {
        return event.getText().get(0).orElse(Text.EMPTY).toPlainSingle().equals("[shop]");
    }

    private void setNBT(Location<World> location, boolean buySign, ItemStack itemStack,
                        double price, int amount, EntityPlayerMP player) {
        net.minecraft.tileentity.TileEntity tileEntity = player.getEntityWorld()
                .getTileEntity(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

        if (tileEntity == null) {
            return;
        }

        tileEntity.getTileData().setString(SHOP_NBT, player.getUniqueID().toString());
        tileEntity.getTileData().setDouble(SHOP_PRICE_NBT, price);
        tileEntity.getTileData().setInteger(SHOP_AMOUNT_NBT, amount);
        tileEntity.getTileData().setString(SHOP_ITEM_NBT, itemStack.getItem().getRegistryName() + ":" + itemStack.getItemDamage());
        tileEntity.getTileData().setBoolean(SHOP_TYPE_NBT, buySign);
    }
}
