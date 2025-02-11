/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.items;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.pocket.apis.PocketAPI;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import dan200.computercraft.shared.pocket.inventory.PocketComputerMenuProvider;
import dan200.computercraft.shared.util.IDAssigner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemPocketComputer extends Item implements IComputerItem, IMedia, IColouredItem {
    private static final String NBT_UPGRADE = "Upgrade";
    private static final String NBT_UPGRADE_INFO = "UpgradeInfo";
    public static final String NBT_LIGHT = "Light";
    private static final String NBT_ON = "On";

    private static final String NBT_INSTANCE = "Instanceid";
    private static final String NBT_SESSION = "SessionId";

    private final ComputerFamily family;

    public ItemPocketComputer(Properties settings, ComputerFamily family) {
        super(settings);
        this.family = family;
    }

    public ItemStack create(int id, String label, int colour, IPocketUpgrade upgrade) {
        var result = new ItemStack(this);
        if (id >= 0) result.getOrCreateTag().putInt(NBT_ID, id);
        if (label != null) result.setHoverName(Component.literal(label));
        if (upgrade != null) result.getOrCreateTag().putString(NBT_UPGRADE, upgrade.getUpgradeID().toString());
        if (colour != -1) result.getOrCreateTag().putInt(NBT_COLOUR, colour);
        return result;
    }

    @Override
    public void fillItemCategory(@Nonnull CreativeModeTab group, @Nonnull NonNullList<ItemStack> stacks) {
        if (!allowedIn(group)) return;
        stacks.add(create(-1, null, -1, null));
        PocketUpgrades.getVanillaUpgrades().map(x -> create(-1, null, -1, x)).forEach(stacks::add);
    }

    private boolean tick(@Nonnull ItemStack stack, @Nonnull Level world, @Nonnull Entity entity, @Nonnull PocketServerComputer computer) {
        var upgrade = getUpgrade(stack);

        computer.setLevel((ServerLevel) world);
        computer.updateValues(entity, stack, upgrade);

        var changed = false;

        // Sync ID
        var id = computer.getID();
        if (id != getComputerID(stack)) {
            changed = true;
            setComputerID(stack, id);
        }

        // Sync label
        var label = computer.getLabel();
        if (!Objects.equal(label, getLabel(stack))) {
            changed = true;
            setLabel(stack, label);
        }

        var on = computer.isOn();
        if (on != isMarkedOn(stack)) {
            changed = true;
            stack.getOrCreateTag().putBoolean(NBT_ON, on);
        }

        // Update pocket upgrade
        if (upgrade != null) upgrade.update(computer, computer.getPeripheral(ComputerSide.BACK));

        return changed;
    }

    @Override
    public void inventoryTick(@Nonnull ItemStack stack, Level world, @Nonnull Entity entity, int slotNum, boolean selected) {
        if (world.isClientSide) return;
        Container inventory = entity instanceof Player player ? player.getInventory() : null;
        var computer = createServerComputer((ServerLevel) world, entity, inventory, stack);
        computer.keepAlive();

        var changed = tick(stack, world, entity, computer);
        if (changed && inventory != null) inventory.setChanged();
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level.isClientSide) return false;

        var computer = getServerComputer(entity.level.getServer(), stack);
        if (computer != null && tick(stack, entity.level, entity, computer)) entity.setItem(stack.copy());
        return false;
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, @Nonnull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            var computer = createServerComputer((ServerLevel) world, player, player.getInventory(), stack);
            computer.turnOn();

            var stop = false;
            var upgrade = getUpgrade(stack);
            if (upgrade != null) {
                computer.updateValues(player, stack, upgrade);
                stop = upgrade.onRightClick(world, computer, computer.getPeripheral(ComputerSide.BACK));
            }

            if (!stop) {
                var isTypingOnly = hand == InteractionHand.OFF_HAND;
                new ComputerContainerData(computer, stack).open(player, new PocketComputerMenuProvider(computer, stack, this, hand, isTypingOnly));
            }
        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack stack) {
        var baseString = getDescriptionId(stack);
        var upgrade = getUpgrade(stack);
        if (upgrade != null) {
            return Component.translatable(baseString + ".upgraded",
                Component.translatable(upgrade.getUnlocalisedAdjective())
            );
        } else {
            return super.getName(stack);
        }
    }


    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level world, @Nonnull List<Component> list, TooltipFlag flag) {
        if (flag.isAdvanced() || getLabel(stack) == null) {
            var id = getComputerID(stack);
            if (id >= 0) {
                list.add(Component.translatable("gui.computercraft.tooltip.computer_id", id)
                    .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Nullable
    @Override
    public String getCreatorModId(ItemStack stack) {
        var upgrade = getUpgrade(stack);
        if (upgrade != null) {
            // If we're a non-vanilla, non-CC upgrade then return whichever mod this upgrade
            // belongs to.
            var mod = PocketUpgrades.instance().getOwner(upgrade);
            if (mod != null && !mod.equals(ComputerCraft.MOD_ID)) return mod;
        }

        return super.getCreatorModId(stack);
    }

    @Nonnull
    public PocketServerComputer createServerComputer(ServerLevel world, Entity entity, @Nullable Container inventory, @Nonnull ItemStack stack) {
        if (world.isClientSide) throw new IllegalStateException("Cannot call createServerComputer on the client");

        var sessionID = getSessionID(stack);

        var registry = ServerContext.get(world.getServer()).registry();
        var computer = (PocketServerComputer) registry.get(sessionID, getInstanceID(stack));
        if (computer == null) {
            var computerID = getComputerID(stack);
            if (computerID < 0) {
                computerID = ComputerCraftAPI.createUniqueNumberedSaveDir(world, IDAssigner.COMPUTER);
                setComputerID(stack, computerID);
            }

            computer = new PocketServerComputer(world, getComputerID(stack), getLabel(stack), getFamily());

            setInstanceID(stack, computer.register());
            setSessionID(stack, registry.getSessionID());

            computer.updateValues(entity, stack, getUpgrade(stack));
            computer.addAPI(new PocketAPI(computer));

            // Only turn on when initially creating the computer, rather than each tick.
            if (isMarkedOn(stack) && entity instanceof Player) computer.turnOn();

            if (inventory != null) inventory.setChanged();
        }
        computer.setLevel(world);
        return computer;
    }

    @Nullable
    public static PocketServerComputer getServerComputer(MinecraftServer server, @Nonnull ItemStack stack) {
        return (PocketServerComputer) ServerContext.get(server).registry().get(getSessionID(stack), getInstanceID(stack));
    }

    // IComputerItem implementation

    private static void setComputerID(@Nonnull ItemStack stack, int computerID) {
        stack.getOrCreateTag().putInt(NBT_ID, computerID);
    }

    @Override
    public String getLabel(@Nonnull ItemStack stack) {
        return IComputerItem.super.getLabel(stack);
    }

    @Override
    public ComputerFamily getFamily() {
        return family;
    }

    @Override
    public ItemStack withFamily(@Nonnull ItemStack stack, @Nonnull ComputerFamily family) {
        return PocketComputerItemFactory.create(
            getComputerID(stack), getLabel(stack), getColour(stack),
            family, getUpgrade(stack)
        );
    }

    // IMedia

    @Override
    public boolean setLabel(@Nonnull ItemStack stack, String label) {
        if (label != null) {
            stack.setHoverName(Component.literal(label));
        } else {
            stack.resetHoverName();
        }
        return true;
    }

    @Override
    public IMount createDataMount(@Nonnull ItemStack stack, @Nonnull Level world) {
        var id = getComputerID(stack);
        if (id >= 0) {
            return ComputerCraftAPI.createSaveDirMount(world, "computer/" + id, ComputerCraft.computerSpaceLimit);
        }
        return null;
    }

    public static int getInstanceID(@Nonnull ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_INSTANCE) ? nbt.getInt(NBT_INSTANCE) : -1;
    }

    private static void setInstanceID(@Nonnull ItemStack stack, int instanceID) {
        stack.getOrCreateTag().putInt(NBT_INSTANCE, instanceID);
    }

    private static int getSessionID(@Nonnull ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_SESSION) ? nbt.getInt(NBT_SESSION) : -1;
    }

    private static void setSessionID(@Nonnull ItemStack stack, int sessionID) {
        stack.getOrCreateTag().putInt(NBT_SESSION, sessionID);
    }

    private static boolean isMarkedOn(@Nonnull ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.getBoolean(NBT_ON);
    }

    public static IPocketUpgrade getUpgrade(@Nonnull ItemStack stack) {
        var compound = stack.getTag();
        return compound != null && compound.contains(NBT_UPGRADE)
            ? PocketUpgrades.instance().get(compound.getString(NBT_UPGRADE)) : null;
    }

    public static void setUpgrade(@Nonnull ItemStack stack, IPocketUpgrade upgrade) {
        var compound = stack.getOrCreateTag();

        if (upgrade == null) {
            compound.remove(NBT_UPGRADE);
        } else {
            compound.putString(NBT_UPGRADE, upgrade.getUpgradeID().toString());
        }

        compound.remove(NBT_UPGRADE_INFO);
    }

    public static CompoundTag getUpgradeInfo(@Nonnull ItemStack stack) {
        return stack.getOrCreateTagElement(NBT_UPGRADE_INFO);
    }
}
