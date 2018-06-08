package com.htge.login.config;

import com.htge.login.model.RedisSessionDao;
import com.htge.login.model.UserinfoDao;
import com.htge.login.shrio.CredentialsMatcher;
import com.htge.login.shrio.UserRealm;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.mgt.quartz.QuartzSessionValidationScheduler;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShiroConfig implements ApplicationContextAware {
    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /*####### 会话验证器，防止session过期后不能自动删除 #######*/
    @Bean
    QuartzSessionValidationScheduler sessionValidationScheduler() {
        QuartzSessionValidationScheduler ret = new QuartzSessionValidationScheduler();
        ret.setSessionValidationInterval(300000); //5分钟检查一次
        return ret;
    }

    @Bean
    DefaultWebSessionManager defaultWebSessionManager() {
        QuartzSessionValidationScheduler validationScheduler = context.getBean(QuartzSessionValidationScheduler.class);
        RedisSessionDao dao = context.getBean(RedisSessionDao.class);

        DefaultWebSessionManager webSessionManager = new DefaultWebSessionManager();
        webSessionManager.setGlobalSessionTimeout(300000); //没有登录，5分钟过期
        webSessionManager.setDeleteInvalidSessions(true);
        webSessionManager.setSessionValidationSchedulerEnabled(true);
        webSessionManager.setSessionValidationScheduler(validationScheduler);

        Cookie cookie = new SimpleCookie("JSESSID");
        cookie.setHttpOnly(true);
        webSessionManager.setSessionIdCookie(cookie);
        webSessionManager.setSessionIdCookieEnabled(true);
        webSessionManager.setSessionDAO(dao);

        validationScheduler.setSessionManager(webSessionManager);

        return webSessionManager;
    }

    @Bean
    DefaultWebSecurityManager defaultWebSecurityManager() {
        DefaultWebSessionManager sessionManager = context.getBean(DefaultWebSessionManager.class);
        UserRealm realm = context.getBean(UserRealm.class);

        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setSessionManager(sessionManager);
        securityManager.setRealm(realm);
        return securityManager;
    }

    /* Shrio configuration*/
    @Bean
    EhCacheManager ehCacheManager() {
        final CredentialsMatcher matcher = context.getBean(CredentialsMatcher.class);
        final UserRealm realm = context.getBean(UserRealm.class);
        final UserinfoDao dao = context.getBean(UserinfoDao.class);

        final EhCacheManager ehCacheManager = new EhCacheManager();
        final String cacheManagerName = "shiroCache";
        final String ehCacheName = "passwordRetryCache";

        CacheManager oldCacheManager = CacheManager.getCacheManager(cacheManagerName);
        if (oldCacheManager == null) {
            net.sf.ehcache.config.Configuration configuration = new net.sf.ehcache.config.Configuration();
            DiskStoreConfiguration diskStoreConfiguration = new DiskStoreConfiguration();
            diskStoreConfiguration.setPath("java.io.tmpdir");
            configuration.addDiskStore(diskStoreConfiguration);

            CacheConfiguration cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setName(ehCacheName);
            cacheConfiguration.setMaxEntriesLocalHeap(2000);
            cacheConfiguration.setEternal(false);
            cacheConfiguration.setTimeToIdleSeconds(1800);
            cacheConfiguration.setTimeToLiveSeconds(0);
            cacheConfiguration.setOverflowToDisk(false);
            cacheConfiguration.setStatistics(true);
            configuration.addCache(cacheConfiguration);

            configuration.setName(cacheManagerName);

            oldCacheManager = CacheManager.newInstance(configuration);

        }

        ehCacheManager.setCacheManager(oldCacheManager);

        matcher.setPasswordRetryCache(ehCacheManager.getCache(ehCacheName));

        realm.setCredentialsMatcher(matcher);
        realm.setUserinfoDao(dao);

        return ehCacheManager;
    }

    @Bean
    ShiroFilterFactoryBean shiroFilterFactoryBean() {
        DefaultSecurityManager manager = context.getBean(DefaultSecurityManager.class);

        ShiroFilterFactoryBean ret = new ShiroFilterFactoryBean();
        try {
            if (manager != null) {
                ret.setSecurityManager(manager);
                SecurityUtils.setSecurityManager(manager); //预先设置好管理的对象后，之后其他地方就可以直接用了
            }
            ret.setLoginUrl("/");
            ret.setUnauthorizedUrl("/");
            ret.setFilterChainDefinitions("/** = anon"); //这个无视了，因为有另一套方案处理权限相关的问题
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Bean
    LifecycleBeanPostProcessor lifecycleBeanPostProcessor() { //不能和AppConfig的配置放在一起，会导致properties无法解析
        return new LifecycleBeanPostProcessor();
    }
}
