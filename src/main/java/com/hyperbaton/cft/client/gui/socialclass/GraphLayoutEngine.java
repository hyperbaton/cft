package com.hyperbaton.cft.client.gui.socialclass;

import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.socialclass.SocialClassUpdate;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.*;

public class GraphLayoutEngine {

    private static final int NODE_HEIGHT = 20;
    private static final int NODE_PADDING_X = 10;
    private static final int LAYER_SPACING = 50;
    private static final int NODE_SPACING = 15;
    private static final int MARGIN = 20;

    public record LayoutResult(
            List<SocialClassNode> nodes,
            List<GraphEdge> edges,
            int contentWidth,
            int contentHeight
    ) {}

    public static LayoutResult computeLayout(List<SocialClass> classes, Font font, int panelWidth, int panelHeight) {
        if (classes.isEmpty()) {
            return new LayoutResult(List.of(), List.of(), 0, 0);
        }

        Map<String, SocialClass> classById = new LinkedHashMap<>();
        for (SocialClass sc : classes) {
            classById.put(sc.getId(), sc);
        }

        // Build adjacency from upgrade edges (A upgrades to B means edge A → B)
        Map<String, Set<String>> upgradeTargets = new HashMap<>();
        Map<String, Set<String>> upgradeSourcesOf = new HashMap<>();
        for (SocialClass sc : classes) {
            upgradeTargets.put(sc.getId(), new HashSet<>());
            upgradeSourcesOf.put(sc.getId(), new HashSet<>());
        }
        for (SocialClass sc : classes) {
            for (SocialClassUpdate upgrade : sc.getUpgrades()) {
                String targetId = upgrade.getNextClass();
                if (classById.containsKey(targetId)) {
                    upgradeTargets.get(sc.getId()).add(targetId);
                    upgradeSourcesOf.get(targetId).add(sc.getId());
                }
            }
        }

        // Assign layers via longest path from roots
        Map<String, Integer> layerAssignment = assignLayers(classes, upgradeTargets, upgradeSourcesOf);

        // Group by layer
        int maxLayer = layerAssignment.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<List<String>> layers = new ArrayList<>();
        for (int i = 0; i <= maxLayer; i++) {
            layers.add(new ArrayList<>());
        }
        for (Map.Entry<String, Integer> entry : layerAssignment.entrySet()) {
            layers.get(entry.getValue()).add(entry.getKey());
        }

        // Order within layers using barycenter heuristic
        orderWithinLayers(layers, upgradeTargets, upgradeSourcesOf);

        // Compute display names and node widths
        Map<String, String> displayNames = new HashMap<>();
        Map<String, Integer> nodeWidths = new HashMap<>();
        for (SocialClass sc : classes) {
            String name = Component.translatable(sc.getId()).getString();
            displayNames.put(sc.getId(), name);
            nodeWidths.put(sc.getId(), font.width(name) + NODE_PADDING_X * 2);
        }

        // Compute pixel positions
        // Layers are bottom-to-top: layer 0 at the bottom (roots/starters), highest layer at the top
        int totalLayerHeight = (maxLayer + 1) * (NODE_HEIGHT + LAYER_SPACING) - LAYER_SPACING + MARGIN * 2;
        int contentHeight = Math.max(totalLayerHeight, panelHeight);

        Map<String, SocialClassNode> nodeMap = new HashMap<>();
        int contentWidth = 0;

        for (int layerIdx = 0; layerIdx <= maxLayer; layerIdx++) {
            List<String> layer = layers.get(layerIdx);
            int totalWidth = 0;
            for (String id : layer) {
                totalWidth += nodeWidths.get(id);
            }
            totalWidth += (layer.size() - 1) * NODE_SPACING;

            int startX = Math.max(MARGIN, (panelWidth - totalWidth) / 2);
            // Layer 0 at bottom, higher layers go up
            int y = contentHeight - MARGIN - NODE_HEIGHT - layerIdx * (NODE_HEIGHT + LAYER_SPACING);

            int x = startX;
            for (String id : layer) {
                int w = nodeWidths.get(id);
                SocialClassNode node = new SocialClassNode(
                        classById.get(id), x, y, w, NODE_HEIGHT, displayNames.get(id)
                );
                nodeMap.put(id, node);
                x += w + NODE_SPACING;
            }
            contentWidth = Math.max(contentWidth, x + MARGIN);
        }

        // Build edge list
        List<GraphEdge> edgeList = new ArrayList<>();
        for (SocialClass sc : classes) {
            SocialClassNode fromNode = nodeMap.get(sc.getId());
            if (fromNode == null) continue;

            for (SocialClassUpdate upgrade : sc.getUpgrades()) {
                SocialClassNode toNode = nodeMap.get(upgrade.getNextClass());
                if (toNode != null) {
                    edgeList.add(new GraphEdge(fromNode, toNode, true));
                }
            }
            for (SocialClassUpdate downgrade : sc.getDowngrades()) {
                SocialClassNode toNode = nodeMap.get(downgrade.getNextClass());
                if (toNode != null) {
                    edgeList.add(new GraphEdge(fromNode, toNode, false));
                }
            }
        }

        List<SocialClassNode> nodeList = new ArrayList<>(nodeMap.values());
        return new LayoutResult(nodeList, edgeList, contentWidth, contentHeight);
    }

