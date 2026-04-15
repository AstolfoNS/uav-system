package com.tf.backend.core.common.constant;

public interface RedisConst {

    long DEFAULT_TTL_HOURS = 1;

    long NULL_CACHE_TTL_MINUTES = 5;

    String NULL_CACHE_PREFIX = "NULL_";

    /**
     * 空集合/空列表的旁路 markerKey
     */
    String EMPTY_MARKER_SUFFIX = ":__EMPTY__";

}
