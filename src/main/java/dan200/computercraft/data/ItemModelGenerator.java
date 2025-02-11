/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

import static net.minecraft.data.models.model.ModelLocationUtils.getModelLocation;

public final class ItemModelGenerator {
    private ItemModelGenerator() {
    }

    public static void addItemModels(ItemModelGenerators generators) {
        registerDisk(generators, Registry.ModItems.DISK.get());
        registerDisk(generators, Registry.ModItems.TREASURE_DISK.get());

        registerPocketComputer(generators, getModelLocation(Registry.ModItems.POCKET_COMPUTER_NORMAL.get()), false);
        registerPocketComputer(generators, getModelLocation(Registry.ModItems.POCKET_COMPUTER_ADVANCED.get()), false);
        registerPocketComputer(generators, new ResourceLocation(ComputerCraft.MOD_ID, "item/pocket_computer_colour"), true);

        generators.generateFlatItem(Registry.ModItems.PRINTED_BOOK.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(Registry.ModItems.PRINTED_PAGE.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(Registry.ModItems.PRINTED_PAGES.get(), ModelTemplates.FLAT_ITEM);
    }

    private static void registerPocketComputer(ItemModelGenerators generators, ResourceLocation id, boolean off) {
        createFlatItem(generators, addSuffix(id, "_blinking"),
            new ResourceLocation(ComputerCraft.MOD_ID, "item/pocket_computer_blink"),
            id,
            new ResourceLocation(ComputerCraft.MOD_ID, "item/pocket_computer_light")
        );

        createFlatItem(generators, addSuffix(id, "_on"),
            new ResourceLocation(ComputerCraft.MOD_ID, "item/pocket_computer_on"),
            id,
            new ResourceLocation(ComputerCraft.MOD_ID, "item/pocket_computer_light")
        );

        // Don't emit the default/off state for advanced/normal pocket computers, as they have item overrides.
        if (off) {
            createFlatItem(generators, id,
                new ResourceLocation(ComputerCraft.MOD_ID, "item/pocket_computer_frame"),
                id
            );
        }
    }

    private static void registerDisk(ItemModelGenerators generators, Item item) {
        createFlatItem(generators, item,
            new ResourceLocation(ComputerCraft.MOD_ID, "item/disk_frame"),
            new ResourceLocation(ComputerCraft.MOD_ID, "item/disk_colour")
        );
    }

    private static void createFlatItem(ItemModelGenerators generators, Item item, ResourceLocation... ids) {
        createFlatItem(generators, getModelLocation(item), ids);
    }

    /**
     * Generate a flat item from an arbitrary number of layers.
     *
     * @param generators The current item generator helper.
     * @param model      The model we're writing to.
     * @param textures   The textures which make up this model.
     * @see net.minecraft.client.renderer.block.model.ItemModelGenerator The parser for this file format.
     */
    private static void createFlatItem(ItemModelGenerators generators, ResourceLocation model, ResourceLocation... textures) {
        if (textures.length > 5) throw new IndexOutOfBoundsException("Too many layers");
        if (textures.length == 0) throw new IndexOutOfBoundsException("Must have at least one texture");
        if (textures.length == 1) {
            ModelTemplates.FLAT_ITEM.create(model, TextureMapping.layer0(textures[0]), generators.output);
            return;
        }

        var slots = new TextureSlot[textures.length];
        var mapping = new TextureMapping();
        for (var i = 0; i < textures.length; i++) {
            var slot = slots[i] = TextureSlot.create("layer" + i);
            mapping.put(slot, textures[i]);
        }

        new ModelTemplate(Optional.of(new ResourceLocation("item/generated")), Optional.empty(), slots)
            .create(model, mapping, generators.output);
    }

    private static ResourceLocation addSuffix(ResourceLocation location, String suffix) {
        return new ResourceLocation(location.getNamespace(), location.getPath() + suffix);
    }
}
