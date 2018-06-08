package com.htge.login.model;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.Map.Entry;
import java.util.*;

@Component
public class RedisUserItemCache implements UserItemCacheImpl {
    private RedisTemplate<String, Object> template = null;
    private final String userinfoKey = "Userinfo";

    public void setTemplate(RedisTemplate<String, Object> template) {
        template.setEnableTransactionSupport(true);
        this.template = template;
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
    public void setAllUsers(Collection<Userinfo> userinfos) {
        Iterator<Userinfo> iterator = userinfos.iterator();
        while (iterator.hasNext()) {
            //超过一定量得分批处理
            setUsers(iterator);
        }
//        System.out.println("setUsers total count: "+userinfos.size());
    }

    private void setUsers(Iterator<Userinfo> iterator) {
        final int limit = 1000;
        int count = 0;
        HashOperations<String, String, Userinfo> operations = template.opsForHash();
        template.watch(userinfoKey);
        template.multi();
        while (count < limit && iterator.hasNext()) {
            Userinfo userinfo = iterator.next();
            operations.put(userinfoKey, userinfo.getUsername(), userinfo);
            count++;
        }
        template.exec();
//        System.out.println("setUsers added count: "+count);
    }

    @Override
    public Collection<Userinfo> getAllUsers() {
        try {
            Collection<Userinfo> userInfos = new ArrayList<>();
            HashOperations<String, String, Userinfo> operations = template.opsForHash();
            ScanOptions.ScanOptionsBuilder builder = new ScanOptions.ScanOptionsBuilder().match("*").count(1000);
            Cursor<Entry<String, Userinfo>> cursor = operations.scan(userinfoKey, builder.build());
            while (cursor.hasNext()) {
                Entry<String, Userinfo> entry = cursor.next();
                userInfos.add(entry.getValue());
            }
            if (userInfos.size() > 0) {
                return userInfos;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void clear() {
        template.execute((RedisConnection redisConnection) -> {
            redisConnection.flushDb();
            return null;
        }, true);
    }
}
