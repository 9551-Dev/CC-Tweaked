/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.sound;

import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps speakers source IDs to a {@link SpeakerInstance}.
 */
@Mod.EventBusSubscriber(Dist.CLIENT)
public class SpeakerManager {
    private static final Map<UUID, SpeakerInstance> sounds = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void playStreaming(PlayStreamingSourceEvent event) {
        if (!(event.getSound() instanceof SpeakerSound sound) || sound.stream == null) return;

        // Associate the sound with the current channel, so SpeakerInstance.pushAudio can queue audio immediately.
        sound.channel = event.getChannel();
        sound.executor = event.getEngine().executor;
    }

    public static SpeakerInstance getSound(UUID source) {
        return sounds.computeIfAbsent(source, x -> new SpeakerInstance());
    }

    public static void stopSound(UUID source) {
        var sound = sounds.remove(source);
        if (sound != null) sound.stop();
    }

    public static void moveSound(UUID source, SpeakerPosition position) {
        var sound = sounds.get(source);
        if (sound != null) sound.setPosition(position);
    }

    public static void reset() {
        sounds.clear();
    }
}
