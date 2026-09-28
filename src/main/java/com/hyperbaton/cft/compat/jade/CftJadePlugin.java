package com.hyperbaton.cft.compat.jade;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Optional Jade integration, discovered by Jade through the annotation. Nothing else in
 * the mod references this package, so it is never loaded when Jade is absent.
 */
@WailaPlugin(CftMod.MOD_ID)
public class CftJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(XoonglinJadeProvider.INSTANCE, XoonglinEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(XoonglinJadeProvider.INSTANCE, XoonglinEntity.class);
    }
}
