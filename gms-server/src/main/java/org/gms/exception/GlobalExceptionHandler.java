package org.gms.exception;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.gms.model.dto.ResultBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 对于通过多个代理的情况，第一个IP为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }

        return ip;
    }

    /**
     * 获取尝试登录的账号信息
     */
    private String getAttemptedUsername(HttpServletRequest request) {
        // 尝试从请求参数中获取用户名
        String username = request.getParameter("username");
        if (username != null && !username.isEmpty()) {
            return username;
        }

        // 尝试从认证上下文中获取已认证的用户名
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }

        return "未知用户";
    }

    /**
     * 记录异常详细信息
     */
    private void logExceptionDetails(HttpServletRequest request, Exception e, String exceptionType) {
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        String method = request.getMethod();
        String username = getAttemptedUsername(request);

        // 获取User-Agent信息
        String userAgent = request.getHeader("User-Agent");

        logger.error("{} - IP: {}, 用户: {}, 路径: {} [{}], User-Agent: {}, 错误详情: {}",
                exceptionType, clientIp, username, path, method, userAgent, e.getMessage(), e);
    }

    /**
     * 处理自定义的业务异常
     */
    @ExceptionHandler(value = BizException.class)
    @ResponseBody
    public ResultBody<Object> bizExceptionHandler(HttpServletRequest req, BizException e) {
        logExceptionDetails(req, e, "业务异常");
        return ResultBody.error(req, e.getErrorCode(), e.getErrorMsg());
    }

    /**
     * IllegalArgumentException NullPointerException UnsupportedOperationException都是RuntimeException
     * 这里直接捕获RuntimeException来代替一个一个去捕获
     */
    @ExceptionHandler(value = RuntimeException.class)
    @ResponseBody
    public ResultBody<Object> exceptionHandler(HttpServletRequest req, RuntimeException e) {
        logExceptionDetails(req, e, "运行时异常");
        return ResultBody.error(req, BizExceptionEnum.BODY_NOT_MATCH);
    }

    /**
     * 处理请求方法不支持的异常
     */
    @ExceptionHandler(value = ServletException.class)
    @ResponseBody
    public ResultBody<Object> exceptionHandler(HttpServletRequest req, ServletException e) {
        logExceptionDetails(req, e, "请求时异常");
        return ResultBody.error(req, BizExceptionEnum.REQUEST_METHOD_SUPPORT);
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ResultBody<Object> exceptionHandler(HttpServletRequest req, Exception e) {
        logExceptionDetails(req, e, "未知异常");
        return ResultBody.error(req, BizExceptionEnum.INTERNAL_SERVER_ERROR);
    }
}