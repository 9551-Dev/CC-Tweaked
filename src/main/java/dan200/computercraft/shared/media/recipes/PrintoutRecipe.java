/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.media.recipes;

import dan200.computercraft.shared.media.items.ItemPrintout;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;

import javax.annotation.Nonnull;

public final class PrintoutRecipe extends CustomRecipe {
    private PrintoutRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean canCraftInDimensions(int x, int y) {
        return x >= 3 && y >= 3;
    }

    @Nonnull
    @Override
    public ItemStack getResultItem() {
        return ItemPrintout.createMultipleFromTitleAndText(null, null, null);
    }

    @Override
    public boolean matches(@Nonnull CraftingContainer inventory, @Nonnull Level world) {
        return !assemble(inventory).isEmpty();
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingContainer inventory) {
        // See if we match the recipe, and extract the input disk ID and dye colour
        var numPages = 0;
        var numPrintouts = 0;
        ItemStack[] printouts = null;
        var stringFound = false;
        var leatherFound = false;
        var printoutFound = false;
        for (var y = 0; y < inventory.getHeight(); y++) {
            for (var x = 0; x < inventory.getWidth(); x++) {
                var stack = inventory.getItem(x + y * inventory.getWidth());
                if (!stack.isEmpty()) {
                    if (stack.getItem() instanceof ItemPrintout printout && printout.getType() != ItemPrintout.Type.BOOK) {
                        if (printouts == null) printouts = new ItemStack[9];
                        printouts[numPrintouts] = stack;
                        numPages += ItemPrintout.getPageCount(stack);
                        numPrintouts++;
                        printoutFound = true;
                    } else if (stack.getItem() == Items.PAPER) {
                        if (printouts == null) {
                            printouts = new ItemStack[9];
                        }
                        printouts[numPrintouts] = stack;
                        numPages++;
                        numPrintouts++;
                    } else if (stack.is(Tags.Items.STRING) && !stringFound) {
                        stringFound = true;
                    } else if (stack.is(Tags.Items.LEATHER) && !leatherFound) {
                        leatherFound = true;
                    } else {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // Build some pages with what was passed in
        if (numPages <= ItemPrintout.MAX_PAGES && stringFound && printoutFound && numPrintouts >= (leatherFound ? 1 : 2)) {
            var text = new String[numPages * ItemPrintout.LINES_PER_PAGE];
            var colours = new String[numPages * ItemPrintout.LINES_PER_PAGE];
            var line = 0;

            for (var printout = 0; printout < numPrintouts; printout++) {
                var stack = printouts[printout];
                if (stack.getItem() instanceof ItemPrintout) {
                    // Add a printout
                    var pageText = ItemPrintout.getText(printouts[printout]);
                    var pageColours = ItemPrintout.getColours(printouts[printout]);
                    for (var pageLine = 0; pageLine < pageText.length; pageLine++) {
                        text[line] = pageText[pageLine];
                        colours[line] = pageColours[pageLine];
                        line++;
                    }
                } else {
                    // Add a blank page
                    for (var pageLine = 0; pageLine < ItemPrintout.LINES_PER_PAGE; pageLine++) {
                        text[line] = "";
                        colours[line] = "";
                        line++;
                    }
                }
            }

            String title = null;
            if (printouts[0].getItem() instanceof ItemPrintout) {
                title = ItemPrintout.getTitle(printouts[0]);
            }

            if (leatherFound) {
                return ItemPrintout.createBookFromTitleAndText(title, text, colours);
            } else {
                return ItemPrintout.createMultipleFromTitleAndText(title, text, colours);
            }
        }

        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    public static final SimpleRecipeSerializer<?> SERIALIZER = new SimpleRecipeSerializer<>(PrintoutRecipe::new);
}
