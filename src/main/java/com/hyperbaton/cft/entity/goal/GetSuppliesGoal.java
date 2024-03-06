package com.hyperbaton.cft.entity.goal;

import com.hyperbaton.cft.entity.custom.XunguiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.Optional;

public class GetSuppliesGoal extends Goal {
    protected final XunguiEntity mob;

    protected final BlockPos targetContainer;

    public GetSuppliesGoal(XunguiEntity mob, BlockPos targetContainer) {
        this.mob = mob;
        this.targetContainer = targetContainer;
    }

    @Override
    public boolean canUse() {
        return mob.getHome() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse() && !this.mob.getNavigation().isDone() && !this.mob.isVehicle();
    }

    public void start() {
        System.out.println("Starting Get supplies goal\n");
        System.out.println("Container at X: " + targetContainer.getX() + " Y: " + targetContainer.getY() + " Z: " + targetContainer.getZ() + "\n");
        ServerLevel level = (ServerLevel) mob.level();
        Optional<BlockPos> targetPosition = findEmptyBlockNextToContainer(level, targetContainer);
        if(targetPosition.isPresent()){
            this.mob.getNavigation().moveTo(targetPosition.get().getX(), targetPosition.get().getY(), targetPosition.get().getZ(), this.mob.getSpeed());
        } else {
            System.out.println("Container can't be accessed\n");
        }

    }

    private Optional<BlockPos> findEmptyBlockNextToContainer(ServerLevel level, BlockPos targetContainer) {
        return Direction.allShuffled(level.random).stream()
                .map(direction -> targetContainer.relative(direction))
                .filter(pos -> level.getBlockState(pos).isAir())
                .findAny();
        /*Direction.allShuffled(level.random).stream()
                .map(direction -> targetContainer.relative(direction))
                .map(pos -> level.getBlockState(pos)).toList();*/
    }

    public void stop() {
        this.mob.getNavigation().stop();

        System.out.println("Checking container in home\n");
        ChestBlockEntity chest = (ChestBlockEntity) mob.level().getBlockEntity(targetContainer);
        LazyOptional<IItemHandler> itemHandlerOptional = chest.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP);
        if(itemHandlerOptional.isPresent()){
            InvWrapper chestInventory = (InvWrapper) itemHandlerOptional.orElse(null);
            int latestStickSlot = -1;
            for(int i = 0; i < chest.getContainerSize(); i++){
                if(chestInventory.getStackInSlot(i).getItem() == Items.STICK) {
                    latestStickSlot = i;
                }
            }
            if(latestStickSlot >= 0){
                mob.getInventory().addItem(chestInventory.extractItem(latestStickSlot, 1, false));
            }
        }
        super.stop();
    }
}
