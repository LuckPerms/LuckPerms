package me.lucko.luckperms.utils;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class Patterns {
    public static final Pattern SPACE = Pattern.compile(" ");
    public static final Pattern SERVER_DELIMITER = Pattern.compile("\\/");
    public static final Pattern WORLD_DELIMITER = Pattern.compile("\\-");
    public static final Pattern TEMP_DELIMITER = Pattern.compile("\\$");
    public static final Pattern DOT = Pattern.compile("\\.");
    public static final Pattern GROUP_MATCH = Pattern.compile("group\\..*");
    public static final Pattern NON_ALPHA_NUMERIC = Pattern.compile("[^A-Za-z0-9]");
    public static final Pattern NON_USERNAME = Pattern.compile("[^A-Za-z0-9_]");

}
