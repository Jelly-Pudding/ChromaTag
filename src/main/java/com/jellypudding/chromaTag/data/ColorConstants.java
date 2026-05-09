package com.jellypudding.chromaTag.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ColorConstants {

    private ColorConstants() {}

    public static final Map<String, String> NAMED_COLORS;
    public static final List<String> COLOR_NAMES;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("black",        "#000000");
        map.put("dark_blue",    "#0000AA");
        map.put("dark_green",   "#00AA00");
        map.put("dark_aqua",    "#00AAAA");
        map.put("dark_red",     "#AA0000");
        map.put("dark_purple",  "#AA00AA");
        map.put("gold",         "#FFAA00");
        map.put("gray",         "#AAAAAA");
        map.put("dark_gray",    "#555555");
        map.put("blue",         "#5555FF");
        map.put("green",        "#55FF55");
        map.put("aqua",         "#55FFFF");
        map.put("red",          "#FF5555");
        map.put("light_purple", "#FF55FF");
        map.put("yellow",       "#FFFF55");
        map.put("white",        "#FFFFFF");
        NAMED_COLORS = Collections.unmodifiableMap(map);
        COLOR_NAMES  = List.copyOf(map.keySet());
    }
}
