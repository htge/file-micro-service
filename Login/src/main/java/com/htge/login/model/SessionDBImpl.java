package com.htge.login.model;

import org.apache.shiro.session.Session;

import java.io.Serializable;

public interface SessionDBImpl {
    Session get(Serializable key);
    void update(Session session);
    void delete(Session session);

    //剔除逻辑
    Serializable getOldSessionId(String user);
    void updateUser(Session oldSession, Session newSession, String user);
}
