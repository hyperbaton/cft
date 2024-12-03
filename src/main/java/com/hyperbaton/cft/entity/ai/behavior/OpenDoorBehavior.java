package com.hyperbaton.cft.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.Optional;

public class OpenDoorBehavior extends Behavior<Mob> {

    private Optional<BlockPos> lastDoorPosition = Optional.empty();
    public OpenDoorBehavior() {
        super(Collections.emptyMap());
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Mob entity) {
        BlockPos targetPos = entity.getNavigation().getTargetPos();

        // Ensure the target position is valid and check for a door blocking the path
        if (targetPos == null) {
            return false;
        }

        BlockPos mobPos = entity.blockPosition();
        BlockPos nextStep = getNextStep(mobPos, targetPos);

        // Check if the next step contains a closed door
        return isClosedDoor(level, nextStep);
    }

    private boolean isClosedDoor(ServerLevel level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        return blockState.getBlock() instanceof DoorBlock && !blockState.getValue(DoorBlock.OPEN);
    }

    private BlockPos getNextStep(BlockPos mobPos, BlockPos targetPos) {
        // Calculate the next step in the path toward the target position
        int xStep = Integer.compare(targetPos.getX(), mobPos.getX());
        int yStep = Integer.compare(targetPos.getY(), mobPos.getY());
        int zStep = Integer.compare(targetPos.getZ(), mobPos.getZ());

        return mobPos.offset(xStep, yStep, zStep);
    }

    @Override
    protected void start(ServerLevel level, Mob entity, long gameTime) {
        BlockPos mobPos = entity.blockPosition();
        BlockPos targetPos = entity.getNavigation().getTargetPos();
        BlockPos nextStep = getNextStep(mobPos, targetPos);

        // Open the door if it's blocking the path
        if (nextStep != null && isClosedDoor(level, nextStep)) {
            BlockState doorState = level.getBlockState(nextStep);
            level.setBlock(nextStep, doorState.setValue(DoorBlock.OPEN, true), 10);

            // Remember this door to close it later
            lastDoorPosition = Optional.of(nextStep);
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Mob entity, long gameTime) {
        // Continue the behavior if the door was opened and the entity hasn't moved far from it
        return lastDoorPosition.isPresent() &&
                lastDoorPosition.get().distManhattan(entity.blockPosition()) <= 2;
    }

    @Override
    protected void stop(ServerLevel level, Mob entity, long gameTime) {
        lastDoorPosition.ifPresent(pos -> {
            BlockState doorState = level.getBlockState(pos);
            if (doorState.getBlock() instanceof DoorBlock && doorState.getValue(DoorBlock.OPEN)) {
                level.setBlock(pos, doorState.setValue(DoorBlock.OPEN, false), 10);
            }
        });

        // Clear the last door position
        lastDoorPosition = Optional.empty();
    }
}
