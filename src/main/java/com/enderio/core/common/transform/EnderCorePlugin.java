package com.enderio.core.common.transform;

/**
 * Compatibility shim for broken EnderCore artifacts that reference EnderCorePlugin
 * from SimpleMixinLoader but do not ship the class.
 */
public final class EnderCorePlugin {
    private static final EnderCorePlugin INSTANCE = new EnderCorePlugin();

    private EnderCorePlugin() {
    }

    public static EnderCorePlugin instance() {
        return INSTANCE;
    }

    public void loadMixinSources(Object source) {
    }

    public void loadMixinSources(Package sourcePackage) {
    }

    public void loadMixinSources(String packageName) {
    }
}

