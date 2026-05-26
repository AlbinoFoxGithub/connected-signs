package com.connectedsigns;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class SignGroupCache {
    private static World currentWorld;

    private static final Map<BlockPos, List<BlockPos>> groupCache = new HashMap<>();

    private static final Set<BlockPos> individualSigns = new HashSet<>();

    // individuality
    public static void markIndividual(BlockPos pos) {
        individualSigns.add(pos);
        System.out.println("[ConnectedSigns] Marked individual: " + pos);
    }
    public static void unmarkIndividual(BlockPos pos) {
        individualSigns.remove(pos);
        System.out.println("[ConnectedSigns] Unmarked individual: " + pos);
    }
    public static boolean isIndividual(BlockPos pos) {
        return individualSigns.contains(pos);
    }

    public static void setWorld(World world) {
        currentWorld = world;
    }
    public static World getWorld(BlockPos pos) {
        return currentWorld;
    }

    public static void registerGroup(List<BlockPos> group) {
        for (BlockPos pos : group) {
            groupCache.put(pos, group);
        }
    }

    public static List<BlockPos> getGroup(BlockPos pos) {
        return groupCache.get(pos);
    }

    public static Collection<List<BlockPos>> getAllGroups() {
        return new HashSet<>(groupCache.values());
    }

    public static void clear() {
        groupCache.clear();
    }
}