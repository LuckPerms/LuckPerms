package me.lucko.luckperms.common.bulkupdate;

import me.lucko.luckperms.common.filter.FilterField;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.node.Node;

import java.util.Locale;

/**
 * Represents a field being used in a bulk update
 */
public enum BulkUpdateField implements FilterField<Node, String> {

    PERMISSION {
        @Override
        public String getValue(Node node) {
            return node.getKey();
        }
    },

    SERVER {
        @Override
        public String getValue(Node node) {
            return node.getContexts().getAnyValue(DefaultContextKeys.SERVER_KEY).orElse("global");
        }
    },

    WORLD {
        @Override
        public String getValue(Node node) {
            return node.getContexts().getAnyValue(DefaultContextKeys.WORLD_KEY).orElse("global");
        }
    };

    public static BulkUpdateField of(String s) {
        try {
            return valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
