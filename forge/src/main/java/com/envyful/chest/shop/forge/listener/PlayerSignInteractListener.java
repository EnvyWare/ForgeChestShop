package com.envyful.chest.shop.forge.listener;

import com.envyful.api.concurrency.UtilConcurrency;
import com.envyful.chest.shop.forge.ChestShopForge;
import com.envyful.chest.shop.forge.util.UtilChestShop;
import com.mc.blaze.core.module.modules.chestshop.ChestShopModule;
import com.mc.blaze.core.utils.concurrency.UtilConcurrency;
import com.mc.blaze.core.utils.item.UtilItemStack;
import com.mc.blaze.core.utils.math.UtilParse;
import com.mc.blaze.core.utils.world.UtilBlock;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.economy.IPixelmonBankAccount;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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

    private static final String BOUGHT_ITEMS = "§e§l(!) §eItems were sold to your shop!";
    private static final String SOLD_ITEMS = "§e§l(!) §eSuccessfully sold items!";
    private static final String PURCHASED_ITEMS = "§e§l(!) §eSuccessfully purchased items!";
    private static final String INSUFFICIENT_PURCHASER_FUNDS = "§c§l(!) §cYou have insufficient funds!";
    private static final String INSUFFICIENT_ITEMS = "§c§l(!) §cThere is not enough items to purchase from this shop!";
    private static final String YOU_HAVE_INSUFFICIENT_ITEMS = "§c§l(!) §cYou don't have enough items to sell to this shop!";
    private static final String INSUFFICIENT_DEALER_FUNDS = "§c§l(!) §cThey have insufficient funds!";
    private static final String INSUFFICIENT_INVENTORY_SPACE = "§c§l(!) §cYou don't have enough inventory space!";
    private static final String INSUFFICIENT_CHEST_SPACE = "§c§l(!) §cThe chest is full!";

    private final ChestShopForge mod;

    public PlayerSignInteractListener(ChestShopForge mod) {
        this.mod = mod;
    }

    @SubscribeEvent
    public void onPlayerInteractBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        TileEntity clicked = event.getEntityPlayer().getEntityWorld().getTileEntity(pos);

        if (!(clicked instanceof TileEntitySign)) {
            return;
        }

        TileEntitySign sign = (TileEntitySign) clicked;

        UtilConcurrency.runAsync(() -> {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
            String ownerString = UtilChestShop.getOwnerUuid(pos, player);

            if (ownerString.isEmpty()) {
                return;
            }

            UUID owner = UUID.fromString(ownerString);

            if (owner.equals(player.getUniqueID())) {
                return;
            }

            Optional<? extends IPixelmonBankAccount> bankAccount = Pixelmon.moneyManager.getBankAccount(owner);

            if (!bankAccount.isPresent()) {
                return;
            }

            IPixelmonBankAccount ownerBank = bankAccount.get();
            IPixelmonBankAccount interacterBank = Pixelmon.moneyManager.getBankAccount(player.getUniqueID()).get();

            ItemStack item = UtilChestShop.getSignItem(sign);
            int signAmount = UtilChestShop.getSignAmount(sign);
            int transactionWorth = UtilChestShop.getSignWorthPer(sign);
            boolean buySign = UtilChestShop.getSignType(sign);
            EntityPlayerMP ownerPlayer = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(owner);

            Chest chest = (Chest) UtilBlock.getPlacedOn(targetBlock.getLocation().get(), BlockTypes.CHEST).getLocation().getTileEntity().get();
            Inventory chestInventory = chest.getInventory();

            if (chest.getDoubleChestInventory().isPresent()) {
                chestInventory = chest.getDoubleChestInventory().get();
            }


            if (buySign) {
                if (transactionWorth > interacterBank.getMoney()) {
                    player.sendMessage(new TextComponentString(INSUFFICIENT_PURCHASER_FUNDS));
                    return;
                }

                Optional<ItemStack> poll = chestInventory.query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY
                        .of(item)).poll(signAmount);

                if (!poll.isPresent() || poll.get().getQuantity() < signAmount) {
                    player.sendMessage(new TextComponentString(INSUFFICIENT_ITEMS));
                    if (poll.isPresent()) {
                        chestInventory.offer(poll.get());
                    }
                    return;
                }

                InventoryTransactionResult offer = player.getInventory().offer(poll.get());

                if (offer.getType() == InventoryTransactionResult.Type.FAILURE) {
                    player.sendMessage(new TextComponentString(INSUFFICIENT_INVENTORY_SPACE));
                    chestInventory.offer(poll.get());
                    return;
                }

                if (ownerPlayer != null) {
                    ownerPlayer.sendMessage(new TextComponentString(SOLD_ITEMS));
                }

                player.sendMessage(new TextComponentString(PURCHASED_ITEMS));
                interacterBank.changeMoney(-transactionWorth);
                ownerBank.changeMoney(transactionWorth);
            } else {
                if (transactionWorth > ownerBank.getMoney()) {
                    player.sendMessage(new TextComponentString(INSUFFICIENT_DEALER_FUNDS));
                    return;
                }

                Optional<ItemStack> poll = player.getInventory().query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY
                        .of(item)).poll(signAmount);

                if (!poll.isPresent() || poll.get().getQuantity() < signAmount) {
                    player.sendMessage(new TextComponentString(YOU_HAVE_INSUFFICIENT_ITEMS));

                    if (poll.isPresent()) {
                        player.getInventory().offer(poll.get());
                    }
                    return;
                }

                InventoryTransactionResult offer = chestInventory.offer(poll.get());

                if (offer.getType() == InventoryTransactionResult.Type.FAILURE) {
                    player.sendMessage(new TextComponentString(INSUFFICIENT_CHEST_SPACE));
                    return;
                }

                if (ownerPlayer != null) {
                    ownerPlayer.sendMessage(new TextComponentString(BOUGHT_ITEMS));
                }

                player.sendMessage(new TextComponentString(SOLD_ITEMS));
                interacterBank.changeMoney(transactionWorth);
                ownerBank.changeMoney(-transactionWorth);
            }
        });
    }
}
