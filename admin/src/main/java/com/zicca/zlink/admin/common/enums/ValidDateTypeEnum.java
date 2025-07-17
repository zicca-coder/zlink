package com.zicca.zlink.admin.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zicca.zlink.framework.execption.ClientException;

import java.util.Arrays;

public enum ValidDateTypeEnum implements IEnum<Integer> {

    // 永久有效
    PERMANENT("永久有效", 0),
    CUSTOM("自定义", 1),
    ;
    @JsonValue
    private String desc;
    @EnumValue
    private Integer code;

    ValidDateTypeEnum(String desc, Integer code) {
        this.desc = desc;
        this.code = code;
    }

    @Override
    public Integer getValue() {
        return code;
    }

    public String getDes() {
        return desc;
    }

    public static ValidDateTypeEnum formCode(Integer code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst().orElseThrow(() -> new ClientException("无效的【有效期类型】码：" + code));
    }

}
