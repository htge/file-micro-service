package com.htge.login.model;

import java.util.Collection;
import java.util.List;

public interface UserItemCacheImpl {
    void deleteUser(String username);
    void updateUser(Userinfo userItem);
    Userinfo findUser(String username);
    void setAllUsers(Collection<Userinfo> userinfos);
    Collection<Userinfo> getAllUsers();
    void clear();
}
