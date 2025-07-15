package com.zicca.zlink.backend.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zicca.zlink.framework.execption.ClientException;

import java.io.Serializable;
import java.util.Arrays;

public enum CreateTypeEnum implements IEnum<Integer> {

    INTERFACE_CREATE("接口创建", 0),
    CONSOLE_CREATE("控制台创建", 1)
    ;
    @JsonValue
    private String desc;
    @EnumValue
    private Integer code;

    CreateTypeEnum(String desc, Integer code) {
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

    public static CreateTypeEnum formCode(Integer code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst().orElseThrow(() -> new ClientException("无效的【创建类型】码：" + code));
    }
}
