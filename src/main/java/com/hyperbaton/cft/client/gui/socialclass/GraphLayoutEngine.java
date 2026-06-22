package com.hyperbaton.cft.client.gui.socialclass;

import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.socialclass.SocialClassUpdate;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

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

    public static LayoutResult computeLayout(List<SocialClass> classes, Font font, int panelWidth, int panelHeight) {
        if (classes.isEmpty()) {
            return new LayoutResult(List.of(), List.of(), 0, 0);
        }

        Map<String, SocialClass> classById = new LinkedHashMap<>();
        for (SocialClass sc : classes) {
            classById.put(sc.getId(), sc);
        }

        // Build adjacency from upgrade edges
        Map<String, Set<String>> upgradeTargets = new HashMap<>();
        Map<String, Set<String>> upgradeSourcesOf = new HashMap<>();
        // Undirected adjacency for component detection (includes both upgrades and downgrades)
        Map<String, Set<String>> undirectedAdj = new HashMap<>();
        for (SocialClass sc : classes) {
            upgradeTargets.put(sc.getId(), new HashSet<>());
            upgradeSourcesOf.put(sc.getId(), new HashSet<>());
            undirectedAdj.put(sc.getId(), new HashSet<>());
        }
        for (SocialClass sc : classes) {
            for (SocialClassUpdate upgrade : sc.getUpgrades()) {
                String targetId = upgrade.getNextClass();
                if (classById.containsKey(targetId)) {
                    upgradeTargets.get(sc.getId()).add(targetId);
                    upgradeSourcesOf.get(targetId).add(sc.getId());
                    undirectedAdj.get(sc.getId()).add(targetId);
                    undirectedAdj.get(targetId).add(sc.getId());
                }
            }
            for (SocialClassUpdate downgrade : sc.getDowngrades()) {
                String targetId = downgrade.getNextClass();
                if (classById.containsKey(targetId)) {
                    undirectedAdj.get(sc.getId()).add(targetId);
                    undirectedAdj.get(targetId).add(sc.getId());
                }
            }
        }

        // Find connected components
        List<List<String>> components = findConnectedComponents(classes, undirectedAdj);

        // Compute display names and node widths
        Map<String, String> displayNames = new HashMap<>();
        Map<String, Integer> nodeWidths = new HashMap<>();
        for (SocialClass sc : classes) {
            String name = Component.translatable(sc.getId()).getString();
            displayNames.put(sc.getId(), name);
            nodeWidths.put(sc.getId(), font.width(name) + NODE_PADDING_X * 2);
        }

        // Lay out each component independently, then place side by side
        int globalMaxLayer = 0;
        List<ComponentLayout> componentLayouts = new ArrayList<>();

        for (List<String> component : components) {
            List<SocialClass> compClasses = component.stream().map(classById::get).toList();
            Map<String, Integer> layerAssignment = assignLayers(compClasses, upgradeTargets, upgradeSourcesOf);

            int maxLayer = layerAssignment.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            globalMaxLayer = Math.max(globalMaxLayer, maxLayer);

            List<List<String>> layers = new ArrayList<>();
            for (int i = 0; i <= maxLayer; i++) {
                layers.add(new ArrayList<>());
            }
            for (Map.Entry<String, Integer> entry : layerAssignment.entrySet()) {
                layers.get(entry.getValue()).add(entry.getKey());
            }

            orderWithinLayers(layers, upgradeTargets, upgradeSourcesOf);

            // Compute width of this component
            int compWidth = 0;
            for (List<String> layer : layers) {
                int layerWidth = 0;
                for (String id : layer) {
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

        Map<String, SocialClassNode> nodeMap = new HashMap<>();
        int contentWidth = 0;
        int componentStartX = Math.max(MARGIN, (panelWidth - totalComponentsWidth) / 2);

        for (ComponentLayout cl : componentLayouts) {
            // Pad layers to globalMaxLayer so all components align vertically
            while (cl.layers.size() <= globalMaxLayer) {
                cl.layers.add(new ArrayList<>());
            }

            for (int layerIdx = 0; layerIdx <= globalMaxLayer; layerIdx++) {
                List<String> layer = cl.layers.get(layerIdx);
                if (layer.isEmpty()) continue;

                int layerWidth = 0;
                for (String id : layer) {
                    layerWidth += nodeWidths.get(id);
                }
                layerWidth += (layer.size() - 1) * NODE_SPACING;

                // Center this layer within the component's column
                int layerStartX = componentStartX + (cl.width - layerWidth) / 2;
                int y = contentHeight - MARGIN - NODE_HEIGHT - layerIdx * (NODE_HEIGHT + LAYER_SPACING);

                int x = layerStartX;
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

            componentStartX += cl.width + COMPONENT_SPACING;
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

    private static List<List<String>> findConnectedComponents(
            List<SocialClass> classes,
            Map<String, Set<String>> undirectedAdj
    ) {
        Set<String> visited = new HashSet<>();
        List<List<String>> components = new ArrayList<>();

        for (SocialClass sc : classes) {
            String id = sc.getId();
            if (visited.contains(id)) continue;

            List<String> component = new ArrayList<>();
            Queue<String> queue = new LinkedList<>();
            queue.add(id);
            visited.add(id);

            while (!queue.isEmpty()) {
                String current = queue.poll();
                component.add(current);
                for (String neighbor : undirectedAdj.get(current)) {
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

    private static Map<String, Integer> assignLayers(
            List<SocialClass> classes,
            Map<String, Set<String>> upgradeTargets,
            Map<String, Set<String>> upgradeSourcesOf
    ) {
        Map<String, Integer> layers = new HashMap<>();

        Set<String> roots = new HashSet<>();
        for (SocialClass sc : classes) {
            if (upgradeSourcesOf.get(sc.getId()).isEmpty()) {
                roots.add(sc.getId());
            }
        }

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
                if (layers.containsKey(target) && layers.get(target) < currentLayer + 1) {
                    layers.put(target, currentLayer + 1);
                    queue.add(target);
                }
            }
        }

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
        Map<String, Integer> positionInLayer = new HashMap<>();
        for (List<String> layer : layers) {
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

    private static class ComponentLayout {
        final List<List<String>> layers;
        final int maxLayer;
        final int width;

        ComponentLayout(List<List<String>> layers, int maxLayer, int width) {
            this.layers = layers;
            this.maxLayer = maxLayer;
            this.width = width;
        }
    }
}
