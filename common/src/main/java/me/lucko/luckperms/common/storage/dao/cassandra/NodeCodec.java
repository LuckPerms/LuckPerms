package me.lucko.luckperms.common.storage.dao.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.InvalidTypeException;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.reflect.TypeToken;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.config.keys.StringKey;
import me.lucko.luckperms.common.node.NodeModel;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.*;

public class NodeCodec extends TypeCodec<NodeModel> {
    private final TypeCodec<UDTValue> innerCodec;
    private final UserType userType;

    protected NodeCodec(TypeCodec<UDTValue> innerCodec) {
        super(innerCodec.getCqlType(), NodeModel.class);
        this.innerCodec = innerCodec;
        this.userType = (UserType) innerCodec.getCqlType();
    }

    @Override
    public ByteBuffer serialize(NodeModel value, ProtocolVersion protocolVersion) throws InvalidTypeException {
        return innerCodec.serialize(toUDTValue(value), protocolVersion);
    }

    @Override
    public NodeModel deserialize(ByteBuffer bytes, ProtocolVersion protocolVersion) throws InvalidTypeException {
        return toInstance(innerCodec.deserialize(bytes, protocolVersion));
    }

    @Override
    public NodeModel parse(String value) throws InvalidTypeException {
        return value == null ? null : toInstance(innerCodec.parse(value));
    }

    @Override
    public String format(NodeModel value) throws InvalidTypeException {
        return value == null ? null : innerCodec.format(toUDTValue(value));
    }

    private NodeModel toInstance(UDTValue value) {
        if(value == null) return null;
        String permission = value.getString("id");
        boolean enabled = value.getBool("value");
        String server = value.getString("server");
        String world = value.getString("world");
        Date expiry = value.getTimestamp("expiry");
        Map<String, Set<String>> contexts = value.getMap("contexts", TypeToken.of(String.class), new TypeToken<Set<String>>() {});
        return NodeModel.of(permission, enabled, server, world, expiry.getTime() / 1000L, fromStringSetMap(contexts));
    }

    private UDTValue toUDTValue(NodeModel model) {
        if(model == null) return null;
        ImmutableContextSet contexts = model.getContexts();
        UDTValue udtValue = userType.newValue();
        udtValue.setString("id", model.getPermission());
        udtValue.setBool("value", model.getValue());
        udtValue.setString("server", model.getServer());
        udtValue.setString("world", model.getWorld());
        udtValue.setMap("contexts", toStringSetMap(contexts));
        udtValue.setTimestamp("expiry", new Date(model.getExpiry() * 1000L));
        return udtValue;
    }

    @Nonnull
    private static ImmutableContextSet fromStringSetMap(@Nonnull Map<String, Set<String>> map) {
        Objects.requireNonNull(map, "map");
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                builder.add(key, value);
            }
        }
        return builder.build();
    }

    @Nonnull
    public Map<String, Set<String>> toStringSetMap(ImmutableContextSet set) {
        return (Map) set.toMultimap().asMap();
    }
}
