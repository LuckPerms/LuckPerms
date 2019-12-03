package me.lucko.luckperms.common.actionlog;

import com.google.gson.JsonObject;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import net.luckperms.api.actionlog.Action;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

public class ActionJsonSerializerTest {

    private @NonNull LoggedAction action;

    @Before
    public void setup() {
        this.action = new LoggedAction.Builder()
                .source(UUID.randomUUID())
                .sourceName("Admin")
                .target(UUID.randomUUID())
                .targetName("Player")
                .targetType(Action.Target.Type.USER)
                .description("desc")
                .build();
    }

    @Test
    public void testSerialization() {
        String serialized = GsonProvider.normal().toJson(
                ActionJsonSerializer.serialize(this.action)
        );

        JsonObject jsonObj = GsonProvider.normal()
                .fromJson(serialized, JsonObject.class)
                .getAsJsonObject();

        LoggedAction loggedAction = ActionJsonSerializer.deserialize(jsonObj);

        Assert.assertEquals(this.action, loggedAction);
    }

}
