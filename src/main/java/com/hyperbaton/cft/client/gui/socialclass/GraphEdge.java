package com.hyperbaton.cft.client.gui.socialclass;

public record GraphEdge(
        SocialClassNode from,
        SocialClassNode to,
        boolean isUpgrade
) {
}
