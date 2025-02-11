/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;

public class TurtleSuckCommand implements ITurtleCommand {
    private final InteractDirection direction;
    private final int quantity;

    public TurtleSuckCommand(InteractDirection direction, int quantity) {
        this.direction = direction;
        this.quantity = quantity;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute(@Nonnull ITurtleAccess turtle) {
        // Sucking nothing is easy
        if (quantity == 0) {
            turtle.playAnimation(TurtleAnimation.WAIT);
            return TurtleCommandResult.success();
        }

        // Get world direction from direction
        var direction = this.direction.toWorldDir(turtle);

        // Get inventory for thing in front
        var world = turtle.getLevel();
        var turtlePosition = turtle.getPosition();
        var blockPosition = turtlePosition.relative(direction);
        var side = direction.getOpposite();

        var inventory = InventoryUtil.getInventory(world, blockPosition, side);

        if (inventory != null) {
            // Take from inventory of thing in front
            var stack = InventoryUtil.takeItems(quantity, inventory);
            if (stack.isEmpty()) return TurtleCommandResult.failure("No items to take");

            // Try to place into the turtle
            var remainder = InventoryUtil.storeItems(stack, turtle.getItemHandler(), turtle.getSelectedSlot());
            if (!remainder.isEmpty()) {
                // Put the remainder back in the inventory
                InventoryUtil.storeItems(remainder, inventory);
            }

            // Return true if we consumed anything
            if (remainder != stack) {
                turtle.playAnimation(TurtleAnimation.WAIT);
                return TurtleCommandResult.success();
            } else {
                return TurtleCommandResult.failure("No space for items");
            }
        } else {
            // Suck up loose items off the ground
            var aabb = new AABB(
                blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(),
                blockPosition.getX() + 1.0, blockPosition.getY() + 1.0, blockPosition.getZ() + 1.0
            );
            var list = world.getEntitiesOfClass(ItemEntity.class, aabb, EntitySelector.ENTITY_STILL_ALIVE);
            if (list.isEmpty()) return TurtleCommandResult.failure("No items to take");

            for (var entity : list) {
                // Suck up the item
                var stack = entity.getItem().copy();

                ItemStack storeStack;
                ItemStack leaveStack;
                if (stack.getCount() > quantity) {
                    storeStack = stack.split(quantity);
                    leaveStack = stack;
                } else {
                    storeStack = stack;
                    leaveStack = ItemStack.EMPTY;
                }

                var remainder = InventoryUtil.storeItems(storeStack, turtle.getItemHandler(), turtle.getSelectedSlot());

                if (remainder != storeStack) {
                    if (remainder.isEmpty() && leaveStack.isEmpty()) {
                        entity.discard();
                    } else if (remainder.isEmpty()) {
                        entity.setItem(leaveStack);
                    } else if (leaveStack.isEmpty()) {
                        entity.setItem(remainder);
                    } else {
                        leaveStack.grow(remainder.getCount());
                        entity.setItem(leaveStack);
                    }

                    // Play fx
                    world.globalLevelEvent(1000, turtlePosition, 0); // BLOCK_DISPENSER_DISPENSE
                    turtle.playAnimation(TurtleAnimation.WAIT);
                    return TurtleCommandResult.success();
                }
            }


            return TurtleCommandResult.failure("No space for items");
        }
    }
}
