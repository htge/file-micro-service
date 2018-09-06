package com.htge.login.filter;

import com.htge.login.config.LoginProperties;
import com.htge.login.util.LoginManager;
import org.jboss.logging.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 通用的过滤条件，在shiro.xml的filterChainDefinitions参数中定义
 */
public class loginInterceptor implements HandlerInterceptor {
    private Logger logger = Logger.getLogger(loginInterceptor.class);

    private LoginManager loginManager;
    private LoginProperties properties;

    public void setLoginManager(LoginManager loginManager) {
        this.loginManager = loginManager;
    }
    public void setProperties(LoginProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws IOException {
        //过滤掉一些静态资源
        String requestPath = httpServletRequest.getServletPath();
        String requestMethod = httpServletRequest.getMethod();

        //默认回302的地址
        if (requestPath.equals("/") || requestPath.equals("/auth")) {
            httpServletResponse.sendRedirect(properties.getRootPath());
            return false;
        }
        //权限检测
        LoginManager.ROLE role = loginManager.role();
        if (requestPath.equals("/auth/") || (requestPath.equals("/auth/user") && requestMethod.equals("GET"))) {
            //sessionid始终变化的情况
            if (!loginManager.isValidSession(httpServletRequest)) {
                logger.warn("session is not equal, redirect to login page...");
                loginManager.redirectToLogin(httpServletRequest, httpServletResponse);
                return false;
            }
            //已经登录过了，直接跳转，否则显示页面或者执行登录操作
            if (role != LoginManager.ROLE.Undefined) {
                httpServletResponse.sendRedirect(properties.getNextRoot());
                return false;
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) {

    }
}
