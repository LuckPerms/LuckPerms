package me.lucko.luckperms.utils;

public interface LPConfiguration {

    String getServer();
    int getSyncTime();
    String getDefaultGroupNode();
    String getDefaultGroupName();
    boolean getIncludeGlobalPerms();
    String getDatabaseValue(String value);
    String getStorageMethod();

}
