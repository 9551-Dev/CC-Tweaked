/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.turtle;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.upgrades.IUpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import dan200.computercraft.impl.upgrades.SerialiserWithCraftingItem;
import dan200.computercraft.impl.upgrades.SimpleSerialiser;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Reads a {@link ITurtleUpgrade} from disk and reads/writes it to a network packet.
 * <p>
 * These should be registered in a {@link Registry} while the game is loading, much like {@link RecipeSerializer}s.
 * <p>
 * If your turtle upgrade doesn't have any associated configurable parameters (like most upgrades), you can use
 * {@link #simple(Function)} or {@link #simpleWithCustomItem(BiFunction)} to create a basic upgrade serialiser.
 *
 * <h2>Example (Forge)</h2>
 * <pre>{@code
 * static final DeferredRegister<TurtleUpgradeSerialiser<?>> SERIALISERS = DeferredRegister.create( TurtleUpgradeSerialiser.TYPE, "my_mod" );
 *
 * // Register a new upgrade serialiser called "my_upgrade".
 * public static final RegistryObject<TurtleUpgradeSerialiser<MyUpgrade>> MY_UPGRADE =
 *     SERIALISERS.register( "my_upgrade", () -> TurtleUpgradeSerialiser.simple( MyUpgrade::new ) );
 *
 * // Then in your constructor
 * SERIALISERS.register( bus );
 * }</pre>
 * <p>
 * We can then define a new upgrade using JSON by placing the following in
 * {@literal data/<my_mod>/computercraft/turtle_upgrades/<my_upgrade_id>.json}}.
 *
 * <pre>{@code
 * {
 *     "type": my_mod:my_upgrade",
 * }
 * }</pre>
 * <p>
 * Finally, we need to register a model for our upgrade. This is done with
 * {@link dan200.computercraft.api.client.ComputerCraftAPIClient#registerTurtleUpgradeModeller}:
 *
 * <pre>{@code
 * // Register our model inside FMLClientSetupEvent
 * ComputerCraftAPIClient.registerTurtleUpgradeModeller(MY_UPGRADE.get(), TurtleUpgradeModeller.flatItem())
 * }</pre>
 * <p>
 * {@link TurtleUpgradeDataProvider} provides a data provider to aid with generating these JSON files.
 *
 * @param <T> The type of turtle upgrade this is responsible for serialising.
 * @see ITurtleUpgrade
 * @see TurtleUpgradeDataProvider
 * @see dan200.computercraft.api.client.turtle.TurtleUpgradeModeller
 */
public interface TurtleUpgradeSerialiser<T extends ITurtleUpgrade> extends UpgradeSerialiser<T> {
    /**
     * The ID for the associated registry.
     */
    ResourceKey<Registry<TurtleUpgradeSerialiser<?>>> REGISTRY_ID = ResourceKey.createRegistryKey(new ResourceLocation(ComputerCraftAPI.MOD_ID, "turtle_upgrade_serialiser"));

    /**
     * The associated registry.
     *
     * @return The registry for pocket upgrade serialisers.
     * @see #REGISTRY_ID
     * @deprecated Use {@link #REGISTRY_ID} directly.
     */
    @Deprecated(forRemoval = true)
    static IForgeRegistry<TurtleUpgradeSerialiser<?>> registry() {
        return RegistryManager.ACTIVE.getRegistry(REGISTRY_ID);
    }

    /**
     * Create an upgrade serialiser for a simple upgrade. This is similar to a {@link SimpleRecipeSerializer}, but for
     * upgrades.
     * <p>
     * If you might want to vary the item, it's suggested you use {@link #simpleWithCustomItem(BiFunction)} instead.
     *
     * @param factory Generate a new upgrade with a specific ID.
     * @param <T>     The type of the generated upgrade.
     * @return The serialiser for this upgrade
     */
    static <T extends ITurtleUpgrade> TurtleUpgradeSerialiser<T> simple(Function<ResourceLocation, T> factory) {
        final class Impl extends SimpleSerialiser<T> implements TurtleUpgradeSerialiser<T> {
            private Impl(Function<ResourceLocation, T> constructor) {
                super(constructor);
            }
        }

        return new Impl(factory);
    }

    /**
     * Create an upgrade serialiser for a simple upgrade whose crafting item can be specified.
     *
     * @param factory Generate a new upgrade with a specific ID and crafting item. The returned upgrade's
     *                {@link IUpgradeBase#getCraftingItem()} <strong>MUST</strong> equal the provided item.
     * @param <T>     The type of the generated upgrade.
     * @return The serialiser for this upgrade.
     * @see #simple(Function)  For upgrades whose crafting stack should not vary.
     */
    static <T extends ITurtleUpgrade> TurtleUpgradeSerialiser<T> simpleWithCustomItem(BiFunction<ResourceLocation, ItemStack, T> factory) {
        final class Impl extends SerialiserWithCraftingItem<T> implements TurtleUpgradeSerialiser<T> {
            private Impl(BiFunction<ResourceLocation, ItemStack, T> factory) {
                super(factory);
            }
        }

        return new Impl(factory);
    }
}
