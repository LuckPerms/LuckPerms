package me.lucko.luckperms.utils;

public interface LPConfiguration {

    String getServer();
    String getPrefix();
    int getSyncTime();
    String getDefaultGroupNode();
    String getDefaultGroupName();
    boolean getIncludeGlobalPerms();
    String getDatabaseValue(String value);

}
