package com.htge.login.shrio;

import com.htge.login.util.Crypto;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.cache.Cache;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CredentialsMatcher extends HashedCredentialsMatcher {

    private Cache<String, AtomicInteger> passwordRetryCache = null;

    public CredentialsMatcher() {
        setHashAlgorithmName("sha256");
        setHashIterations(1);
        setStoredCredentialsHexEncoded(true);
    }

    public void setPasswordRetryCache(Cache<String, AtomicInteger> passwordRetryCache) {
        this.passwordRetryCache = passwordRetryCache;
    }

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        String username = (String)token.getPrincipal();
        AtomicInteger retryCount = passwordRetryCache.get(username);
        if (retryCount == null) {
            retryCount = new AtomicInteger(0);
            passwordRetryCache.put(username, retryCount);
        }
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
