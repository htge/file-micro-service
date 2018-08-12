package com.htge.login.filter;

import com.htge.login.config.LoginProperties;
import com.htge.login.util.LoginManager;
import org.jboss.logging.Logger;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

    /**
     * 无需登录即可访问的资源，如登录、登出页面，图片、样式表、JS文件等 *
     */
    private static final String[] NOFILTERS = new String[]{
        "/auth/css",
        "/auth/js",
        "/auth/images",
        "/auth/localization",
        "/favicon.ico",
        "/error",
    };

    /**
     * 管理员权限可访问的资源
     */
    private static final String[] ADMINRES = new String[]{
        "/auth/register",
        "/auth/delete",
        "/auth/userList",
    };

    /**
     * 请求的资源是否需要登录后才可以访问
     *
     * @param resource 访问的资源
     * @return 是否可以访问
     */
    private boolean isFilterResources(String resource) {
        for (String nofilter : NOFILTERS) {
            if (resource.startsWith(nofilter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 请求的资源是否需要管理员权限才可以访问
     *
     * @param resource 访问的资源
     * @return 是否可以访问
     */
    private boolean isAdminResources(String resource) {
        for (String adminres : ADMINRES) {
            if (resource.startsWith(adminres)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws IOException {
        //过滤掉一些静态资源
        String requestPath = httpServletRequest.getServletPath();
        if (isFilterResources(requestPath)) {
            return true;
        }
        //一些页面不用检测权限，直接跳过
        if (!requestPath.startsWith("/auth")) {
            return true;
        }
        //sessionid始终变化的情况
        if (!loginManager.isValidSession(httpServletRequest)) {
            logger.warn("session is not equal, redirect to login page...");
            loginManager.redirectToLogin(httpServletRequest, httpServletResponse);
            return false;
        }
        if (requestPath.equals("/auth")) {
            loginManager.redirectToLogin(httpServletRequest, httpServletResponse);
            return false;
        }
        //权限检测
        LoginManager.ROLE role = loginManager.role();
        if (requestPath.equals("/auth/") || requestPath.equals("/auth/login")) {
            //已经登录过了，直接跳转，否则显示页面或者执行登录操作
            if (role != LoginManager.ROLE.Undefined) {
                httpServletResponse.sendRedirect(properties.getNextRoot());
                return false;
            }
            return true;
        }
        //未登录
        if (role == LoginManager.ROLE.Undefined) {
            httpServletResponse.sendError(404);
            return false;
        }
        //只有管理员权限才能访问的页面
        if (isAdminResources(requestPath)) {
            if (role != LoginManager.ROLE.Admin) {
                loginManager.redirectToLogin(httpServletRequest, httpServletResponse);
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
