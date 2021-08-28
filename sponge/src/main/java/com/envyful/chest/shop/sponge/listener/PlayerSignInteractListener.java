package com.envyful.chest.shop.sponge.listener;

import com.envyful.api.concurrency.UtilConcurrency;
import com.envyful.chest.shop.sponge.ChestShopSponge;
import com.envyful.chest.shop.sponge.util.UtilBlock;
import com.envyful.chest.shop.sponge.util.UtilItemStack;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.economy.IPixelmonBankAccount;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class PlayerSignInteractListener {

    private static final Text BOUGHT_ITEMS = Text.of("§e§l(!) §eItems were sold to your shop!");
    private static final Text SOLD_ITEMS = Text.of("§e§l(!) §eSuccessfully sold items!");
    private static final Text PURCHASED_ITEMS = Text.of("§e§l(!) §eSuccessfully purchased items!");
    private static final Text INSUFFICIENT_PURCHASER_FUNDS = Text.of("§c§l(!) §cYou have insufficient funds!");
    private static final Text INSUFFICIENT_ITEMS = Text.of("§c§l(!) §cThere is not enough items to purchase from this shop!");
    private static final Text YOU_HAVE_INSUFFICIENT_ITEMS = Text.of("§c§l(!) §cYou don't have enough items to sell to this shop!");
    private static final Text INSUFFICIENT_DEALER_FUNDS = Text.of("§c§l(!) §cThey have insufficient funds!");
    private static final Text INSUFFICIENT_INVENTORY_SPACE = Text.of("§c§l(!) §cYou don't have enough inventory space!");
    private static final Text INSUFFICIENT_CHEST_SPACE = Text.of("§c§l(!) §cThe chest is full!");

    private final ChestShopSponge mod;

    public PlayerSignInteractListener(ChestShopSponge mod) {
        this.mod = mod;
    }

    @Listener
    public void onPlayerInteractBlock(InteractBlockEvent event, @Root Player player) {
        UtilConcurrency.runAsync(() -> {
            BlockSnapshot targetBlock = event.getTargetBlock();

            if (targetBlock.getState().getType() != BlockTypes.WALL_SIGN) {
                return;
            }

            String ownerString = this.getOwnerUuid(targetBlock.getLocation().get(), (EntityPlayerMP) player);

            if (ownerString.isEmpty()) {
                return;
            }

            UUID owner = UUID.fromString(ownerString);

            if (owner.equals(player.getUniqueId())) {
                return;
            }

            Optional<? extends IPixelmonBankAccount> bankAccount = Pixelmon.moneyManager.getBankAccount(owner);

            if (!bankAccount.isPresent()) {
                return;
            }

            EntityPlayerMP forgePlayer = (EntityPlayerMP) player;

            IPixelmonBankAccount ownerBank = bankAccount.get();
            IPixelmonBankAccount interacterBank = Pixelmon.moneyManager.getBankAccount(player.getUniqueId()).get();
            ItemStack item = this.getSignItem(forgePlayer, targetBlock.getLocation().get());
            int signAmount = this.getSignAmount(forgePlayer, targetBlock.getLocation().get());
            double transactionWorth = this.getSignWorthPer(forgePlayer, targetBlock.getLocation().get());
            boolean buySign = this.getSignType(forgePlayer, targetBlock.getLocation().get());
            Chest chest = (Chest) UtilBlock.getPlacedOn(targetBlock.getLocation().get(), BlockTypes.CHEST).getLocation().getTileEntity().get();
            Player ownerPlayer = Sponge.getServer().getPlayer(owner).orElse(null);
            Inventory chestInventory = chest.getInventory();

            if (chest.getDoubleChestInventory().isPresent()) {
                chestInventory = chest.getDoubleChestInventory().get();
            }

            if (buySign) {
                if (transactionWorth > interacterBank.getMoney()) {
                    player.sendMessage(INSUFFICIENT_PURCHASER_FUNDS);
                    return;
                }

                Optional<ItemStack> poll = chestInventory
                        .query(QueryOperationTypes.ITEM_STACK_CUSTOM.of(itemStack -> this.matches(item, itemStack)))
                        .poll(signAmount);

                if (!poll.isPresent() || poll.get().getQuantity() < signAmount) {
                    player.sendMessage(INSUFFICIENT_ITEMS);
                    if (poll.isPresent()) {
                        chestInventory.offer(poll.get());
                    }
                    return;
                }

                InventoryTransactionResult offer = player.getInventory().offer(poll.get());

                if (offer.getType() == InventoryTransactionResult.Type.FAILURE) {
                    player.sendMessage(INSUFFICIENT_INVENTORY_SPACE);
                    chestInventory.offer(poll.get());
                    return;
                }

                if (ownerPlayer != null) {
                    ownerPlayer.sendMessage(SOLD_ITEMS);
                }

                player.sendMessage(PURCHASED_ITEMS);
                interacterBank.changeMoney((int) -transactionWorth);
                ownerBank.changeMoney((int) transactionWorth);
            } else {
                if (transactionWorth > ownerBank.getMoney()) {
                    player.sendMessage(INSUFFICIENT_DEALER_FUNDS);
                    return;
                }

                Optional<ItemStack> poll = player.getInventory()
                        .query(QueryOperationTypes.ITEM_STACK_CUSTOM.of(itemStack -> this.matches(item, itemStack)))
                        .poll(signAmount);

                if (!poll.isPresent() || poll.get().getQuantity() < signAmount) {
                    player.sendMessage(YOU_HAVE_INSUFFICIENT_ITEMS);

                    if (poll.isPresent()) {
                        player.getInventory().offer(poll.get());
                    }
                    return;
                }

                InventoryTransactionResult offer = chestInventory.offer(poll.get());

                if (offer.getType() == InventoryTransactionResult.Type.FAILURE) {
                    player.sendMessage(INSUFFICIENT_CHEST_SPACE);
                    player.getInventory().offer(poll.get());
                    return;
                }

                if (ownerPlayer != null) {
                    ownerPlayer.sendMessage(BOUGHT_ITEMS);
                }

                player.sendMessage(SOLD_ITEMS);
                interacterBank.changeMoney((int) transactionWorth);
                ownerBank.changeMoney((int) -transactionWorth);
            }
        });
    }

    private boolean matches(ItemStack one, ItemStack two) {
        net.minecraft.item.ItemStack nmsOne = ItemStackUtil.toNative(one);
        net.minecraft.item.ItemStack nmsTwo = ItemStackUtil.toNative(two);

        return Objects.equals(nmsOne.getItem(), nmsTwo.getItem())
                && Objects.equals(nmsOne.getItemDamage(), nmsTwo.getItemDamage());
    }

    private String getOwnerUuid(Location<World> location, EntityPlayerMP player) {
        net.minecraft.tileentity.TileEntity tileEntity = player.getEntityWorld()
                .getTileEntity(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

        if (tileEntity == null) {
            return "";
        }

        return tileEntity.getTileData().getString(PlayerSignPlaceListener.SHOP_NBT);
    }

    private ItemStack getSignItem(EntityPlayerMP player, Location<World> location) {
        net.minecraft.tileentity.TileEntity tileEntity = player.getEntityWorld()
                .getTileEntity(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

        if (tileEntity == null) {
            return null;
        }

        net.minecraft.item.ItemStack itemStack = UtilItemStack.fromString(tileEntity.getTileData().getString(PlayerSignPlaceListener.SHOP_ITEM_NBT));

        return ItemStackUtil.fromNative(itemStack);
    }

    private int getSignAmount(EntityPlayerMP player, Location<World> location) {
        net.minecraft.tileentity.TileEntity tileEntity = player.getEntityWorld()
                .getTileEntity(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

        if (tileEntity == null) {
            return 0;
        }

        return tileEntity.getTileData().getInteger(PlayerSignPlaceListener.SHOP_AMOUNT_NBT);
    }

    private double getSignWorthPer(EntityPlayerMP player, Location<World> location) {
        net.minecraft.tileentity.TileEntity tileEntity = player.getEntityWorld()
                .getTileEntity(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

        if (tileEntity == null) {
            return 0.0;
        }

        return tileEntity.getTileData().getDouble(PlayerSignPlaceListener.SHOP_PRICE_NBT);
    }

    public boolean getSignType(EntityPlayerMP player, Location<World> location) {
        net.minecraft.tileentity.TileEntity tileEntity = player.getEntityWorld()
                .getTileEntity(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

        if (tileEntity == null) {
            return false;
        }

        return tileEntity.getTileData().getBoolean(PlayerSignPlaceListener.SHOP_TYPE_NBT);
    }

    private TileEntityChest getChest(Location<World> location, EntityPlayerMP player) {
        net.minecraft.tileentity.TileEntity tileEntity = player.getEntityWorld()
                .getTileEntity(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

        return (TileEntityChest) tileEntity;
    }
}
