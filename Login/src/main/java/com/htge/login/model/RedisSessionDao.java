package com.htge.login.model;

import com.htge.login.util.LoginManager;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.jboss.logging.Logger;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Random;

public class RedisSessionDao extends EnterpriseCacheSessionDAO {
    private SessionDBImpl sessionDB = null;

    private Logger logger = Logger.getLogger(RedisSessionDao.class);

    public void setSessionDB(SessionDBImpl sessionDB) {
        this.sessionDB = sessionDB;
    }

    public SessionDBImpl getSessionDB() {
        return sessionDB;
    }

    @Override
    protected Serializable generateSessionId(Session session) {
        BigInteger integer = new BigInteger(128, new Random());
        return String.format("%032X", integer);
    }

    @Override
    protected Serializable doCreate(Session session) {
        Serializable ret = super.doCreate(session);
        logger.debug("doCreate: "+ret.toString());
        sessionDB.update(session); //没有设置db，抛异常，下同
        return ret;
    }

    @Override
    public Session readSession(Serializable sessionId) throws UnknownSessionException {
        //shiro内置缓存优先，没有的情况再找redis
        Session session = super.getCachedSession(sessionId);
        if (session == null) {
            logger.debug("readSession: "+sessionId.toString());
            session = sessionDB.get(sessionId);
            if (session != null) {
                //从redis找到后，放到shiro内置缓存中去，否则让shiro内置的方法创建后，自动调用doUpdate方法与redis同步
                Cache<Serializable, Session> cache = getActiveSessionsCache();
                if (cache == null) {
                    cache = createActiveSessionsCache();
                    setActiveSessionsCache(cache);
                }
                cache.put(sessionId, session);
            }
            return super.readSession(sessionId);
        }
        return session;
    }

    @Override
    protected void doUpdate(Session session) {
        logger.debug("doUpdate: "+session.toString());
        String user = (String)session.getAttribute(LoginManager.SESSION.USER_KEY);

        Session oldSession = null;
        if (user != null) {
            Serializable sessionId = sessionDB.getOldSessionId(user);
            if (sessionId != null) {
                try {
                    oldSession = readSession(sessionId);
                } catch (UnknownSessionException e) {
                    //容错，sessionId可能非法
                    logger.debug(e.getMessage());
                }
            }
        }
        sessionDB.update(session);

        //剔除逻辑
        if (user != null) {
            sessionDB.updateUser(oldSession, session, user);
        }
    }

    @Override
    protected void doDelete(Session session) {
        logger.debug("doDelete: "+session.toString());
        sessionDB.delete(session);
    }
}
