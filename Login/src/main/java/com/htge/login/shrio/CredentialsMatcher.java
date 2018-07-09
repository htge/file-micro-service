package com.htge.login.shrio;

import com.htge.login.util.Crypto;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.ehcache.EhCacheManager;

import java.util.concurrent.atomic.AtomicInteger;

public class CredentialsMatcher extends HashedCredentialsMatcher {

    private Cache<String, AtomicInteger> passwordRetryCache = null;

    public CredentialsMatcher() {
        setHashAlgorithmName("sha256");
        setHashIterations(1);
        setStoredCredentialsHexEncoded(true);
    }

    public void setCacheManager(EhCacheManager cacheManager) {
        passwordRetryCache = cacheManager.getCache("passwordRetryCache");
    }

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) token;
        String host = usernamePasswordToken.getHost();
        String username = (String)token.getPrincipal();
        String retryKey = String.format("%s_%s", host, username);
        AtomicInteger retryCount = passwordRetryCache.get(retryKey);
        if (retryCount == null) {
            retryCount = new AtomicInteger(0);
            passwordRetryCache.put(retryKey, retryCount);
        }
        //同一个地址的某个用户输错密码次数过多，则不允许继续尝试
        if (retryCount.incrementAndGet() > 5) {
            throw new ExcessiveAttemptsException();
        }
        String password = String.valueOf((char[])token.getCredentials());
        String userData = Crypto.generateUserData(username, password);
        String oldUserData = (String) info.getCredentials();
        if (oldUserData.equals(userData)) {
            retryCount.set(0);
            return true;
        }
        return false;
    }
}
