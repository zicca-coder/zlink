package com.zicca.zlink.framework.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 正确返回码
     */
    public static final String SUCCESS_CODE = "0";

    /**
     * 返回码
     */
    @Schema(description = "返回码", example = "200")
    private String code;

    /**
     * 返回消息
     */
    @Schema(description = "返回消息", example = "成功")
    private String message;

    /**
     * 响应数据
     */
    @Schema(description = "响应数据")
    private T data;

    /**
     * 请求ID
     */
    @Schema(description = "请求ID", example = "1234567890")
    private String requestId;

    @Schema(description = "是否成功")
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }

    @Schema(description = "是否失败")
    public boolean isFail() {
        return !isSuccess();
    }

}
