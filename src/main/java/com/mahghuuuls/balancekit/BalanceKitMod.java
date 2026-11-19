package com.mahghuuuls.balancekit;

import com.mahghuuuls.balancekit.command.InspectCommand;
import com.mahghuuuls.balancekit.config.BalanceSettings;
import com.mahghuuuls.balancekit.config.ConfigFile;
import com.mahghuuuls.balancekit.mob.MobBalancer;
import com.mahghuuuls.balancekit.state.BalanceStates;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Entry point. Gameplay rules live in their own packages; this class only wires them to the Forge lifecycle.
 * Clients without the mod may join a server that has it, because all balance rules run on the logical server.
 */
@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, acceptableRemoteVersions = "*")
public class BalanceKitMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    private BalanceSettings settings = BalanceSettings.NEUTRAL;
    private MobBalancer mobs;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        settings = ConfigFile.load(new File(event.getModConfigurationDirectory(), Tags.MOD_ID + ".cfg"), LOGGER);
        BalanceStates.register();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        mobs = new MobBalancer(settings);
        MinecraftForge.EVENT_BUS.register(mobs);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new InspectCommand(mobs));
    }
}
