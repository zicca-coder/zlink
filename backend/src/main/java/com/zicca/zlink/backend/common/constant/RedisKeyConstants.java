package com.zicca.zlink.backend.common.constant;

import java.time.Duration;

public class RedisKeyConstants {

    public static final String CACHE_PREFIX = "zlink:cache:";

    public static final String BLOOM_FILTER_PREFIX = "zlink:bloom_filter:";

    public static final String BLOOM_FILTER_NAME = BLOOM_FILTER_PREFIX + "zlink_bloom_filter";

    public static final String BLOOM_FILTER_KEY = CACHE_PREFIX + "bloom_filter";

    public static final String LINK_CACHE_KEY = CACHE_PREFIX + "link_cache:";

    public static final String COUNT_CACHE_KEY = CACHE_PREFIX + "count_cache:";

    public static final Duration NORAML_EXPIRE_TIME = Duration.ofHours(1);

    public static final Duration HOT_EXPIRE_TIME = Duration.ofDays(24);

    public static final String LINK_NOT_EXIST_VALUE = "-1";

    public static final long DEFAULT_CACHE_VALID_TIME = 2626560000L;

    public static final String LINK_LOCK_KEY_PREFIX = "zlink:lock:link_lock:";

}
