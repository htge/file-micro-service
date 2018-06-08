package com.htge.login.model;

import org.apache.shiro.session.Session;

import java.io.Serializable;

public interface SessionDBImpl {
    Session get(Serializable key);
    void update(Session session);
    void delete(Session session);
}
