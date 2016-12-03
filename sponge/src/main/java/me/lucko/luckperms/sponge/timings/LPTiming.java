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

package me.lucko.luckperms.sponge.timings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LPTiming {

    GET_SUBJECTS("getSubjects"),

    USER_COLLECTION_GET("userCollectionGet"),
    GROUP_COLLECTION_GET("groupCollectionGet"),

    USER_GET_PERMISSION_VALUE("userGetPermissionValue"),
    USER_GET_PARENTS("userGetParents"),
    USER_IS_CHILD_OF("userIsChildOf"),
    USER_GET_OPTION("userGetOption"),
    USER_GET_ACTIVE_CONTEXTS("userGetActiveContexts"),

    GROUP_GET_PERMISSION_VALUE("groupGetPermissionValue"),
    GROUP_GET_PARENTS("groupGetParents"),
    GROUP_IS_CHILD_OF("groupIsChildOf"),
    GROUP_GET_OPTION("groupGetOption"),
    GROUP_GET_ACTIVE_CONTEXTS("groupGetActiveContexts"),

    LP_SUBJECT_GET_PERMISSIONS("lpSubjectGetPermissions"),
    LP_SUBJECT_SET_PERMISSION("lpSubjectSetPermission"),
    LP_SUBJECT_CLEAR_PERMISSIONS("lpSubjectClearPermissions"),
    LP_SUBJECT_GET_PARENTS("lpSubjectGetParents"),
    LP_SUBJECT_ADD_PARENT("lpSubjectAddParent"),
    LP_SUBJECT_REMOVE_PARENT("lpSubjectRemoveParent"),
    LP_SUBJECT_CLEAR_PARENTS("lpSubjectClearParents"),
    LP_SUBJECT_GET_OPTIONS("lpSubjectGetOptions"),
    LP_SUBJECT_SET_OPTION("lpSubjectSetOption"),
    LP_SUBJECT_CLEAR_OPTIONS("lpSubjectClearOptions"),

    INTERNAL_SUBJECT_GET_PERMISSION_VALUE("internalSubjectGetPermissionValue"),
    INTERNAL_SUBJECT_IS_CHILD_OF("internalSubjectIsChildOf"),
    INTERNAL_SUBJECT_GET_PARENTS("internalSubjectGetParents"),
    INTERNAL_SUBJECT_GET_OPTION("internalSubjectGetOption"),
    INTERNAL_SUBJECT_GET_ACTIVE_CONTEXTS("internalSubjectGetActiveContexts"),

    ON_COMMAND("onCommand"),
    COMMAND_TAB_COMPLETE("commandTabComplete"),
    ON_CLIENT_LOGIN("onClientLogin"),
    ON_CLIENT_LEAVE("onClientLeave");

    private final String id;

}
