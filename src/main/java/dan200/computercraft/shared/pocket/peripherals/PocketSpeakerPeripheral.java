/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import dan200.computercraft.shared.peripheral.speaker.UpgradeSpeakerPeripheral;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

public class PocketSpeakerPeripheral extends UpgradeSpeakerPeripheral {
    private final IPocketAccess access;
    private Level level;
    private Vec3 position = Vec3.ZERO;

    public PocketSpeakerPeripheral(IPocketAccess access) {
        this.access = access;
    }

    @Nonnull
    @Override
    public SpeakerPosition getPosition() {
        var entity = access.getEntity();
        return entity == null ? SpeakerPosition.of(level, position) : SpeakerPosition.of(entity);
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof PocketSpeakerPeripheral;
    }

    @Override
    public void update() {
        var entity = access.getEntity();
        if (entity != null) {
            level = entity.level;
            position = entity.position();
        }

        super.update();

        access.setLight(madeSound() ? 0x3320fc : -1);
    }
}
