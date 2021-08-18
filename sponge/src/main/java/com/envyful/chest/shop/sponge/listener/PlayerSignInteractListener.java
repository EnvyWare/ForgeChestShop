package com.envyful.chest.shop.sponge.listener;

import com.envyful.api.concurrency.UtilConcurrency;
import com.envyful.api.type.UtilParse;
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
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
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

            IPixelmonBankAccount ownerBank = bankAccount.get();
            IPixelmonBankAccount interacterBank = Pixelmon.moneyManager.getBankAccount(player.getUniqueId()).get();
            SignData signData = targetBlock.getLocation().get().getTileEntity().get().get(SignData.class).get();
            ItemStack item = this.getSignItem(signData);
            int signAmount = this.getSignAmount(signData);
            int transactionWorth = this.getSignWorthPer(signData);
            boolean buySign = this.getSignType(signData);
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

                Optional<ItemStack> poll = chestInventory.query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY
                        .of(item)).poll(signAmount);

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
                interacterBank.changeMoney(-transactionWorth);
                ownerBank.changeMoney(transactionWorth);
            } else {
                if (transactionWorth > ownerBank.getMoney()) {
                    player.sendMessage(INSUFFICIENT_DEALER_FUNDS);
                    return;
                }

                Optional<ItemStack> poll = player.getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY
                        .of(item)).poll(signAmount);

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
                interacterBank.changeMoney(transactionWorth);
                ownerBank.changeMoney(-transactionWorth);
            }
        });
    }

    private String getOwnerUuid(Location<World> location, EntityPlayerMP player) {
        net.minecraft.tileentity.TileEntity tileEntity = player.getEntityWorld()
                .getTileEntity(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

        if (tileEntity == null) {
            return "";
        }

        return tileEntity.getTileData().getString(PlayerSignPlaceListener.SHOP_NBT);
    }

    private ItemStack getSignItem(SignData signData) {
        net.minecraft.item.ItemStack itemStack = UtilItemStack.fromString(signData.get(3).get().toPlainSingle());

        if (itemStack == null) {
            return null;
        }

        return ItemStackUtil.fromNative(itemStack);
    }

    private int getSignAmount(SignData signData) {
        return UtilParse.parseInteger(signData.getListValue().get(1).toPlainSingle()).orElse(-1);
    }

    private int getSignWorthPer(SignData signData) {
        return UtilParse.parseInteger(signData.getListValue().get(2).toPlainSingle().split(" ")[1]).orElse(-1);
    }

    public boolean getSignType(SignData signData) {
        return signData.getListValue().get(2).toPlainSingle().split(" ")[0].equalsIgnoreCase("b");
    }

    private TileEntityChest getChest(Location<World> location, EntityPlayerMP player) {
        net.minecraft.tileentity.TileEntity tileEntity = player.getEntityWorld()
                .getTileEntity(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

        return (TileEntityChest) tileEntity;
    }
}
