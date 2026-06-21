package com.hyperbaton.cft.client.gui.socialclass;

import com.hyperbaton.cft.event.CftDatapackRegistryEvents;
import com.hyperbaton.cft.need.Need;
import com.hyperbaton.cft.socialclass.SocialClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SocialClassBrowserScreen extends Screen {

    private List<SocialClassNode> nodes = new ArrayList<>();
    private List<GraphEdge> edges = new ArrayList<>();
    private SocialClassNode selectedNode;

    private Registry<SocialClass> socialClassRegistry;
    private Registry<Need> needRegistry;

    private int graphPanelWidth;
    private int detailPanelX;
    private int detailPanelWidth;

    private SocialClassDetailPanel detailPanel;

    private double graphScrollY = 0;
    private double graphScrollX = 0;
    private int graphContentHeight;
    private int graphContentWidth;

    public SocialClassBrowserScreen() {
        super(Component.translatable("gui.cft.social_class_browser"));
    }

    @Override
    protected void init() {
        super.init();

        RegistryAccess access = Minecraft.getInstance().level.registryAccess();
        socialClassRegistry = access.registryOrThrow(CftDatapackRegistryEvents.SOCIAL_CLASS_KEY);
        needRegistry = access.registryOrThrow(CftDatapackRegistryEvents.NEED_KEY);

        graphPanelWidth = (int) (this.width * 0.55);
        detailPanelX = graphPanelWidth;
        detailPanelWidth = this.width - graphPanelWidth;

        List<SocialClass> allClasses = socialClassRegistry.stream().toList();
        GraphLayoutEngine.LayoutResult layout = GraphLayoutEngine.computeLayout(
                allClasses, this.font, graphPanelWidth, this.height
        );
        this.nodes = layout.nodes();
        this.edges = layout.edges();
        this.graphContentHeight = layout.contentHeight();
        this.graphContentWidth = layout.contentWidth();
        this.graphScrollY = 0;
        this.graphScrollX = 0;

        this.detailPanel = new SocialClassDetailPanel(
                detailPanelX, 0, detailPanelWidth, this.height, this.font, needRegistry
        );
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xDD1A1A2E);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        renderGraphPanel(graphics, mouseX, mouseY);
        renderDivider(graphics);
        detailPanel.render(graphics, mouseX, mouseY);

        Component title = Component.translatable("gui.cft.social_class_browser");
        int titleWidth = this.font.width(title);
        graphics.drawString(this.font, title, (graphPanelWidth - titleWidth) / 2, 5, 0xFFE0E0E0, true);
    }

    private void renderGraphPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.enableScissor(0, 0, graphPanelWidth, this.height);

        int offsetX = (int) -graphScrollX;
        int offsetY = (int) -graphScrollY;

        for (GraphEdge edge : edges) {
            ClassGraphRenderer.renderEdge(graphics, edge, offsetX, offsetY);
        }

        for (SocialClassNode node : nodes) {
            boolean hovered = isMouseOverNode(node, mouseX, mouseY);
            boolean selected = node == selectedNode;
            node.render(graphics, this.font, offsetX, offsetY, hovered, selected);
        }

        graphics.disableScissor();
    }

    private void renderDivider(GuiGraphics graphics) {
        graphics.fill(graphPanelWidth - 1, 0, graphPanelWidth + 1, this.height, 0xFF4A4A6A);
    }

    private boolean isMouseOverNode(SocialClassNode node, int mouseX, int mouseY) {
        int nx = node.x() + (int) -graphScrollX;
        int ny = node.y() + (int) -graphScrollY;
        return mouseX >= nx && mouseX <= nx + node.width()
                && mouseY >= ny && mouseY <= ny + node.height();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX < graphPanelWidth) {
            for (SocialClassNode node : nodes) {
                if (isMouseOverNode(node, (int) mouseX, (int) mouseY)) {
                    selectedNode = node;
                    detailPanel.setSelectedClass(node.socialClass());
                    return true;
                }
            }
            selectedNode = null;
            detailPanel.setSelectedClass(null);
            return true;
        }

        if (mouseX >= detailPanelX) {
            return detailPanel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < graphPanelWidth) {
            if (Screen.hasShiftDown()) {
                graphScrollX = Math.max(0, Math.min(graphScrollX - scrollY * 10,
                        Math.max(0, graphContentWidth - graphPanelWidth)));
            } else {
                graphScrollY = Math.max(0, Math.min(graphScrollY - scrollY * 10,
                        Math.max(0, graphContentHeight - this.height)));
            }
            return true;
        }

        if (mouseX >= detailPanelX) {
            return detailPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void tick() {
        super.tick();
        if (detailPanel != null) {
            detailPanel.tick();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
