package mana_craft.tile;

import mana_craft.api.registry.MBFuel;
import mana_craft.config.ManaCraftConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import sausage_core.api.util.common.SausageUtils;
import sausage_core.api.util.item.SingleItemStackHandler;
import sausage_core.api.util.tile.ITileDropItems;
import sausage_core.api.util.tile.TileBase;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static mana_craft.api.registry.IManaCraftRegistries.MB_FUELS;
import static mana_craft.block.BlockManaBooster.BURNING;
import static mana_craft.block.BlockManaProducer.SavedData;
import static mana_craft.block.BlockManaProducer.WORKING;

public class TileManaBooster extends TileBase implements ITickable, ITileDropItems {
    public int burn_time = 0;
    public int burn_level = 0;
    public int total_burn_time = 0;
    public SingleItemStackHandler handler = new SingleItemStackHandler();

    @Override
    public ItemStackHandler[] getItemStackHandlers() {
        return new ItemStackHandler[] {handler};
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        handler.deserializeNBT(compound.getCompoundTag("fuel"));
        burn_time = compound.getInteger("burn_time");
        burn_level = compound.getInteger("burn_level");
        total_burn_time = compound.getInteger("total_burn_time");
        state = compound.getInteger("state");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("fuel", handler.serializeNBT());
        compound.setInteger("burn_time", burn_time);
        compound.setInteger("burn_level", burn_level);
        compound.setInteger("total_burn_time", total_burn_time);
        compound.setInteger("state", state);
        return super.writeToNBT(compound);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == capability
                && facing != null && facing.getAxis().isHorizontal()
                || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if(hasCapability(capability, facing)) return SausageUtils.rawtype(handler);
        return super.getCapability(capability, facing);
    }

    private static final int states = 16;
    int state = states - 1;
    int[] times = new int[states];
    void work() {
        if(++state == states) state = 0;
        if(state % 3 == 0)
            --burn_time;
        if(state % 2 == 0)
            SavedData.get(world).list.stream()
                    .filter(dp -> world.provider.getDimension() == dp.getDim())
                    .filter(dp -> pos.distanceSq(dp.getPos()) <= ManaCraftConfig.boostRadius * ManaCraftConfig.boostRadius
                            && dp.getPos().getY() > pos.getY() && world.getBlockState(dp.getPos()).getValue(WORKING))
                    .map(dp -> (TileManaProducer) world.getTileEntity(dp.getPos()))
                    .limit(ManaCraftConfig.boostLimit).filter(Objects::nonNull)
                    .forEach(tile -> {
                        tile.work_time += burn_level;
                        burn_time -= 3;
            });
        if(state == 0) {
            int sum = 0;
            for (int i = 1; i < times.length; ++i)
                sum += times[i] =  burn_level / times.length;
            times[0] = burn_level - sum;
        }
        for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL.facings()) {
            TileEntity tileEntity = world.getTileEntity(pos.offset(facing));
            boolean enable = tileEntity instanceof TileEntityFurnace && ((TileEntityFurnace) tileEntity).isBurning()
            || tileEntity instanceof TileEntityBrewingStand && ((TileEntityBrewingStand) tileEntity).getField(0) > 0;
            if(enable) {
                ITickable tickable = (ITickable) tileEntity;
                --burn_time;
                for (int i = 0; i < times[state]; ++i)
                    tickable.update();
            }
        }
        if(burn_time < 0) burn_time = 0;
    }

    @Override
    public void update() {
        if(world.isRemote)
            return;
        IBlockState state = world.getBlockState(pos);
        if(world.canSeeSky(pos.up())) {
            if(burn_time > 0) {
                work();
            } else {
                Optional<MBFuel> fuel = MB_FUELS.find(fuel0 -> fuel0.test(handler.getStack()));
                if(fuel.isPresent()) {
                    world.setBlockState(pos, state.withProperty(BURNING, Boolean.TRUE));
                    total_burn_time = burn_time = fuel.get().time;
                    burn_level = fuel.get().level;
                    handler.extractItem(1, false);
                    markDirty();
                    return;
                }
                world.setBlockState(pos, state.withProperty(BURNING, Boolean.FALSE));
            }
            return;
        }
        world.setBlockState(pos, state.withProperty(BURNING, Boolean.FALSE));
    }
}
