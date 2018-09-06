package com.htge.login.filter;

import com.htge.login.util.LoginManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class IPAuthFilter extends FormAuthenticationFilter {
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        Subject subject = SecurityUtils.getSubject();
        HttpServletRequest servletRequest = (HttpServletRequest)request;

        //登录后判断ip是否一致，这个需要放在其他地方才行
        Session session = subject.getSession();
        if (subject.getPrincipal() != null) {
            String ip = LoginManager.getIpAddress(servletRequest);
            String sessionIP = (String)session.getAttribute(LoginManager.SESSION.IP_KEY);
            if (ip == null || !ip.equals(sessionIP)) {
                return false;
            }
        }

        return super.isAccessAllowed(request, response, mappedValue);
    }
}
