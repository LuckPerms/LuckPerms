package me.lucko.luckperms.utils;

import lombok.experimental.UtilityClass;
import me.lucko.luckperms.api.Logger;

@UtilityClass
public class LogUtil {
    public static Logger wrap(org.slf4j.Logger l) {
        return new Logger() {
            private final org.slf4j.Logger logger = l;

            @Override
            public void info(String s) {
                logger.info(s);
            }

            @Override
            public void warn(String s) {
                logger.warn(s);
            }

            @Override
            public void severe(String s) {
                logger.error(s);
            }
        };
    }

    public static Logger wrap(java.util.logging.Logger l) {
        return new Logger() {
            private final java.util.logging.Logger logger = l;

            @Override
            public void info(String s) {
                logger.info(s);
            }

            @Override
            public void warn(String s) {
                logger.warning(s);
            }

            @Override
            public void severe(String s) {
                logger.severe(s);
            }
        };
    }
}
