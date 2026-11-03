package com.paimonclub.common;

/**
 * 业务异常：由全局异常映射器转换为统一响应。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String message) {
        this(Constants.CODE_BAD_REQUEST, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(Constants.CODE_BAD_REQUEST, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(Constants.CODE_UNAUTHORIZED, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(Constants.CODE_FORBIDDEN, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(Constants.CODE_NOT_FOUND, message);
    }
}
