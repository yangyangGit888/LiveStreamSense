package com.yangyang.demo_01.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 捕获 Controller 层抛出的所有未处理异常，返回统一的 JSON 格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获所有 Exception 类型的异常
     * 
     * @param e 异常对象
     * @return 包含错误信息的 Map
     */
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleException(Exception e) {
        log.error("系统发生未处理异常: ", e);
        Map<String, Object> res = new HashMap<>();
        res.put("code", 500);
        res.put("message", "系统内部错误: " + e.getMessage());
        return res;
    }
}
