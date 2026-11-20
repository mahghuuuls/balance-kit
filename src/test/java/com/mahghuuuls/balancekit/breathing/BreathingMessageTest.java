package com.mahghuuuls.balancekit.breathing;

import com.mahghuuuls.balancekit.config.BalanceSettings;
import com.mahghuuuls.balancekit.config.SettingsParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreathingMessageTest {

    private static BreathingAdjuster adjuster(String ownK) {
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.PLAYER_BREATHING, ownK);
        BalanceSettings settings = SettingsParser.parse(raw).settings();
        return new BreathingAdjuster(settings, null, "balancekit");
    }

    @Test
    void theMultiplierSurvivesEncodeAndDecode() {
        ByteBuf buf = Unpooled.buffer();
        new BreathingMessage(1.75D).toBytes(buf);
        assertEquals(9, buf.readableBytes(), "a format byte and one double");
        BreathingMessage read = new BreathingMessage();
        read.fromBytes(buf);
        assertEquals(1.75D, read.multiplier(), 0.0D);
    }

    @Test
    void aMessageOfAnotherFormatGivesNoValue() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(BreathingMessage.FORMAT + 1);
        buf.writeDouble(3.0D);
        BreathingMessage read = new BreathingMessage();
        read.fromBytes(buf);
        assertTrue(Double.isNaN(read.multiplier()));
    }

    @Test
    void theClientUsesOnlyTheServerValueAndNothingBeforeIt() {
        BreathingAdjuster side = adjuster("1.0");
        assertTrue(Double.isNaN(side.effectiveMultiplier(true)), "without the message the client does nothing");
        assertEquals(1.0D, side.effectiveMultiplier(false), 0.0D, "the server uses its own config");
        side.receiveServerMultiplier(2.0D);
        assertEquals(2.0D, side.effectiveMultiplier(true), 0.0D, "the client follows the server, not its own 1.0");
        assertEquals(1.0D, side.effectiveMultiplier(false), 0.0D);
        side.receiveServerMultiplier(0.0D);
        assertTrue(Double.isNaN(side.effectiveMultiplier(true)), "an unusable value is ignored");
    }
}
