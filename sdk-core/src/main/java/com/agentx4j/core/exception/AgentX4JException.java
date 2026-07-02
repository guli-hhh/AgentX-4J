package com.agentx4j.core.exception;

/**
 * AgentX-4J 基础异常。
 *
 * <p>所有 SDK 异常的基类。</p>
 */
public class AgentX4JException extends RuntimeException {

    /** 错误码 */
    private final String errorCode;

    public AgentX4JException(String message) {
        super(message);
        this.errorCode = "AGENTX4J_ERROR";
    }

    public AgentX4JException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AgentX4JException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "AGENTX4J_ERROR";
    }

    public AgentX4JException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
