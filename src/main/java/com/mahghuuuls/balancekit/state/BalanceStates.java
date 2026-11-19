package com.mahghuuuls.balancekit.state;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Registers and attaches {@link BalanceState}. The capability is saved under the entity's {@code ForgeCaps}, so it
 * travels with the entity through unloads, restarts and dimension changes.
 */
public final class BalanceStates {

    @CapabilityInject(BalanceState.class)
    private static Capability<BalanceState> capability;

    private static final ResourceLocation KEY = new ResourceLocation("balancekit", "state");

    private BalanceStates() {
    }

    public static void register() {
        CapabilityManager.INSTANCE.register(BalanceState.class, new Capability.IStorage<BalanceState>() {
            @Nullable
            @Override
            public NBTBase writeNBT(Capability<BalanceState> cap, BalanceState instance, EnumFacing side) {
                // Only used outside an entity, where no health exists; entity saves go through Provider.
                return instance.write(BalanceState.NO_FRACTION);
            }

            @Override
            public void readNBT(Capability<BalanceState> cap, BalanceState instance, EnumFacing side, NBTBase nbt) {
                if (nbt instanceof NBTTagCompound) {
                    instance.read((NBTTagCompound) nbt);
                }
            }
        }, BalanceState::new);
    }

    /**
     * Attaches a state to every entity the predicate accepts. {@code fractionForSave} gives the health fraction to save
     * with the entity; it runs during the entity's own save, so it matches the saved health.
     */
    public static void attach(AttachCapabilitiesEvent<Entity> event, Predicate<Entity> eligible,
                              Function<EntityLivingBase, Float> fractionForSave) {
        Entity entity = event.getObject();
        if (entity instanceof EntityLivingBase && eligible.test(entity)) {
            event.addCapability(KEY, new Provider((EntityLivingBase) entity, fractionForSave));
        }
    }

    @Nullable
    public static BalanceState get(Entity entity) {
        return capability == null ? null : entity.getCapability(capability, null);
    }

    private static final class Provider implements ICapabilitySerializable<NBTTagCompound> {

        private final EntityLivingBase entity;
        private final Function<EntityLivingBase, Float> fractionForSave;
        private final BalanceState state = new BalanceState();

        Provider(EntityLivingBase entity, Function<EntityLivingBase, Float> fractionForSave) {
            this.entity = entity;
            this.fractionForSave = fractionForSave;
        }

        @Override
        public boolean hasCapability(Capability<?> cap, @Nullable EnumFacing facing) {
            return cap != null && cap == capability;
        }

        @Nullable
        @Override
        public <T> T getCapability(Capability<T> cap, @Nullable EnumFacing facing) {
            return hasCapability(cap, facing) ? capability.cast(state) : null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return state.write(fractionForSave.apply(entity));
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            state.read(nbt);
        }
    }
}
