package com.htge.login.model;

import com.htge.login.config.LoginProperties;
import org.jboss.logging.Logger;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.util.StopWatch;

import java.util.Map.Entry;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class RedisUserItemCache implements UserItemCacheImpl {
    private RedisTemplate<String, Object> template = null;
    private final String userinfoKey = "Userinfo";
    private final BlockingQueue<Collection<Userinfo>> queues = new LinkedBlockingDeque<>();
    private final Object waitObject = new Object();
    private static Thread addThread = null;
    private final Logger logger = Logger.getLogger(RedisUserItemCache.class);

    private LoginProperties loginProperties = null;

    public RedisUserItemCache() {
        //起一个线程用于并发加速
        if (addThread != null) {
            return;
        }
        addThread = new Thread(() -> {
            while (true) {
                try {
                    if (queues.isEmpty()) {
                        synchronized (waitObject) {
                            waitObject.notifyAll();
                        }
                    }
                    addUsersSync(queues.take());
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
        addThread.setDaemon(true);
        addThread.start();
    }

    public void setFactoryManager(RedisFactoryManager factoryManager) {
        template = factoryManager.getTemplate(0);

        //初始化时做一次清理，以免数据不同步
        clear();
    }

    public void setLoginProperties(LoginProperties loginProperties) {
        this.loginProperties = loginProperties;
    }

    @Override
    public void deleteUser(String username) {
        template.opsForHash().delete(userinfoKey, username);
    }

    @Override
    public void updateUser(Userinfo userinfo) {
        template.opsForHash().put(userinfoKey, userinfo.getUsername(), userinfo);
    }

    @Override
    public Userinfo findUser(String username) {
        Object value = template.opsForHash().get(userinfoKey, username);
        if (value instanceof Userinfo) {
            return (Userinfo)value;
        }
        return null;
    }

    @Override
    public void addUsers(Collection<Userinfo> userinfos) {
        queues.offer(userinfos);
    }

    @Override
    public void addUsersSync(Collection<Userinfo> userinfos) {
        Iterator<Userinfo> iterator = userinfos.iterator();
        while (iterator.hasNext()) {
            //超过一定量得分批处理
            setUsers(iterator);
        }
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void setUsers(Iterator<Userinfo> iterator) {
        final int limit = loginProperties.getCacheUnit();
        if (limit <= 0) {
            throw new InvalidPropertyException(loginProperties.getClass(), "login.per", "value invalid: "+limit);
        }
        SessionCallback <Object>callback = new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                final BoundHashOperations<?, String, Userinfo> operations = redisOperations.boundHashOps(userinfoKey);
                int count = 0;
                while (count < limit && iterator.hasNext()) {
                    Userinfo userinfo = iterator.next();
                    operations.put(userinfo.getUsername(), userinfo);
                    count++;
                }
//                logger.info("setUsers added count: "+count);
                return null;
            }
        };
        template.executePipelined(callback);
    }

    @Override
    public void waitForAddUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        if (!queues.isEmpty()) {
            synchronized (waitObject) {
                try {
                    waitObject.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        stopWatch.stop();
        logger.info("waitForAddUsers() elapsed: "+stopWatch.getTotalTimeMillis()+"ms");
    }

    @Override
    public Collection<Userinfo> getUsers(int begin, int limit) {
        try {
            Collection<Userinfo> userInfos = new ArrayList<>();
            if (!template.hasKey(userinfoKey)) {
                return null;
            }
            HashOperations<String, String, Userinfo> operations = template.opsForHash();
            ScanOptions.ScanOptionsBuilder builder = new ScanOptions.ScanOptionsBuilder().match("*").count(10000);
            Cursor<Entry<String, Userinfo>> cursor = operations.scan(userinfoKey, builder.build());
            int nCursor = 0;
            while (cursor.hasNext()) {
                Entry<String, Userinfo> entry = cursor.next();
                if (begin <= nCursor && nCursor < begin+limit) {
                    userInfos.add(entry.getValue());
                }
                nCursor++;
            }
            return userInfos;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public long size() {
        return template.opsForHash().size(userinfoKey);
    }

    @Override
    public void clear() {
        template.execute((RedisConnection redisConnection) -> {
            redisConnection.flushDb();
            return null;
        }, true);
    }
}
