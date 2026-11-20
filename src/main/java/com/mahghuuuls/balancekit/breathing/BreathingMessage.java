package com.mahghuuuls.balancekit.breathing;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.function.DoubleConsumer;

/**
 * The one message Balance Kit sends: the server's breathing multiplier k, from server to a client that has Balance Kit,
 * so the client can scale its own prediction of the local player's air the same way. Common code: no client classes.
 */
public final class BreathingMessage implements IMessage {

    /** Written first, so a later Balance Kit can change the message and an older client can ignore a newer one. */
    static final byte FORMAT = 1;

    private double multiplier = Double.NaN;

    /** For the network layer, which fills the value with {@link #fromBytes}. */
    public BreathingMessage() {
    }

    public BreathingMessage(double multiplier) {
        this.multiplier = multiplier;
    }

    public double multiplier() {
        return multiplier;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(FORMAT);
        buf.writeDouble(multiplier);
    }

    /** A message of another format leaves the value NaN, which the client treats as "no value". */
    @Override
    public void fromBytes(ByteBuf buf) {
        if (buf.readByte() == FORMAT && buf.readableBytes() >= Double.BYTES) {
            multiplier = buf.readDouble();
        }
    }

    /** Hands the received value to {@code receiver} on the game thread of the receiving side. */
    public static final class Handler implements IMessageHandler<BreathingMessage, IMessage> {

        private final DoubleConsumer receiver;

        public Handler(DoubleConsumer receiver) {
            this.receiver = receiver;
        }

        @Override
        public IMessage onMessage(BreathingMessage message, MessageContext ctx) {
            double value = message.multiplier();
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> receiver.accept(value));
            return null;
        }
    }
}
