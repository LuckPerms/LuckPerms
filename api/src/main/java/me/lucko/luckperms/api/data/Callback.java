package me.lucko.luckperms.api.data;

import java.util.UUID;

public interface Callback {
    void onComplete(boolean success);

    interface GetUUID {
        void onComplete(UUID uuid);
    }
}
