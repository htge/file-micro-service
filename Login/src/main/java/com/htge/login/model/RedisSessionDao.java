package com.htge.login.model;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.jboss.logging.Logger;

import java.io.Serializable;

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
        //shiro内置缓存优先，没有的情况再找redis
        Session session = super.getCachedSession(sessionId);
        if (session == null) {
            logger.info("readSession: "+sessionId.toString());
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
        logger.info("doUpdate: "+session.toString());
        sessionDB.update(session);
    }

    @Override
    protected void doDelete(Session session) {
        logger.info("doDelete: "+session.toString());
        sessionDB.delete(session);
    }
}
