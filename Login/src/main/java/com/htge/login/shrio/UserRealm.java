package com.htge.login.shrio;

import com.htge.login.model.UserinfoDao;
import com.htge.login.model.Userinfo;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class UserRealm extends AuthorizingRealm {
    private UserinfoDao userinfoDao = null;

    public void setUserinfoDao(UserinfoDao userinfoDao) {
        this.userinfoDao = userinfoDao;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        Set<String> permissions = new HashSet<>();
        permissions.add("anon"); //写死这个值
        authorizationInfo.setStringPermissions(permissions);
        return authorizationInfo;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken)
            throws AuthenticationException {
        String username = (String) authenticationToken.getPrincipal();
        Userinfo userinfo = userinfoDao.findUser(username);
        if (userinfo == null) {
            //伪造一个用户信息，防止注册过的用户名被猜测到
            userinfo = new Userinfo();
            userinfo.setUsername("");
            userinfo.setUserdata("");
        }
        return new SimpleAuthenticationInfo(userinfo.getUsername(), userinfo.getUserdata(),
                null, getName());
    }
}
