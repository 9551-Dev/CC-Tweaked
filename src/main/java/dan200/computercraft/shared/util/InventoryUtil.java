/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class InventoryUtil {
    private InventoryUtil() {
    }
    // Methods for comparing things:

    public static boolean areItemsEqual(@Nonnull ItemStack a, @Nonnull ItemStack b) {
        return a == b || ItemStack.matches(a, b);
    }

    public static boolean areItemsStackable(@Nonnull ItemStack a, @Nonnull ItemStack b) {
        return a == b || ItemHandlerHelper.canItemStacksStack(a, b);
    }

    // Methods for finding inventories:

    @Nullable
    public static IItemHandler getInventory(@Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Direction side) {
        // Look for tile with inventory
        var tileEntity = world.getBlockEntity(pos);
        if (tileEntity != null) {
            var itemHandler = tileEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side);
            if (itemHandler.isPresent()) {
                return itemHandler.orElseThrow(NullPointerException::new);
            } else if (tileEntity instanceof WorldlyContainer) {
                return new SidedInvWrapper((WorldlyContainer) tileEntity, side);
            } else if (tileEntity instanceof Container) {
                return new InvWrapper((Container) tileEntity);
            }
        }

        var block = world.getBlockState(pos);
        if (block.getBlock() instanceof WorldlyContainerHolder) {
            var inventory = ((WorldlyContainerHolder) block.getBlock()).getContainer(block, world, pos);
            return new SidedInvWrapper(inventory, side);
        }

        // Look for entity with inventory
        var vecStart = new Vec3(
            pos.getX() + 0.5 + 0.6 * side.getStepX(),
            pos.getY() + 0.5 + 0.6 * side.getStepY(),
            pos.getZ() + 0.5 + 0.6 * side.getStepZ()
        );
        var dir = side.getOpposite();
        var vecDir = new Vec3(
            dir.getStepX(), dir.getStepY(), dir.getStepZ()
        );
        var hit = WorldUtil.rayTraceEntities(world, vecStart, vecDir, 1.1);
        if (hit != null) {
            var entity = hit.getKey();
            if (entity instanceof Container) {
                return new InvWrapper((Container) entity);
            }
        }
        return null;
    }

    // Methods for placing into inventories:

    @Nonnull
    public static ItemStack storeItems(@Nonnull ItemStack itemstack, IItemHandler inventory, int begin) {
        return storeItems(itemstack, inventory, 0, inventory.getSlots(), begin);
    }

    @Nonnull
    public static ItemStack storeItems(@Nonnull ItemStack itemstack, IItemHandler inventory) {
        return storeItems(itemstack, inventory, 0, inventory.getSlots(), 0);
    }

    @Nonnull
    public static ItemStack storeItems(@Nonnull ItemStack stack, IItemHandler inventory, int start, int range, int begin) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        // Inspect the slots in order and try to find empty or stackable slots
        var remainder = stack.copy();
        for (var i = 0; i < range; i++) {
            var slot = start + (i + begin - start) % range;
            if (remainder.isEmpty()) break;
            remainder = inventory.insertItem(slot, remainder, false);
        }
        return areItemsEqual(stack, remainder) ? stack : remainder;
    }

    // Methods for taking out of inventories

    @Nonnull
    public static ItemStack takeItems(int count, IItemHandler inventory) {
        return takeItems(count, inventory, 0, inventory.getSlots(), 0);
    }

    @Nonnull
    public static ItemStack takeItems(int count, IItemHandler inventory, int start, int range, int begin) {
        // Combine multiple stacks from inventory into one if necessary
        var partialStack = ItemStack.EMPTY;
        for (var i = 0; i < range; i++) {
            var slot = start + (i + begin - start) % range;

            // If we've extracted all items, return
            if (count <= 0) break;

            // If this doesn't slot, abort.
            var stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && (partialStack.isEmpty() || areItemsStackable(stack, partialStack))) {
                var extracted = inventory.extractItem(slot, count, false);
                if (!extracted.isEmpty()) {
                    if (partialStack.isEmpty()) {
                        // If we've extracted for this first time, then limit the count to the maximum stack size.
                        partialStack = extracted;
                        count = Math.min(count, extracted.getMaxStackSize());
                    } else {
                        partialStack.grow(extracted.getCount());
                    }

                    count -= extracted.getCount();
                }
            }

        }

        return partialStack;
    }
}
