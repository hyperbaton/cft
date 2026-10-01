package com.hyperbaton.cft.client.gui.socialclass;

import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.socialclass.SocialClassUpdate;
import com.hyperbaton.cft.util.LangUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class GraphLayoutEngine {

    private static final int NODE_HEIGHT = 30;
    private static final int NODE_PADDING_X = 10;
    private static final int LAYER_SPACING = 50;
    private static final int NODE_SPACING = 15;
    private static final int COMPONENT_SPACING = 40;
    private static final int MARGIN = 20;

    public record LayoutResult(
            List<SocialClassNode> nodes,
            List<GraphEdge> edges,
            int contentWidth,
            int contentHeight
    ) {}

    /**
     * @param classById the social classes by their id, in the order they should be laid out
     */
    public static LayoutResult computeLayout(Map<ResourceLocation, SocialClass> classById, Font font, int panelWidth, int panelHeight) {
        if (classById.isEmpty()) {
            return new LayoutResult(List.of(), List.of(), 0, 0);
        }

        // Build adjacency from upgrade edges
        Map<ResourceLocation, Set<ResourceLocation>> upgradeTargets = new HashMap<>();
        Map<ResourceLocation, Set<ResourceLocation>> upgradeSourcesOf = new HashMap<>();
        // Undirected adjacency for component detection (includes both upgrades and downgrades)
        Map<ResourceLocation, Set<ResourceLocation>> undirectedAdj = new HashMap<>();
        for (ResourceLocation id : classById.keySet()) {
            upgradeTargets.put(id, new HashSet<>());
            upgradeSourcesOf.put(id, new HashSet<>());
            undirectedAdj.put(id, new HashSet<>());
        }
        for (Map.Entry<ResourceLocation, SocialClass> entry : classById.entrySet()) {
            ResourceLocation id = entry.getKey();
            for (SocialClassUpdate upgrade : entry.getValue().getUpgrades()) {
                ResourceLocation targetId = upgrade.getNextClass();
                if (classById.containsKey(targetId)) {
                    upgradeTargets.get(id).add(targetId);
                    upgradeSourcesOf.get(targetId).add(id);
                    undirectedAdj.get(id).add(targetId);
                    undirectedAdj.get(targetId).add(id);
                }
            }
            for (SocialClassUpdate downgrade : entry.getValue().getDowngrades()) {
                ResourceLocation targetId = downgrade.getNextClass();
                if (classById.containsKey(targetId)) {
                    undirectedAdj.get(id).add(targetId);
                    undirectedAdj.get(targetId).add(id);
                }
            }
        }

        // Find connected components
        List<List<ResourceLocation>> components = findConnectedComponents(classById.keySet(), undirectedAdj);

        // Compute display names and node widths
        Map<ResourceLocation, String> displayNames = new HashMap<>();
        Map<ResourceLocation, Integer> nodeWidths = new HashMap<>();
        for (ResourceLocation id : classById.keySet()) {
            String name = LangUtil.socialClassName(id).getString();
            displayNames.put(id, name);
            nodeWidths.put(id, font.width(name) + NODE_PADDING_X * 2);
        }

        // Lay out each component independently, then place side by side
        int globalMaxLayer = 0;
        List<ComponentLayout> componentLayouts = new ArrayList<>();

        for (List<ResourceLocation> component : components) {
            Map<ResourceLocation, Integer> layerAssignment = assignLayers(component, upgradeTargets, upgradeSourcesOf);

            int maxLayer = layerAssignment.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            globalMaxLayer = Math.max(globalMaxLayer, maxLayer);

            List<List<ResourceLocation>> layers = new ArrayList<>();
            for (int i = 0; i <= maxLayer; i++) {
                layers.add(new ArrayList<>());
            }
            for (Map.Entry<ResourceLocation, Integer> entry : layerAssignment.entrySet()) {
                layers.get(entry.getValue()).add(entry.getKey());
            }

            orderWithinLayers(layers, upgradeTargets, upgradeSourcesOf);

            // Compute width of this component
            int compWidth = 0;
            for (List<ResourceLocation> layer : layers) {
                int layerWidth = 0;
                for (ResourceLocation id : layer) {
                    layerWidth += nodeWidths.get(id);
                }
                layerWidth += Math.max(0, layer.size() - 1) * NODE_SPACING;
                compWidth = Math.max(compWidth, layerWidth);
            }

            componentLayouts.add(new ComponentLayout(layers, maxLayer, compWidth));
        }

        // Compute total width and position each component
        int totalComponentsWidth = 0;
        for (ComponentLayout cl : componentLayouts) {
            totalComponentsWidth += cl.width;
        }
        totalComponentsWidth += Math.max(0, componentLayouts.size() - 1) * COMPONENT_SPACING;

        int totalLayerHeight = (globalMaxLayer + 1) * (NODE_HEIGHT + LAYER_SPACING) - LAYER_SPACING + MARGIN * 2;
        int contentHeight = Math.max(totalLayerHeight, panelHeight);

        Map<ResourceLocation, SocialClassNode> nodeMap = new HashMap<>();
        int contentWidth = 0;
        int componentStartX = Math.max(MARGIN, (panelWidth - totalComponentsWidth) / 2);

        for (ComponentLayout cl : componentLayouts) {
            // Pad layers to globalMaxLayer so all components align vertically
            while (cl.layers.size() <= globalMaxLayer) {
                cl.layers.add(new ArrayList<>());
            }

            for (int layerIdx = 0; layerIdx <= globalMaxLayer; layerIdx++) {
                List<ResourceLocation> layer = cl.layers.get(layerIdx);
                if (layer.isEmpty()) continue;

                int layerWidth = 0;
                for (ResourceLocation id : layer) {
                    layerWidth += nodeWidths.get(id);
                }
                layerWidth += (layer.size() - 1) * NODE_SPACING;

                // Center this layer within the component's column
                int layerStartX = componentStartX + (cl.width - layerWidth) / 2;
                int y = contentHeight - MARGIN - NODE_HEIGHT - layerIdx * (NODE_HEIGHT + LAYER_SPACING);

                int x = layerStartX;
                for (ResourceLocation id : layer) {
                    int w = nodeWidths.get(id);
                    SocialClassNode node = new SocialClassNode(
                            id, classById.get(id), x, y, w, NODE_HEIGHT, displayNames.get(id)
                    );
                    nodeMap.put(id, node);
                    x += w + NODE_SPACING;
                }
                contentWidth = Math.max(contentWidth, x + MARGIN);
            }

            componentStartX += cl.width + COMPONENT_SPACING;
        }

        // Build edge list
        List<GraphEdge> edgeList = new ArrayList<>();
        for (Map.Entry<ResourceLocation, SocialClass> entry : classById.entrySet()) {
            SocialClassNode fromNode = nodeMap.get(entry.getKey());
            if (fromNode == null) continue;

            for (SocialClassUpdate upgrade : entry.getValue().getUpgrades()) {
                SocialClassNode toNode = nodeMap.get(upgrade.getNextClass());
                if (toNode != null) {
                    edgeList.add(new GraphEdge(fromNode, toNode, true));
                }
            }
            for (SocialClassUpdate downgrade : entry.getValue().getDowngrades()) {
                SocialClassNode toNode = nodeMap.get(downgrade.getNextClass());
                if (toNode != null) {
                    edgeList.add(new GraphEdge(fromNode, toNode, false));
                }
            }
        }

        List<SocialClassNode> nodeList = new ArrayList<>(nodeMap.values());
        return new LayoutResult(nodeList, edgeList, contentWidth, contentHeight);
    }

    private static List<List<ResourceLocation>> findConnectedComponents(
            Collection<ResourceLocation> classIds,
            Map<ResourceLocation, Set<ResourceLocation>> undirectedAdj
    ) {
        Set<ResourceLocation> visited = new HashSet<>();
        List<List<ResourceLocation>> components = new ArrayList<>();

        for (ResourceLocation id : classIds) {
            if (visited.contains(id)) continue;

            List<ResourceLocation> component = new ArrayList<>();
            Queue<ResourceLocation> queue = new LinkedList<>();
            queue.add(id);
            visited.add(id);

            while (!queue.isEmpty()) {
                ResourceLocation current = queue.poll();
                component.add(current);
                for (ResourceLocation neighbor : undirectedAdj.get(current)) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }

            components.add(component);
        }

        // Sort components by size descending so the largest hierarchy is on the left
        components.sort((a, b) -> Integer.compare(b.size(), a.size()));
        return components;
    }

    private static Map<ResourceLocation, Integer> assignLayers(
            List<ResourceLocation> classIds,
            Map<ResourceLocation, Set<ResourceLocation>> upgradeTargets,
            Map<ResourceLocation, Set<ResourceLocation>> upgradeSourcesOf
    ) {
        Map<ResourceLocation, Integer> layers = new HashMap<>();

        Set<ResourceLocation> roots = new HashSet<>();
        for (ResourceLocation id : classIds) {
            if (upgradeSourcesOf.get(id).isEmpty()) {
                roots.add(id);
            }
        }

        if (roots.isEmpty()) {
            ResourceLocation minIncoming = classIds.get(0);
            int minCount = Integer.MAX_VALUE;
            for (ResourceLocation id : classIds) {
                int count = upgradeSourcesOf.get(id).size();
                if (count < minCount) {
                    minCount = count;
                    minIncoming = id;
                }
            }
            roots.add(minIncoming);
        }

        for (ResourceLocation id : classIds) {
            layers.put(id, -1);
        }

        Queue<ResourceLocation> queue = new LinkedList<>(roots);
        for (ResourceLocation root : roots) {
            layers.put(root, 0);
        }

        while (!queue.isEmpty()) {
            ResourceLocation current = queue.poll();
            int currentLayer = layers.get(current);

            for (ResourceLocation target : upgradeTargets.get(current)) {
                if (layers.containsKey(target) && layers.get(target) < currentLayer + 1) {
                    layers.put(target, currentLayer + 1);
                    queue.add(target);
                }
            }
        }

        for (Map.Entry<ResourceLocation, Integer> entry : layers.entrySet()) {
            if (entry.getValue() < 0) {
                entry.setValue(0);
            }
        }

        return layers;
    }

    private static void orderWithinLayers(
            List<List<ResourceLocation>> layers,
            Map<ResourceLocation, Set<ResourceLocation>> upgradeTargets,
            Map<ResourceLocation, Set<ResourceLocation>> upgradeSourcesOf
    ) {
        Map<ResourceLocation, Integer> positionInLayer = new HashMap<>();
        for (List<ResourceLocation> layer : layers) {
            for (int i = 0; i < layer.size(); i++) {
                positionInLayer.put(layer.get(i), i);
            }
        }

        for (int pass = 0; pass < 3; pass++) {
            for (int layerIdx = layers.size() - 1; layerIdx >= 0; layerIdx--) {
                sortLayerByBarycenter(layers.get(layerIdx), upgradeTargets, positionInLayer);
                updatePositions(layers.get(layerIdx), positionInLayer);
            }

            for (int layerIdx = 0; layerIdx < layers.size(); layerIdx++) {
                sortLayerByBarycenter(layers.get(layerIdx), upgradeSourcesOf, positionInLayer);
                updatePositions(layers.get(layerIdx), positionInLayer);
            }
        }
    }

    private static void sortLayerByBarycenter(
            List<ResourceLocation> layer,
            Map<ResourceLocation, Set<ResourceLocation>> adjacency,
            Map<ResourceLocation, Integer> positionInLayer
    ) {
        Map<ResourceLocation, Double> barycenters = new HashMap<>();
        for (ResourceLocation nodeId : layer) {
            Set<ResourceLocation> neighbors = adjacency.get(nodeId);
            if (neighbors != null && !neighbors.isEmpty()) {
                double sum = 0;
                int count = 0;
                for (ResourceLocation neighbor : neighbors) {
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

    private static void updatePositions(List<ResourceLocation> layer, Map<ResourceLocation, Integer> positionInLayer) {
        for (int i = 0; i < layer.size(); i++) {
            positionInLayer.put(layer.get(i), i);
        }
    }

    private static class ComponentLayout {
        final List<List<ResourceLocation>> layers;
        final int maxLayer;
        final int width;

        ComponentLayout(List<List<ResourceLocation>> layers, int maxLayer, int width) {
            this.layers = layers;
            this.maxLayer = maxLayer;
            this.width = width;
        }
    }
}
