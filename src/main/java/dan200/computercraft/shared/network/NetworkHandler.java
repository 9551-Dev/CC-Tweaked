/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.network.client.*;
import dan200.computercraft.shared.network.server.*;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Collection;
import java.util.function.Function;

public final class NetworkHandler {
    private static SimpleChannel network;
    private static final IntSet usedIds = new IntOpenHashSet();

    private NetworkHandler() {
    }

    public static void setup() {
        var version = ComputerCraftAPI.getInstalledVersion();
        network = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(ComputerCraft.MOD_ID, "network"))
            .networkProtocolVersion(() -> version)
            .clientAcceptedVersions(version::equals).serverAcceptedVersions(version::equals)
            .simpleChannel();

        // Server messages
        registerMainThread(0, NetworkDirection.PLAY_TO_SERVER, ComputerActionServerMessage.class, ComputerActionServerMessage::new);
        registerMainThread(1, NetworkDirection.PLAY_TO_SERVER, QueueEventServerMessage.class, QueueEventServerMessage::new);
        registerMainThread(2, NetworkDirection.PLAY_TO_SERVER, KeyEventServerMessage.class, KeyEventServerMessage::new);
        registerMainThread(3, NetworkDirection.PLAY_TO_SERVER, MouseEventServerMessage.class, MouseEventServerMessage::new);
        registerMainThread(4, NetworkDirection.PLAY_TO_SERVER, UploadFileMessage.class, UploadFileMessage::new);

        // Client messages
        registerMainThread(10, NetworkDirection.PLAY_TO_CLIENT, ChatTableClientMessage.class, ChatTableClientMessage::new);
        registerMainThread(11, NetworkDirection.PLAY_TO_CLIENT, PocketComputerDataMessage.class, PocketComputerDataMessage::new);
        registerMainThread(12, NetworkDirection.PLAY_TO_CLIENT, PocketComputerDeletedClientMessage.class, PocketComputerDeletedClientMessage::new);
        registerMainThread(13, NetworkDirection.PLAY_TO_CLIENT, ComputerTerminalClientMessage.class, ComputerTerminalClientMessage::new);
        registerMainThread(14, NetworkDirection.PLAY_TO_CLIENT, PlayRecordClientMessage.class, PlayRecordClientMessage::new);
        registerMainThread(15, NetworkDirection.PLAY_TO_CLIENT, MonitorClientMessage.class, MonitorClientMessage::new);
        registerMainThread(16, NetworkDirection.PLAY_TO_CLIENT, SpeakerAudioClientMessage.class, SpeakerAudioClientMessage::new);
        registerMainThread(17, NetworkDirection.PLAY_TO_CLIENT, SpeakerMoveClientMessage.class, SpeakerMoveClientMessage::new);
        registerMainThread(18, NetworkDirection.PLAY_TO_CLIENT, SpeakerPlayClientMessage.class, SpeakerPlayClientMessage::new);
        registerMainThread(19, NetworkDirection.PLAY_TO_CLIENT, SpeakerStopClientMessage.class, SpeakerStopClientMessage::new);
        registerMainThread(20, NetworkDirection.PLAY_TO_CLIENT, UploadResultMessage.class, UploadResultMessage::new);
        registerMainThread(21, NetworkDirection.PLAY_TO_CLIENT, UpgradesLoadedMessage.class, UpgradesLoadedMessage::new);
    }

    public static void sendToPlayer(ServerPlayer player, NetworkMessage packet) {
        network.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToAllPlayers(NetworkMessage packet) {
        network.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendToServer(NetworkMessage packet) {
        network.sendToServer(packet);
    }

    public static void sendToAllAround(NetworkMessage packet, Level world, Vec3 pos, double range) {
        var target = new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, range, world.dimension());
        network.send(PacketDistributor.NEAR.with(() -> target), packet);
    }

    public static void sendToAllTracking(NetworkMessage packet, LevelChunk chunk) {
        network.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), packet);
    }

    public static void sendToPlayers(NetworkMessage packet, Collection<ServerPlayer> players) {
        if (players.isEmpty()) return;

        var vanillaPacket = network.toVanillaPacket(packet, NetworkDirection.PLAY_TO_CLIENT);
        for (var player : players) player.connection.send(vanillaPacket);
    }


    /**
     * Register packet, and a thread-unsafe handler for it.
     *
     * @param <T>       The type of the packet to send.
     * @param type      The class of the type of packet to send.
     * @param id        The identifier for this packet type.
     * @param direction A network direction which will be asserted before any processing of this message occurs
     * @param decoder   The factory for this type of packet.
     */
    private static <T extends NetworkMessage> void registerMainThread(int id, NetworkDirection direction, Class<T> type, Function<FriendlyByteBuf, T> decoder) {
        if (!usedIds.add(id)) throw new IllegalStateException("Duplicate message kind for for id " + id);
        network.messageBuilder(type, id, direction)
            .encoder(NetworkMessage::toBytes)
            .decoder(decoder)
            .consumerMainThread((packet, contextSup) -> {
                var context = contextSup.get();
                context.enqueueWork(() -> packet.handle(context));
                context.setPacketHandled(true);
            })
            .add();
    }
}
