/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;

public class MouseEventServerMessage extends ComputerServerMessage {
    public static final int TYPE_CLICK = 0;
    public static final int TYPE_DRAG = 1;
    public static final int TYPE_UP = 2;
    public static final int TYPE_SCROLL = 3;

    private final int type;
    private final int x;
    private final int y;
    private final int arg;

    public MouseEventServerMessage(AbstractContainerMenu menu, int type, int arg, int x, int y) {
        super(menu);
        this.type = type;
        this.arg = arg;
        this.x = x;
        this.y = y;
    }

    public MouseEventServerMessage(@Nonnull FriendlyByteBuf buf) {
        super(buf);
        type = buf.readByte();
        arg = buf.readVarInt();
        x = buf.readVarInt();
        y = buf.readVarInt();
    }

    @Override
    public void toBytes(@Nonnull FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeByte(type);
        buf.writeVarInt(arg);
        buf.writeVarInt(x);
        buf.writeVarInt(y);
    }

    @Override
    protected void handle(NetworkEvent.Context context, @Nonnull ComputerMenu container) {
        var input = container.getInput();
        switch (type) {
            case TYPE_CLICK -> input.mouseClick(arg, x, y);
            case TYPE_DRAG -> input.mouseDrag(arg, x, y);
            case TYPE_UP -> input.mouseUp(arg, x, y);
            case TYPE_SCROLL -> input.mouseScroll(arg, x, y);
        }
    }
}
