package com.github.yaossg.mana_craft.inventory;

import com.github.yaossg.sausage_core.api.util.inventory.IEnumGUIHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public enum ManaCraftGUIs implements IEnumGUIHandler {
    ManaProducer {
        @Nonnull
        @Override
        public Object getServer(EntityPlayer player, World world, BlockPos pos) {
            return new ContainerManaProducer(player.inventory, world.getTileEntity(pos));
        }

        @Nonnull
        @Override
        public Object getClient(EntityPlayer player, World world, BlockPos pos) {
            return new GUIContainerManaProducer(new ContainerManaProducer(player.inventory, world.getTileEntity(pos)));
        }
    },
    ManaBooster {
        @Nonnull
        @Override
        public Object getServer(EntityPlayer player, World world, BlockPos pos) {
            return new ContainerManaBooster(player.inventory, world.getTileEntity(pos));
        }

        @Nonnull
        @Override
        public Object getClient(EntityPlayer player, World world, BlockPos pos) {
            return new GUIContainerManaBooster(new ContainerManaBooster(player.inventory, world.getTileEntity(pos)));
        }
    }
}