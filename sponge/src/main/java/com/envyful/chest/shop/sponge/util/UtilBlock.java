package com.envyful.chest.shop.sponge.util;

import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Arrays;
import java.util.List;

public class UtilBlock {

    private static final Direction[] DIRECTIONS = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST
    };

    public static TileEntity getPlacedOn(Location<World> location, BlockType... types) {
        List<BlockType> specificTypes = Arrays.asList(types);

        for (Direction direction : DIRECTIONS) {
            Location<World> relative = location.getRelative(direction);
            BlockState block = relative.getBlock();

            if (specificTypes.contains(block.getType()) || specificTypes.isEmpty()) {
                return relative.getTileEntity().get();
            }
        }

        return null;
    }
}