    private static Map<String, Integer> assignLayers(
            List<SocialClass> classes,
            Map<String, Set<String>> upgradeTargets,
            Map<String, Set<String>> upgradeSourcesOf
    ) {
        Map<String, Integer> layers = new HashMap<>();

        // Find roots: nodes with no incoming upgrade edges
        Set<String> roots = new HashSet<>();
        for (SocialClass sc : classes) {
            if (upgradeSourcesOf.get(sc.getId()).isEmpty()) {
                roots.add(sc.getId());
            }
        }

        // If no roots found (all in cycles), pick the node with the fewest incoming edges
        if (roots.isEmpty()) {
            String minIncoming = classes.get(0).getId();
            int minCount = Integer.MAX_VALUE;
            for (SocialClass sc : classes) {
                int count = upgradeSourcesOf.get(sc.getId()).size();
                if (count < minCount) {
                    minCount = count;
                    minIncoming = sc.getId();
                }
            }
            roots.add(minIncoming);
        }

        // BFS longest path from any root
        // Initialize all to -1
        for (SocialClass sc : classes) {
            layers.put(sc.getId(), -1);
        }

        Queue<String> queue = new LinkedList<>(roots);
        for (String root : roots) {
            layers.put(root, 0);
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentLayer = layers.get(current);

            for (String target : upgradeTargets.get(current)) {
                if (layers.get(target) < currentLayer + 1) {
                    layers.put(target, currentLayer + 1);
                    queue.add(target);
                }
            }
        }

        // Assign unvisited nodes (disconnected or in cycles) to layer 0
        for (Map.Entry<String, Integer> entry : layers.entrySet()) {
            if (entry.getValue() < 0) {
                entry.setValue(0);
            }
        }

        return layers;
    }

    private static void orderWithinLayers(
            List<List<String>> layers,
            Map<String, Set<String>> upgradeTargets,
            Map<String, Set<String>> upgradeSourcesOf
    ) {
        // Build position index for barycenter computation
        Map<String, Integer> positionInLayer = new HashMap<>();
        for (List<String> layer : layers) {
            for (int i = 0; i < layer.size(); i++) {
                positionInLayer.put(layer.get(i), i);
            }
        }

        // Run barycenter passes: top-down then bottom-up, repeat 3 times
        for (int pass = 0; pass < 3; pass++) {
            // Top-down (from highest layer down)
            for (int layerIdx = layers.size() - 1; layerIdx >= 0; layerIdx--) {
                sortLayerByBarycenter(layers.get(layerIdx), upgradeTargets, positionInLayer);
                updatePositions(layers.get(layerIdx), positionInLayer);
            }

            // Bottom-up (from lowest layer up)
            for (int layerIdx = 0; layerIdx < layers.size(); layerIdx++) {
                sortLayerByBarycenter(layers.get(layerIdx), upgradeSourcesOf, positionInLayer);
                updatePositions(layers.get(layerIdx), positionInLayer);
            }
        }
    }

    private static void sortLayerByBarycenter(
            List<String> layer,
            Map<String, Set<String>> adjacency,
            Map<String, Integer> positionInLayer
    ) {
        Map<String, Double> barycenters = new HashMap<>();
        for (String nodeId : layer) {
            Set<String> neighbors = adjacency.get(nodeId);
            if (neighbors != null && !neighbors.isEmpty()) {
                double sum = 0;
                int count = 0;
                for (String neighbor : neighbors) {
                    Integer pos = positionInLayer.get(neighbor);
                    if (pos != null) {
                        sum += pos;
                        count++;
                    }
                }
                barycenters.put(nodeId, count > 0 ? sum / count : positionInLayer.getOrDefault(nodeId, 0).doubleValue());
            } else {
                barycenters.put(nodeId, positionInLayer.getOrDefault(nodeId, 0).doubleValue());
            }
        }
        layer.sort(Comparator.comparingDouble(id -> barycenters.getOrDefault(id, 0.0)));
    }

    private static void updatePositions(List<String> layer, Map<String, Integer> positionInLayer) {
        for (int i = 0; i < layer.size(); i++) {
            positionInLayer.put(layer.get(i), i);
        }
    }
}
