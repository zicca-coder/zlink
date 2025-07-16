package com.zicca.zlink.backend.toolkit;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.zicca.zlink.backend.common.constant.RedisKeyConstants;

import javax.swing.text.html.Option;
import java.util.Date;
import java.util.Optional;

/**
 * 短链接工具类
 */
public class LinkUtil {

    /**
     * 获取短链接缓存有效时间
     *
     * @param validDate 有效时间
     * @return 缓存有效时间
     */
    public static long getLinkCacheValidTime(Date validDate) {
        return Optional.ofNullable(validDate)
                .map(date -> DateUtil.between(new Date(), date, DateUnit.MS))
                .orElse(RedisKeyConstants.DEFAULT_CACHE_VALID_TIME);
    }


}
