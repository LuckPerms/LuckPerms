package me.lucko.luckperms.common.utils;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.stream.Collectors;

@UtilityClass
public class TextUtils {

    public String joinNewline(String... strings) {
        return Arrays.stream(strings).collect(Collectors.joining("\n"));
    }

}
