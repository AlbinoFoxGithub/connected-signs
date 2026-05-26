package com.connectedsigns;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class SignGroup {
    public static final int MAX_GROUP_SIZE = 6;

    public static List<BlockPos> findConnectedSigns(World world, BlockPos origin) {
        if (SignGroupCache.isIndividual(origin)) {
            return List.of(origin);
        }
        BlockState originState = world.getBlockState(origin);

        if (!(originState.getBlock() instanceof WallSignBlock)) {
            return List.of(origin);
        }

        Direction facing = originState.get(WallSignBlock.FACING);
        Block originBlock = originState.getBlock();
        Direction leftRight = facing.rotateYClockwise();

        // debug log
        // System.out.println("Sign at " + origin + " facing " + facing + " leftRight axis " + leftRight);

        List<BlockPos> result = new ArrayList<>();

        BlockPos current = origin.offset(leftRight.getOpposite());
        while (isSameWallSign(world, current, facing, originBlock) && result.size() < MAX_GROUP_SIZE - 1) {
            result.add(0, current);
            current = current.offset(leftRight.getOpposite());
        }

        result.add(origin);

        current = origin.offset(leftRight);
        while (isSameWallSign(world, current, facing, originBlock) && result.size() < MAX_GROUP_SIZE) {
            result.add(current);
            current = current.offset(leftRight);
        }

        java.util.Collections.reverse(result);

        return result;
    }

    private static boolean isSameWallSign(World world, BlockPos pos, Direction expected, Block expectedBlock) {
        if (SignGroupCache.isIndividual(pos)) return false;
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof WallSignBlock)) return false;
        if (!(world.getBlockEntity(pos) instanceof SignBlockEntity)) return false;
        if (state.get(WallSignBlock.FACING) != expected) return false;
        if (state.getBlock() != expectedBlock) return false;
        return true;
    }
}