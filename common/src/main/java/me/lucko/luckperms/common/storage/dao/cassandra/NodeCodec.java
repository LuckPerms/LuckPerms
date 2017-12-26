package me.lucko.luckperms.common.storage.dao.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.InvalidTypeException;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.node.NodeModel;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Map;

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
        Map<String, String> contexts = value.getMap("contexts", String.class, String.class);
        // Lucko, i don't like this shit.
        return NodeModel.of(permission, enabled, server, world, expiry.getTime() / 1000L, ImmutableContextSet.fromMap(contexts));
    }

    private UDTValue toUDTValue(NodeModel model) {
        if(model == null) return null;
        UDTValue udtValue = userType.newValue();
        udtValue.setString("id", model.getPermission());
        udtValue.setBool("value", model.getValue());
        udtValue.setString("server", model.getServer());
        udtValue.setString("world", model.getWorld());
        // Lucko, i don't like this shit again.
        udtValue.setTimestamp("expiry", new Date(model.getExpiry() * 1000L));
        return udtValue;
    }
}
