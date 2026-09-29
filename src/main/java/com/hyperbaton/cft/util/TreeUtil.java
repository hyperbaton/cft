package com.hyperbaton.cft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Finds natural trees: clusters of logs with leaves that grew on their own. Leaves placed by a
 * player are persistent and never decay, so a log cluster only counts as a tree if it has
 * non-persistent leaves. That keeps log houses, log walls and other builds safe.
 */
public final class TreeUtil {
    private TreeUtil() {}

    /** Leaves further than this from the logs are left alone (vanilla leaves decay beyond 6). */
    private static final int MAX_LEAF_DISTANCE = 6;

    /**
     * A tree to fell.
     *
     * @param logs          its logs, from the top down
     * @param leaves        its natural leaves
     * @param plantingSpots where saplings can go once it's felled: the bottom logs standing on dirt
     */
    public record Tree(List<BlockPos> logs, List<BlockPos> leaves, List<BlockPos> plantingSpots) {
        public BlockPos base() {
            return logs.get(logs.size() - 1);
        }

        /** The kind of log it's made of, to replant the matching sapling. */
        public ResourceLocation logId(ServerLevel level) {
            return BuiltInRegistries.BLOCK.getKey(level.getBlockState(base()).getBlock());
        }
    }

    /**
     * The nearest natural tree around {@code center}, looking at logs standing on dirt within the
     * radius. Logs in {@code protectedBlocks} (e.g. those of registered structures) are never part
     * of a tree, and neither are clusters bigger than {@code maxLogs}.
     */
    public static Optional<Tree> findNearestTree(ServerLevel level, BlockPos center, int radius, BlockPos from,
                                                 Set<BlockPos> protectedBlocks, int maxLogs) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 6, radius))) {
            if (level.getBlockState(pos).is(BlockTags.LOGS) && level.getBlockState(pos.below()).is(BlockTags.DIRT)
                    && !protectedBlocks.contains(pos)) {
                candidates.add(pos.immutable());
            }
        }
        candidates.sort(Comparator.comparingDouble(pos -> pos.distSqr(from)));

        Set<BlockPos> checked = new HashSet<>();
        for (BlockPos candidate : candidates) {
            if (checked.contains(candidate)) continue;
            Optional<Tree> tree = treeAt(level, candidate, protectedBlocks, maxLogs, checked);
            if (tree.isPresent()) return tree;
        }
        return Optional.empty();
    }

    /** The sapling that grows into this kind of log (e.g. oak_log -> oak_sapling), if there is one. */
    public static Optional<Item> matchingSapling(ResourceLocation logId) {
        String path = logId.getPath();
        if (!path.endsWith("_log")) return Optional.empty();
        ResourceLocation saplingId = ResourceLocation.fromNamespaceAndPath(logId.getNamespace(),
                path.substring(0, path.length() - "_log".length()) + "_sapling");
        return BuiltInRegistries.ITEM.getOptional(saplingId)
                .filter(item -> new ItemStack(item).is(ItemTags.SAPLINGS));
    }

    /** Whether this sapling can be planted at the spot. */
    public static boolean canPlant(ServerLevel level, ItemStack sapling, BlockPos spot) {
        if (!(sapling.getItem() instanceof BlockItem blockItem) || !sapling.is(ItemTags.SAPLINGS)) return false;
        return level.getBlockState(spot).canBeReplaced()
                && blockItem.getBlock().defaultBlockState().canSurvive(level, spot);
    }

    public static void plant(ServerLevel level, ItemStack sapling, BlockPos spot) {
        if (sapling.getItem() instanceof BlockItem blockItem) {
            level.setBlockAndUpdate(spot, blockItem.getBlock().defaultBlockState());
            sapling.shrink(1);
        }
    }

    private static Optional<Tree> treeAt(ServerLevel level, BlockPos start, Set<BlockPos> protectedBlocks, int maxLogs,
                                         Set<BlockPos> checked) {
        // Logs connected to each other, diagonals included, as branches often are
        Set<BlockPos> logs = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>(List.of(start));
        boolean valid = true;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!logs.add(pos)) continue;
            checked.add(pos);
            if (protectedBlocks.contains(pos) || logs.size() > maxLogs) {
                valid = false;
                break;
            }
            for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (!logs.contains(neighbor) && level.getBlockState(neighbor).is(BlockTags.LOGS)) {
                    queue.add(neighbor.immutable());
                }
            }
        }
        if (!valid) return Optional.empty();

        List<BlockPos> leaves = naturalLeavesAround(level, logs);
        if (leaves.isEmpty()) return Optional.empty();

        int bottomY = logs.stream().mapToInt(BlockPos::getY).min().orElse(start.getY());
        List<BlockPos> plantingSpots = logs.stream()
                .filter(pos -> pos.getY() == bottomY && level.getBlockState(pos.below()).is(BlockTags.DIRT))
                .toList();
        List<BlockPos> sortedLogs = new ArrayList<>(logs);
        sortedLogs.sort(Comparator.comparingInt((BlockPos pos) -> pos.getY()).reversed());
        return Optional.of(new Tree(sortedLogs, leaves, plantingSpots));
    }

    /**
     * The tree's own leaves: non-persistent leaves whose nearest log is one of its logs. Vanilla keeps
     * that distance in each leaf's DISTANCE property, so a leaf reached at a depth greater than its
     * DISTANCE is closer to another tree and belongs to it, and so do the leaves beyond it. Without
     * that check, connected canopies would make the whole forest canopy count as this tree's leaves.
     */
    private static List<BlockPos> naturalLeavesAround(ServerLevel level, Set<BlockPos> logs) {
        Map<BlockPos, Integer> distances = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos log : logs) {
            distances.put(log, 0);
            queue.add(log);
        }
        List<BlockPos> leaves = new ArrayList<>();
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int distance = distances.get(pos);
            if (distance >= MAX_LEAF_DISTANCE) continue;
            for (BlockPos neighbor : List.of(pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west())) {
                if (distances.containsKey(neighbor)) continue;
                BlockState state = level.getBlockState(neighbor);
                if (!isNaturalLeaves(state) || !state.hasProperty(LeavesBlock.DISTANCE)
                        || state.getValue(LeavesBlock.DISTANCE) < distance + 1) continue;
                distances.put(neighbor, distance + 1);
                leaves.add(neighbor);
                queue.add(neighbor);
            }
        }
        return leaves;
    }

    private static boolean isNaturalLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES)
                && state.hasProperty(LeavesBlock.PERSISTENT)
                && !state.getValue(LeavesBlock.PERSISTENT);
    }
}
