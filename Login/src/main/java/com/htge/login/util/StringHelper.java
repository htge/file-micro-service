package com.htge.login.util;

public class StringHelper {
    public static void replaceString(StringBuilder builder, String from, String to) {
        int index = 0;
        while (true) {
            index = builder.indexOf(from, index);
            if (index < 0 || index >= builder.length()) {
                break;
            }
            builder.replace(index, index+from.length(), to);
        }
    }
}
