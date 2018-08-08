package com.htge.login.model;

import com.htge.login.util.LoginManager;
import org.apache.shiro.session.Session;
import org.jboss.logging.Logger;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.*;

public class RedisSessionDB implements SessionDBImpl {
    private Logger logger = Logger.getLogger(RedisSessionDB.class);
    private RedisTemplate<String, Object> template = null;
    private RedisTemplate<String, Object> userTemplate = null;

    public void setFactoryManager(RedisFactoryManager factoryManager) {
        template = factoryManager.getTemplate(1);
        userTemplate = factoryManager.getTemplate(2);
    }

    /* CRUD */
    public Session get(Serializable key) {
        Object value = template.opsForValue().get(key);
        if (value instanceof Session) {
            logger.debug("get: "+value+" user: "+((Session)value).getAttribute(LoginManager.SESSION_USER_KEY));
            return (Session) value;
        }
        return null;
    }

    public void update(Session session) {
        template.opsForValue().set(session.getId().toString(), session);
        String user = (String)session.getAttribute(LoginManager.SESSION_USER_KEY);
        logger.debug("update: "+session+" user: "+session.getAttribute(LoginManager.SESSION_USER_KEY));
    }

    public void delete(Session session) {
        template.delete(session.getId().toString());
    }

    /* 如果已经在其他地方登录，这里做剔出逻辑 */
    public Serializable getOldSessionId(String user) {
        Object value = userTemplate.opsForValue().get(user);
        if (value instanceof Serializable) {
            return (Serializable) value;
        }
        return null;
    }

    public void updateUser(Session oldSession, Session newSession, String user) {
        if (oldSession != null) {
            Serializable oldId = oldSession.getId();
            if (oldId != null && !oldId.equals(newSession.getId())) {
                //旧的Session立即失效
                oldSession.removeAttribute(LoginManager.SESSION_USER_KEY);
                oldSession.setTimeout(0);
                update(oldSession);
                logger.info("remove SessionID:" + newSession.getId().toString());
            }
        }
        logger.info("update SessionID:"+newSession.getId().toString());
        userTemplate.opsForValue().set(user, newSession.getId());
    }
}
