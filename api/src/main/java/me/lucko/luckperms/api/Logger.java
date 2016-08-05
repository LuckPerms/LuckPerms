package me.lucko.luckperms.api;

/**
 * A wrapper class for platform logger instances.
 * Bukkit/Bungee both use java.util.logging, and Sponge uses org.slf4j. This class wraps those classes so the commons
 * module can access a logger.
 */
public interface Logger {

    void info(String s);
    void warn(String s);
    void severe(String s);

}
