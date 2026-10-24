package com.oliver.erydonthemelios.compat.jei;

import java.util.List;

/** Information-page selection without loading JEI or another item browser. */
public enum ThemeliosJeiInfo {
    SLABS("slabs", "_slab"),
    STAIRS("stairs", "_stairs"),
    WALLS("walls", "_wall"),
    LAYERS("layers", "_layer", "_layer_vertical"),
    SLICES_AND_POSTS("slices_and_posts", "_slice_horizontal", "_slice_vertical", "_post"),
    CYLINDERS("cylinders", "_cylinder_small", "_cylinder_large"),
    CORNICES("cornices", "_cornice_small", "_cornice_large"),
    WEAVES("weaves");

    private final String id;
    private final List<String> suffixes;

    ThemeliosJeiInfo(String id, String... suffixes) {
        this.id = id;
        this.suffixes = List.of(suffixes);
    }

    public String translationKey() {
        return "jei_info.themelios." + id;
    }

    public boolean matches(String canonicalPath) {
        if (this == WEAVES) {
            return canonicalPath.contains("_weave_");
        }
        for (String suffix : suffixes) {
            if (canonicalPath.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
