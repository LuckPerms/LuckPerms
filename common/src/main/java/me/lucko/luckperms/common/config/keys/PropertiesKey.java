package me.lucko.luckperms.common.config.keys;

import me.lucko.luckperms.common.config.ConfigKey;
import me.lucko.luckperms.common.config.adapter.ConfigurationAdapter;

import java.util.Map;
import java.util.Properties;

public class PropertiesKey implements ConfigKey<Properties> {
    public static PropertiesKey of(String path, Properties def) {
        return new PropertiesKey(path, def);
    }

    private final String path;
    private final Properties def;

    private PropertiesKey(String path, Properties def) {
        this.path = path;
        this.def = def;
    }

    @Override
    public Properties get(ConfigurationAdapter adapter) {
        Map<String, String> map = adapter.getMap(path, null);
        Properties props = new Properties();
        if(map != null) {
            props.putAll(map);
            return props;
        }
        return def;
    }
}
