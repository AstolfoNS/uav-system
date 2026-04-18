package com.tf.backend.core.common.util;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class NumberUtils {

    public static long random(long begin, long end) {
        return ThreadLocalRandom.current().nextLong(begin, end + 1);
    }

    public static long randomFromZero(long end) {
        return ThreadLocalRandom.current().nextLong(end + 1);
    }

    public static Long praseLong(String string) {
        return Objects.isNull(string) ? null : Long.parseLong(string);
    }

}
