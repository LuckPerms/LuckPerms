package me.lucko.luckperms.commands;

import java.lang.ref.WeakReference;

public abstract class SenderFactory<T> {

    protected abstract void sendMessage(T t, String s);
    protected abstract boolean hasPermission(T t, String node);

    public Sender wrap(T t) {
        final SenderFactory<T> factory = this;
        return new Sender() {
            final WeakReference<T> cs = new WeakReference<>(t);

            @Override
            public void sendMessage(String s) {
                final T c = cs.get();
                if (c != null) {
                    factory.sendMessage(c, s);
                }
            }

            @Override
            public boolean hasPermission(String node) {
                final T c = cs.get();
                return c != null && factory.hasPermission(c, node);
            }
        };
    }
}
