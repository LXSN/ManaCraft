package mana_craft.tile;

import mana_craft.api.registry.ManaBoostItem;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import sausage_core.api.core.tile.IMachineLogic;
import sausage_core.api.core.tile.ITileDropItems;
import sausage_core.api.core.tile.TileBase;
import sausage_core.api.util.common.SausageUtils;
import sausage_core.api.util.item.SingleItemStackHandler;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Collectors;

import static mana_craft.api.registry.ManaCraftRegistries.BOOST_ITEM;
import static mana_craft.api.registry.ManaCraftRegistries.MB_FUELS;
import static mana_craft.block.BlockManaBooster.BURNING;
import static mana_craft.block.BlockManaProducer.Cache;
import static mana_craft.block.BlockManaProducer.WORKING;
import static mana_craft.config.ManaCraftConfig.boostLimit;
import static mana_craft.config.ManaCraftConfig.boostRadius;

public class TileManaBooster extends TileBase implements ITickable, ITileDropItems, IMachineLogic {
    public int burn_time;
    public int burn_level;
    public int total_burn_time;
    public SingleItemStackHandler handler = new SingleItemStackHandler();

    @Override
    public IItemHandler[] getItemStackHandlers() {
        return new IItemHandler[] {handler};
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        handler.deserializeNBT(compound.getCompoundTag("fuel"));
        burn_time = compound.getInteger("burn_time");
        burn_level = compound.getInteger("burn_level");
        total_burn_time = compound.getInteger("total_burn_time");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("fuel", handler.serializeNBT());
        compound.setInteger("burn_time", burn_time);
        compound.setInteger("burn_level", burn_level);
        compound.setInteger("total_burn_time", total_burn_time);
        return super.writeToNBT(compound);
    }

    public boolean hasCapItem(Capability<?> capability, @Nullable EnumFacing side) {
        return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == capability
                && side != null && side.getAxis().isHorizontal();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side) {
        return hasCapItem(capability, side) || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) {
        if(hasCapItem(capability, side)) return SausageUtils.rawtype(handler);
        return super.getCapability(capability, side);
    }

    static {
        BOOST_ITEM.register(new ManaBoostItem<>(TileEntityFurnace.class, furnace -> furnace.isBurning(), ITickable::update));
        BOOST_ITEM.register(new ManaBoostItem<>(TileEntityBrewingStand.class, brew -> brew.getField(0) > 0, ITickable::update));
    }

    boolean detect0() {
        return world.canSeeSky(pos.up()) && burn_time > 0;
    }

    @Override
    public boolean detect() {
        if(detect0())
            return true;
        MB_FUELS.find(fuel -> fuel.test(handler.get()))
                .ifPresent(fuel -> {
                    total_burn_time = burn_time = fuel.time;
                    burn_level = fuel.level;
                    handler.extractItem(1, false);
                    markDirty();
        });
        return detect0();
    }

    int times;
    @SuppressWarnings("unchecked")
    @Override
    public boolean work() {
        times = 0;
        --burn_time;
        for(TileManaProducer tile : Cache.get(world).list.stream()
                .filter(dp -> world.provider.getDimension() == dp.getDim())
                .filter(dp -> pos.distanceSq(dp.getPos()) <= boostRadius * boostRadius)
                .filter(dp -> dp.getPos().getY() > pos.getY())
                .filter(dp -> world.getBlockState(dp.getPos()).getValue(WORKING))
                .map(dp -> world.getTileEntity(dp.getPos()))
                .map(TileManaProducer.class::cast)
                .filter(Objects::nonNull)
                .limit(boostLimit)
                .collect(Collectors.toList())) {
            for (int i = 0; i < burn_level; ++i) tile.update();
            burn_time -= 4;
        }
        for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL.facings()) {
            BlockPos offset = pos.offset(facing);
            TileEntity tileEntity = world.getTileEntity(offset);
            BOOST_ITEM.find(boostItem -> boostItem.clazz.isInstance(tileEntity)).ifPresent(boostItem -> {
                burn_time -= 5;
                for (int i = 0; boostItem.canBoost.test(tileEntity) && i < burn_level; ++i)
                    boostItem.boost.accept(tileEntity);
            });
        }
        if(burn_time < 0) burn_time = 0;
        return burn_time == 0;
    }

    boolean work;
    @Override
    public void update() {
        if(world.isRemote)
            return;
        work = tick(work);
        world.setBlockState(pos, world.getBlockState(pos).withProperty(BURNING, work));
    }
}
