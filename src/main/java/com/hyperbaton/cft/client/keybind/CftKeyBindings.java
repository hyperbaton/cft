package com.hyperbaton.cft.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class CftKeyBindings {
    public static final String CATEGORY = "key.categories.cft";

    public static final KeyMapping OPEN_SOCIAL_CLASS_BROWSER = new KeyMapping(
            "key.cft.open_social_browser",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );
}
