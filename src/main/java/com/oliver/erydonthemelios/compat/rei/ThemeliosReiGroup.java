package com.oliver.erydonthemelios.compat.rei;

import java.util.List;

/** Canonical shape categories shared by REI's groups and subsets. */
public enum ThemeliosReiGroup {
    SLABS("slabs", "Slabs", "_slab"),
    STAIRS("stairs", "Stairs", "_stairs"),
    WALLS("walls", "Walls", "_wall"),
    LAYERS("layers", "Layers", "_layer", "_layer_vertical"),
    SLICES_AND_POSTS("slices_and_posts", "Slices and Posts", "_slice_horizontal", "_slice_vertical", "_post"),
    CYLINDERS("cylinders", "Cylinders", "_cylinder_small", "_cylinder_large"),
    CORNICES("cornices", "Cornices", "_cornice_small", "_cornice_large");

    private final String id;
    private final String subsetName;
    private final List<String> suffixes;

    ThemeliosReiGroup(String id, String subsetName, String... suffixes) {
        this.id = id;
        this.subsetName = subsetName;
        this.suffixes = List.of(suffixes);
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "rei_group.themelios." + id;
    }

    public String subsetPath() {
        return "ERYDON Themelios/Shapes/" + subsetName;
    }

    public boolean matches(String canonicalPath) {
        for (String suffix : suffixes) {
            if (canonicalPath.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
