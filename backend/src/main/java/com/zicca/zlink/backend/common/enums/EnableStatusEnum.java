package com.zicca.zlink.backend.common.enums;


import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zicca.zlink.framework.execption.ClientException;

import java.util.Arrays;

public enum EnableStatusEnum implements IEnum<Integer> {

    ENABLE("启用", 0),
    DISABLE("禁用", 1);

    @JsonValue
    private String des;
    @EnumValue
    private Integer code;

    EnableStatusEnum(String des, Integer code) {
        this.des = des;
        this.code = code;
    }

    @Override
    public Integer getValue() {
        return code;
    }

    public String getDes() {
        return des;
    }

    public static EnableStatusEnum formCode(Integer code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst().orElseThrow(() -> new ClientException("无效的【启用状态】码：" + code));
    }
}
