/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.config;

import me.lucko.luckperms.common.defaults.Rule;
import me.lucko.luckperms.common.storage.DatastoreConfiguration;

import java.util.List;
import java.util.Map;

public interface LPConfiguration {

    String getServer();

    int getSyncTime();

    /**
     * As of 2.6, this value is a constant
     * @return the default group node
     */
    String getDefaultGroupNode();

    /**
     * As of 2.6, this value is a constant
     * @return the name of the default group
     */
    String getDefaultGroupName();

    boolean isIncludingGlobalPerms();

    boolean isIncludingGlobalWorldPerms();

    boolean isApplyingGlobalGroups();

    boolean isApplyingGlobalWorldGroups();

    boolean isOnlineMode();

    boolean isApplyingWildcards();

    boolean isApplyingRegex();

    boolean isApplyingShorthand();

    boolean isLogNotify();

    boolean isOpsEnabled();

    boolean isCommandsAllowOp();

    boolean isAutoOp();

    String getVaultServer();

    boolean isVaultIncludingGlobal();

    boolean isVaultIgnoreWorld();

    boolean isVaultPrimaryGroupOverrides();

    boolean isVaultPrimaryGroupOverridesCheckInherited();

    boolean isVaultPrimaryGroupOverridesCheckExists();

    boolean isVaultPrimaryGroupOverridesCheckMemberOf();

    boolean isVaultDebug();

    Map<String, String> getWorldRewrites();

    Map<String, String> getGroupNameRewrites();

    List<Rule> getDefaultAssignments();

    DatastoreConfiguration getDatabaseValues();

    String getStorageMethod();

    boolean isSplitStorage();

    Map<String, String> getSplitStorageOptions();

    boolean isRedisEnabled();

    String getRedisAddress();

    String getRedisPassword();

}
