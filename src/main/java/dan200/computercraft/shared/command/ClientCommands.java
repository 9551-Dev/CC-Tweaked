/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ServerContext;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;

/**
 * Basic client-side commands.
 * <p>
 * Simply hooks into client chat messages and intercepts matching strings.
 */
@Mod.EventBusSubscriber(modid = ComputerCraft.MOD_ID, value = Dist.CLIENT)
public final class ClientCommands {
    public static final String OPEN_COMPUTER = "/computercraft open-computer ";

    private ClientCommands() {
    }

    @SubscribeEvent
    public static void onClientSendMessage(ClientChatEvent event) {
        // Emulate the command on the client side
        if (event.getMessage().startsWith(OPEN_COMPUTER)) {
            MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server == null) return;

            event.setCanceled(true);

            var idStr = event.getMessage().substring(OPEN_COMPUTER.length()).trim();
            int id;
            try {
                id = Integer.parseInt(idStr);
            } catch (NumberFormatException ignore) {
                return;
            }

            var file = new File(ServerContext.get(server).storageDir().toFile(), "computer/" + id);
            if (!file.isDirectory()) return;

            Util.getPlatform().openFile(file);
        }
    }

}
