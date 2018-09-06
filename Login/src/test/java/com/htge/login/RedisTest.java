package com.htge.login;

import com.htge.login.model.RedisSessionDao;
import com.htge.login.model.SessionDBImpl;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.JavaUuidSessionIdGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StopWatch;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class RedisTest {
    //多线程用的assert，单线程不要用
    private final CountCheck counter = new CountCheck();

    @Autowired
    RedisSessionDao redisSessionDao;

    private SessionDBImpl sessionDB = null;

    @Before
    public void sessionDBInAnyCastInit() {
        counter.initialize(0);
        sessionDB = redisSessionDao.getSessionDB();
    }

    @Test
    public void sessionDBInAnyCast() throws InterruptedException {
        //初始化配置
        JavaUuidSessionIdGenerator generator = new JavaUuidSessionIdGenerator();
        int total = 10000;

        //线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 100,
                60, TimeUnit.SECONDS, new LinkedBlockingDeque<>());

        //计时器不含初始化的时间
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        for (int i=0; i<total; i++) {
            executor.execute(() -> {
                SimpleSession session = new SimpleSession();
                session.setId(generator.generateId(session));
                session.setTimeout(120);
                session.setAttribute("abc", 1);

                //添加
                sessionDB.update(session);
                Session oldSession = sessionDB.get(session.getId());
                counter.assertTrue(oldSession != null);
                counter.assertTrue(oldSession.getTimeout() == 120);
                counter.assertTrue((int)oldSession.getAttribute("abc") == 1);

                //timeout改变以后的值
                session.setTimeout(24000000);
                sessionDB.update(session);
                oldSession = sessionDB.get(session.getId());
                counter.assertTrue(oldSession != null);
                counter.assertTrue(oldSession.getTimeout() == 24000000);

                //删除以后就不存在了
                sessionDB.delete(session);
                oldSession = sessionDB.get(session.getId());
                counter.assertTrue(oldSession == null);
            });
        }

        long completed;
        do {
            completed = executor.getCompletedTaskCount();
            System.out.println("sessionDBInAnyCast() tested: " + completed + "/" + total);
            Thread.sleep(500);
        } while (completed != total);

        //用户的登录剔除逻辑（已经集成到Shiro的内部逻辑）
//        SimpleSession session = new SimpleSession();
//        session.setId(generator.generateId(session));
//        session.setTimeout(120);
//        session.setAttribute(LoginManager.SESSION_USER_KEY, "123"); //假设用户名是123
//        sessionDB.update(session);
//
//        SimpleSession session2 = new SimpleSession();
//        session2.setId(generator.generateId(session2));
//        session2.setTimeout(120);
//        session2.setAttribute(LoginManager.SESSION_USER_KEY, "123"); //假设用户名是123
//        sessionDB.update(session2);
//
//        Session oldSession = sessionDB.get(session.getId());
//        Session newSession = sessionDB.get(session2.getId());
//
//        Assert.assertTrue(oldSession != null);
//        Assert.assertTrue(oldSession.getAttribute(LoginManager.SESSION_USER_KEY) == null);
//        Assert.assertTrue(newSession != null);
//        Assert.assertTrue(newSession.getAttribute(LoginManager.SESSION_USER_KEY).equals("123"));

        stopWatch.stop();

        System.out.println("sessionDBInAnyCast() elapsed time per exec: "
                + stopWatch.getTotalTimeMillis()/(double)total + "ms");
    }

    @After
    public void sessionDBInAnyCastEnd() {
        Assert.assertEquals("has errors", 0, counter.getCount());
    }
}
