package com.htge.login.filter;

import com.htge.login.util.LoginManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authz.AuthorizationFilter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class PermissionFilter extends AuthorizationFilter {
    @Override
    public boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        Subject subject = SecurityUtils.getSubject();
        HttpServletRequest servletRequest = (HttpServletRequest)request;
        String[] perms = ((String[])mappedValue);
        String method = servletRequest.getMethod();
        boolean isPermitted = false;

        if (perms != null && perms.length > 0) {
            for (String perm : perms) {
                try {
                    String[] values = perm.split(":");

                    //perm=role:method
                    if (values.length == 2 &&
                            (values[0].equals("anon") || subject.isPermitted(perm)) &&
                            values[1].equals(method)) {
                        isPermitted = true;
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return isPermitted;
    }
}

