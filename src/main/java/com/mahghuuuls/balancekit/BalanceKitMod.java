package com.mahghuuuls.balancekit;

import com.mahghuuuls.balancekit.command.InspectCommand;
import com.mahghuuuls.balancekit.config.BalanceSettings;
import com.mahghuuuls.balancekit.config.ConfigFile;
import com.mahghuuuls.balancekit.damage.DamageScaler;
import com.mahghuuuls.balancekit.mob.MobBalancer;
import com.mahghuuuls.balancekit.player.PlayerBalancer;
import com.mahghuuuls.balancekit.rules.RuleBook;
import com.mahghuuuls.balancekit.state.BalanceStates;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
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
    private RuleBook rules;
    private MobBalancer mobs;
    private DamageScaler damage;
    private PlayerBalancer players;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        settings = ConfigFile.load(new File(event.getModConfigurationDirectory(), Tags.MOD_ID + ".cfg"), LOGGER);
        BalanceStates.register();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        rules = new RuleBook(settings);
        mobs = new MobBalancer(rules);
        MinecraftForge.EVENT_BUS.register(mobs);
        players = new PlayerBalancer(settings);
        MinecraftForge.EVENT_BUS.register(players);
        damage = new DamageScaler(settings, mobs);
        MinecraftForge.EVENT_BUS.register(damage);
    }

    /** Entity IDs are known only once every mod has registered its entities. */
    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        for (String id : rules.entityIds()) {
            if (!ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(id))) {
                LOGGER.warn("Config: entity ID {} is not registered by any installed mod; its entries apply only once it is", id);
            }
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new InspectCommand(mobs, players, damage));
    }
}
