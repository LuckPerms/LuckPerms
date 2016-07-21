package me.lucko.luckperms.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Patterns {
    public static final Pattern SERVER_SPLIT = Pattern.compile("\\/");
    public static final Pattern TEMP_SPLIT = Pattern.compile("\\$");
    public static final Pattern DOT_SPLIT = Pattern.compile("\\.");
    public static final Pattern GROUP_MATCH = Pattern.compile("group\\..*");
    public static final Pattern NON_ALPHA_NUMERIC = Pattern.compile("[^A-Za-z0-9]");
    public static final Pattern NON_USERNAME = Pattern.compile("[^A-Za-z0-9_]");

}
