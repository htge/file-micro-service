package com.htge.login.model;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.jboss.logging.Logger;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class RedisSessionDao extends EnterpriseCacheSessionDAO {
    private SessionDBImpl sessionDB = null;

    public void setSessionDB(SessionDBImpl sessionDB) {
        this.sessionDB = sessionDB;
    }
    private Logger logger = Logger.getLogger(RedisSessionDao.class);

    @Override
    protected Serializable doCreate(Session session) {
        Serializable ret = super.doCreate(session);
        logger.info("doCreate: "+ret.toString());
        sessionDB.update(session); //没有设置db，抛异常，下同
        return ret;
    }

    @Override
    public Session readSession(Serializable sessionId) throws UnknownSessionException {
        logger.info("readSession: "+sessionId.toString());
        Session session = sessionDB.get(sessionId);
        if (session != null) {
            Cache<Serializable, Session> cache = getActiveSessionsCache();
            if (cache == null) {
                cache = createActiveSessionsCache();
                setActiveSessionsCache(cache);
            }
            cache.put(sessionId, session);
        }
        return super.readSession(sessionId);
    }

    @Override
    protected void doUpdate(Session session) {
        logger.info("doUpdate: "+session.toString());
        sessionDB.update(session);
    }

    @Override
    protected void doDelete(Session session) {
        logger.info("doDelete: "+session.toString());
        sessionDB.delete(session);
    }
}
