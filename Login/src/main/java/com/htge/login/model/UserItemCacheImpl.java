package com.htge.login.model;

import java.util.Collection;

public interface UserItemCacheImpl {
    void deleteUser(String username);
    void updateUser(Userinfo userItem);
    Userinfo findUser(String username);
    void addUsers(Collection<Userinfo> userinfos);
    void addUsersSync(Collection<Userinfo> userinfos);
    void waitForAddUsers();
    Collection<Userinfo> getUsers(int begin, int limit);
    long size();
    void clear();
}
