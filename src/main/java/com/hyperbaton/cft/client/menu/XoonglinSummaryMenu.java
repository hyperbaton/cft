package com.hyperbaton.cft.client.menu;

public class XoonglinSummaryMenu {
    private final int containerId;
    private final Player player;
    private final XoonglinData xoonglinData;

    public XoonglinSummaryMenu(int containerId, Player player, XoonglinData xoonglinData) {
        this.containerId = containerId;
        this.player = player;
        this.xoonglinData = xoonglinData;
    }

    public int getContainerId() {
        return containerId;
    }

    public Player getPlayer() {
        return player;
    }

    public XoonglinData getXoonglinData() {
        return xoonglinData;
    }
}