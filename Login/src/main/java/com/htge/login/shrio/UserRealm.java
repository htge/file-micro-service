package com.htge.login.shrio;

import com.htge.login.model.UserinfoDao;
import com.htge.login.model.Userinfo;
import com.htge.login.util.LoginManager;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

public class UserRealm extends AuthorizingRealm {
    private UserinfoDao userinfoDao = null;

    public void setUserinfoDao(UserinfoDao userinfoDao) {
        this.userinfoDao = userinfoDao;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        String username = (String)principalCollection.getPrimaryPrincipal();
        Userinfo userinfo = userinfoDao.findUser(username);

        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        if (userinfo != null) {
            if (userinfo.getRole() == LoginManager.LoginRole.Admin) {
                //管理员
                authorizationInfo.addRole("admin");
                authorizationInfo.addStringPermission("admin");
            }
            //普通用户
            authorizationInfo.addRole("user");
            authorizationInfo.addStringPermission("user");
        }
        //匿名用户的情况下不会触发此回调，不考虑
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
            userinfo.setUsername("invalid username");
            userinfo.setUserdata("invalid userdata");
        }
        return new SimpleAuthenticationInfo(userinfo.getUsername(), userinfo.getUserdata(),
                null, getName());
    }
}
