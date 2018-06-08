package com.github.yaossg.mana_craft.tile;

import com.github.yaossg.mana_craft.APIsInstance;
import com.github.yaossg.mana_craft.api.ManaCraftAPIs;
import com.github.yaossg.mana_craft.block.ManaCraftBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Arrays;

import static com.github.yaossg.mana_craft.block.BlockManaProducer.FACING;
import static com.github.yaossg.mana_craft.block.BlockManaProducer.WORKING;

public class TileManaProducer extends TileEntity implements ITickable {
    public int work_time;
    public int total_work_time;
    public ItemStackHandler input = new ItemStackHandler(4);
    public ItemStackHandler output = new ItemStackHandler();

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        input.deserializeNBT(compound.getCompoundTag("input"));
        output.deserializeNBT(compound.getCompoundTag("output"));
        work_time = compound.getInteger("work_time");
        total_work_time = compound.getInteger("total_work_time");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("input", this.input.serializeNBT());
        compound.setTag("output", this.output.serializeNBT());
        compound.setInteger("work_time", this.work_time);
        compound.setInteger("total_work_time", this.total_work_time);
        return super.writeToNBT(compound);
    }

    public boolean checkCharged() {
        EnumFacing facing = world.getBlockState(pos).getValue(FACING);
        return world.getBlockState(pos.down()).getBlock() == ManaCraftBlocks.manaIngotBlock
                && world.getBlockState(pos.up()).getBlock() == ManaCraftBlocks.manaGlass
                && world.isAirBlock(pos.offset(facing))
                && world.getBlockState(pos.offset(facing.rotateY())).getBlock() == ManaCraftBlocks.manaGlass
                && world.getBlockState(pos.offset(facing.rotateY().rotateY())).getBlock() == ManaCraftBlocks.manaGlass
                && world.getBlockState(pos.offset(facing.rotateY().rotateY().rotateY())).getBlock() == ManaCraftBlocks.manaGlass

                && world.isAirBlock(pos.offset(facing, 2))
                && world.isAirBlock(pos.offset(facing.rotateY(), 2))
                && world.isAirBlock(pos.offset(facing.rotateY().rotateY(), 2))
                && world.isAirBlock(pos.offset(facing.rotateY().rotateY().rotateY(), 2))

                && world.getBlockState(pos.add(1, -1, 0)).getBlock() == ManaCraftBlocks.manaBlock
                && world.getBlockState(pos.add(0, -1, 1)).getBlock() == ManaCraftBlocks.manaBlock
                && world.getBlockState(pos.add(-1, -1, 0)).getBlock() == ManaCraftBlocks.manaBlock
                && world.getBlockState(pos.add(0, -1, -1)).getBlock() == ManaCraftBlocks.manaBlock

                && world.getBlockState(pos.add(1, 0, 1)).getBlock() == ManaCraftBlocks.manaBlock
                && world.getBlockState(pos.add(-1, 0, 1)).getBlock() == ManaCraftBlocks.manaBlock
                && world.getBlockState(pos.add(-1, 0, -1)).getBlock() == ManaCraftBlocks.manaBlock
                && world.getBlockState(pos.add(1, 0, -1)).getBlock() == ManaCraftBlocks.manaBlock

                && world.getBlockState(pos.add(1, 1, 0)).getBlock() == ManaCraftBlocks.manaBlock
                && world.getBlockState(pos.add(0, 1, 1)).getBlock() == ManaCraftBlocks.manaBlock
                && world.getBlockState(pos.add(-1, 1, 0)).getBlock() == ManaCraftBlocks.manaBlock
                && world.getBlockState(pos.add(0, 1, -1)).getBlock() == ManaCraftBlocks.manaBlock

                && world.getBlockState(pos.add(1, -1, 1)).getBlock() == ManaCraftBlocks.manaIngotBlock
                && world.getBlockState(pos.add(-1, -1, 1)).getBlock() == ManaCraftBlocks.manaIngotBlock
                && world.getBlockState(pos.add(-1, -1, -1)).getBlock() == ManaCraftBlocks.manaIngotBlock
                && world.getBlockState(pos.add(1, -1, -1)).getBlock() == ManaCraftBlocks.manaIngotBlock

                && world.canSeeSky(pos.up(2))
                && world.canSeeSky(pos.add(1, 2, 0))
                && world.canSeeSky(pos.add(-1, 2, 0))
                && world.canSeeSky(pos.add(0, 2, 1))
                && world.canSeeSky(pos.add(0, 2, -1))
                && world.canSeeSky(pos.add(1, 2, 1))
                && world.canSeeSky(pos.add(-1, 2, 1))
                && world.canSeeSky(pos.add(1, 2, -1))
                && world.canSeeSky(pos.add(-1, 2, -1))
                && world.canSeeSky(pos.offset(facing, 2));
    }

    public boolean isSorted = false;
    ItemStackHandler getSorted() {
        ItemStackHandler handler = new ItemStackHandler(4);
        for (int i = 0; i < input.getSlots(); ++i)
            ItemHandlerHelper.insertItemStacked(handler, input.getStackInSlot(i).copy(), false);
        int empty = 0;
        for (int i = 0; i < handler.getSlots(); ++i)
            if(handler.getStackInSlot(i).isEmpty()) ++empty;
        ItemStack[] items = new ItemStack[handler.getSlots() - empty];
        for (int i = 0; i < items.length; ++i)
            items[i] = handler.getStackInSlot(i);
        Arrays.sort(items, ManaCraftAPIs.Recipe.comparatorInput);
        handler = new ItemStackHandler(items.length);
        for (int i = 0; i < handler.getSlots(); ++i)
            handler.setStackInSlot(i, items[i]);
        return handler;
    }

    ManaCraftAPIs.Recipe detect() {
        ItemStackHandler handler = getSorted();
        for(ManaCraftAPIs.Recipe recipe : APIsInstance.recipes) {
            ItemStack[] matches = recipe.getInput();
            boolean found = handler.getSlots() >= matches.length;
            for (int i = 0; i < matches.length && found
                    && (found = ItemStack.areItemStacksEqual(handler.extractItem(i, matches[i].getCount(), true), matches[i])); ++i);
            if (found)
                return recipe;
        }
        return null;
    }

    void copyToInput(ItemStackHandler handler) {
        for (int i = 0; i < input.getSlots(); ++i)
            input.setStackInSlot(i, i < handler.getSlots() ? handler.getStackInSlot(i) : ItemStack.EMPTY);
    }
    @Override
    public void update() {
        if (!world.isRemote) {
            IBlockState state = this.getWorld().getBlockState(pos);
            if(work_time > total_work_time)
                work_time = total_work_time;
            if(checkCharged()) {
                ManaCraftAPIs.Recipe current = detect();
                if (current != null && output.insertItem(0, current.getOutput(), true).isEmpty()) {
                    this.getWorld().setBlockState(pos, state.withProperty(WORKING, Boolean.TRUE));
                    if(total_work_time != current.getWorkTime()) {
                        total_work_time = current.getWorkTime();
                        work_time = Integer.min(work_time, total_work_time - 1);
                    }
                    if(!isSorted) {
                        copyToInput(getSorted());
                        isSorted = true;
                    }
                    if ((++work_time) >= total_work_time) {
                        this.work_time -= current.getWorkTime();
                        ItemStackHandler temp = getSorted();
                        for (int i = 0; i < temp.getSlots() && i < current.getInput().length; ++i)
                            temp.extractItem(i, current.getInput()[i].getCount(),false);
                        copyToInput(temp);
                        output.insertItem(0, current.getOutput().copy(), false);
                        markDirty();
                    }
                } else {
                    if (work_time > 0)
                        work_time -= 3;
                    if (work_time < 0)
                        work_time = 0;
                    world.setBlockState(pos, state.withProperty(WORKING, Boolean.FALSE));
                }
            } else {
                work_time = 0;
                world.setBlockState(pos, state.withProperty(WORKING, Boolean.FALSE));
            }
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }
}
