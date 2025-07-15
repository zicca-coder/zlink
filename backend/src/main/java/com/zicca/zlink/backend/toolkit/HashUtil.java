package com.zicca.zlink.backend.toolkit;

import cn.hutool.core.lang.hash.MurmurHash;

/**
 * Hash 工具类
 */
public class HashUtil {

    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int SIZE = CHARS.length();

    private static String convertDecToBase62(long num) {
        if (num == 0) {
            return String.valueOf(CHARS.charAt(0)); // 返回 "0"
        }

        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            int index = (int) (num % SIZE);
            sb.append(CHARS.charAt(index));
            num /= SIZE;
        }

        return sb.reverse().toString();
    }


    /**
     * 对字符串进行哈希和进制转换，生成 Base62 编码的字符串
     *
     * @param str 待转换的字符串
     * @return Base62 编码的字符串
     */
    public static String hashToBase62(String str) {
        // 对输入字符串进行哈希计算，返回一个 32 位整数
        // todo: 可扩展 hash64 和 hash128
        int i = MurmurHash.hash32(str);
        // 将可能为负数的哈希值转为正数
        long num = i < 0 ? Integer.MAX_VALUE - (long) i : i;
        return convertDecToBase62(num);
    }
}
