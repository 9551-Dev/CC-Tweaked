/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.api.upgrades.IUpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.UpgradeManager;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Syncs turtle and pocket upgrades to the client.
 */
public class UpgradesLoadedMessage implements NetworkMessage {
    private final Map<String, UpgradeManager.UpgradeWrapper<TurtleUpgradeSerialiser<?>, ITurtleUpgrade>> turtleUpgrades;
    private final Map<String, UpgradeManager.UpgradeWrapper<PocketUpgradeSerialiser<?>, IPocketUpgrade>> pocketUpgrades;

    public UpgradesLoadedMessage() {
        turtleUpgrades = TurtleUpgrades.instance().getUpgradeWrappers();
        pocketUpgrades = PocketUpgrades.instance().getUpgradeWrappers();
    }

    public UpgradesLoadedMessage(@Nonnull FriendlyByteBuf buf) {
        turtleUpgrades = fromBytes(buf, RegistryManager.ACTIVE.getRegistry(TurtleUpgradeSerialiser.REGISTRY_ID));
        pocketUpgrades = fromBytes(buf, RegistryManager.ACTIVE.getRegistry(PocketUpgradeSerialiser.REGISTRY_ID));
    }

    private <R extends UpgradeSerialiser<? extends T>, T extends IUpgradeBase> Map<String, UpgradeManager.UpgradeWrapper<R, T>> fromBytes(
        @Nonnull FriendlyByteBuf buf, @Nonnull IForgeRegistry<R> registry
    ) {
        var size = buf.readVarInt();
        Map<String, UpgradeManager.UpgradeWrapper<R, T>> upgrades = new HashMap<>(size);
        for (var i = 0; i < size; i++) {
            var id = buf.readUtf();

            var serialiserId = buf.readResourceLocation();
            var serialiser = registry.getValue(serialiserId);
            if (serialiser == null) throw new IllegalStateException("Unknown serialiser " + serialiserId);

            var upgrade = serialiser.fromNetwork(new ResourceLocation(id), buf);
            var modId = buf.readUtf();

            upgrades.put(id, new UpgradeManager.UpgradeWrapper<R, T>(id, upgrade, serialiser, modId));
        }

        return upgrades;
    }

    @Override
    public void toBytes(@Nonnull FriendlyByteBuf buf) {
        toBytes(buf, RegistryManager.ACTIVE.getRegistry(TurtleUpgradeSerialiser.REGISTRY_ID), turtleUpgrades);
        toBytes(buf, RegistryManager.ACTIVE.getRegistry(PocketUpgradeSerialiser.REGISTRY_ID), pocketUpgrades);
    }

    private <R extends UpgradeSerialiser<? extends T>, T extends IUpgradeBase> void toBytes(
        @Nonnull FriendlyByteBuf buf, IForgeRegistry<R> registry, Map<String, UpgradeManager.UpgradeWrapper<R, T>> upgrades
    ) {
        buf.writeVarInt(upgrades.size());
        for (var entry : upgrades.entrySet()) {
            buf.writeUtf(entry.getKey());

            var serialiser = entry.getValue().serialiser();
            @SuppressWarnings("unchecked")
            var unwrapedSerialiser = (UpgradeSerialiser<T>) serialiser;

            buf.writeResourceLocation(Objects.requireNonNull(registry.getKey(serialiser), "Serialiser is not registered!"));
            unwrapedSerialiser.toNetwork(buf, entry.getValue().upgrade());

            buf.writeUtf(entry.getValue().modId());
        }
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        TurtleUpgrades.instance().loadFromNetwork(turtleUpgrades);
        PocketUpgrades.instance().loadFromNetwork(pocketUpgrades);
    }
}
