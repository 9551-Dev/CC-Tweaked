/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.sound;

import dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Queue;

class DfpwmStream implements AudioStream {
    private static final int PREC = 10;
    private static final int LPF_STRENGTH = 140;

    private static final AudioFormat MONO_8 = new AudioFormat(SpeakerPeripheral.SAMPLE_RATE, 8, 1, true, false);

    private final Queue<ByteBuffer> buffers = new ArrayDeque<>(2);

    private int charge = 0; // q
    private int strength = 0; // s
    private int lowPassCharge;
    private boolean previousBit = false;

    DfpwmStream() {
    }

    void push(@Nonnull ByteBuf input) {
        var readable = input.readableBytes();
        var output = ByteBuffer.allocate(readable * 8).order(ByteOrder.nativeOrder());

        for (var i = 0; i < readable; i++) {
            var inputByte = input.readByte();
            for (var j = 0; j < 8; j++) {
                var currentBit = (inputByte & 1) != 0;
                var target = currentBit ? 127 : -128;

                // q' <- q + (s * (t - q) + 128)/256
                var nextCharge = charge + ((strength * (target - charge) + (1 << (PREC - 1))) >> PREC);
                if (nextCharge == charge && nextCharge != target) nextCharge += currentBit ? 1 : -1;

                var z = currentBit == previousBit ? (1 << PREC) - 1 : 0;

                var nextStrength = strength;
                if (strength != z) nextStrength += currentBit == previousBit ? 1 : -1;
                if (nextStrength < 2 << (PREC - 8)) nextStrength = 2 << (PREC - 8);

                // Apply antijerk
                var chargeWithAntijerk = currentBit == previousBit
                    ? nextCharge
                    : nextCharge + charge + 1 >> 1;

                // And low pass filter: outQ <- outQ + ((expectedOutput - outQ) x 140 / 256)
                lowPassCharge += ((chargeWithAntijerk - lowPassCharge) * LPF_STRENGTH + 0x80) >> 8;

                charge = nextCharge;
                strength = nextStrength;
                previousBit = currentBit;

                // OpenAL expects signed data ([0, 255]) while we produce unsigned ([-128, 127]). Do some bit twiddling
                // magic to convert.
                output.put((byte) ((lowPassCharge & 0xFF) ^ 0x80));

                inputByte >>= 1;
            }
        }

        output.flip();
        synchronized (this) {
            buffers.add(output);
        }
    }

    @Nonnull
    @Override
    public AudioFormat getFormat() {
        return MONO_8;
    }

    @Nonnull
    @Override
    public synchronized ByteBuffer read(int capacity) {
        var result = BufferUtils.createByteBuffer(capacity);
        while (result.hasRemaining()) {
            var head = buffers.peek();
            if (head == null) break;

            var toRead = Math.min(head.remaining(), result.remaining());
            result.put(result.position(), head, head.position(), toRead);
            result.position(result.position() + toRead);
            head.position(head.position() + toRead);

            if (head.hasRemaining()) break;
            buffers.remove();
        }

        result.flip();

        // This is naughty, but ensures we're not enqueuing empty buffers when the stream is exhausted.
        return result.remaining() == 0 ? null : result;
    }

    @Override
    public void close() throws IOException {
        buffers.clear();
    }

    public boolean isEmpty() {
        return buffers.isEmpty();
    }
}
