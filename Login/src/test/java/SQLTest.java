import com.htge.login.boot.Application;
import com.htge.login.config.AppConfig;
import com.htge.login.model.RedisUserItemCache;
import com.htge.login.model.UserItemCacheImpl;
import com.htge.login.model.UserinfoDao;
import com.htge.login.model.Userinfo;
import org.apache.shiro.session.mgt.eis.JavaUuidSessionIdGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.logback.LogbackLoggingSystem;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StopWatch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class SQLTest {
    //多线程用的assert，单线程不要用
    private final CountCheck counter = new CountCheck();

    @Autowired
    UserinfoDao dao;
    //先清空Cache的情况，清空之前先备份值，需要用Cache的用例会把Cache重新设置回去
    @Autowired
    RedisUserItemCache cache;

    @Before
    public void userItemDaoInAnyCastInit() {
        counter.initialize(0);
    }

    private void testSQL(int total, ThreadPoolExecutor executor, UserinfoDao dao, JavaUuidSessionIdGenerator generator) {
        for (int i=0; i<total; i++) {
            executor.execute(() -> {
                Serializable serializableUser = generator.generateId(null);
                Serializable serializableUserData = generator.generateId(null);

                Userinfo userinfo = new Userinfo();
                userinfo.setUsername(serializableUser.toString());
                userinfo.setUserdata(serializableUserData.toString());
                userinfo.setRole((int)(Math.random()*100));

                //插入
                dao.create(userinfo);
                Userinfo oldItem = dao.findUser(userinfo.getUsername());
                counter.assertTrue(oldItem != null);
                counter.assertTrue(oldItem.getUsername().equals(userinfo.getUsername()));
                counter.assertTrue(oldItem.getUserdata().equals(userinfo.getUserdata()));
                counter.assertTrue(oldItem.getRole().equals(userinfo.getRole()));

                //修改，目前只支持userData
                Serializable newUserData = generator.generateId(null);
                userinfo.setUserdata(newUserData.toString());
                dao.update(userinfo);
                oldItem = dao.findUser(userinfo.getUsername());
                counter.assertTrue(oldItem != null);
                counter.assertTrue(oldItem.getUserdata().equals(userinfo.getUserdata()));

                //删除
                dao.delete(userinfo.getUsername());
                oldItem = dao.findUser(userinfo.getUsername());
                counter.assertTrue(oldItem == null);
            });
        }
    }

//    @Test
//    public void insertCast() {
//        //测试插入N条记录
//        final int count = 1000000, limit = 10000;
//        int i;
//        if (dao.getUserItemCache() == null) {
//            dao.setUserItemCache(cache);
//        }
//        List<Userinfo> list = new ArrayList<>();
//        for (i=0; i<count; i++) {
//            Userinfo u = new Userinfo();
//            u.setUsername("testu"+i);
//            u.setUserdata("a");
//            u.setRole(0);
//            list.add(u);
//            if (i % limit == limit-1 || i == count-1) {
//                counter.assertTrue(dao.createAll(list));
//                list.clear();
//                System.out.println((i+1)+" requests completed");
//            }
//        }
//        dao.waitAction();
//    }
//
    @Test
    public void buildCacheTest() {
        cache.clear();
        dao.buildUserCache();
    }

    @Test
    public void userItemDaoInAnyCast() throws Exception {
        //初始化配置
        JavaUuidSessionIdGenerator generator = new JavaUuidSessionIdGenerator();
        if (dao.getUserItemCache() == null) {
            dao.setUserItemCache(cache);
        }

        //线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 100,
                60, TimeUnit.SECONDS, new LinkedBlockingDeque<>());

        //有一种情况要覆盖
        Serializable serializableUser = generator.generateId(null);
        Serializable serializableUser2 = generator.generateId(null);
        Serializable serializableUserData = generator.generateId(null);

        //插入两条记录，测试getAllUsers()重建缓存的情况
        Userinfo u1 = new Userinfo();
        u1.setUsername(serializableUser.toString());
        u1.setUserdata(serializableUserData.toString());
        u1.setRole((int)(Math.random()*100));

        Userinfo u2 = new Userinfo();
        u2.setUsername(serializableUser2.toString());
        u2.setUserdata(serializableUserData.toString());
        u2.setRole((int)(Math.random()*100));

        dao.create(u1);
        dao.create(u2);
        dao.waitAction();//等数据库插入后
        dao.getUserItemCache().clear();//清理缓存

        Assert.assertTrue(dao.getUserItemCache().size() == 0); //缓存被清空
        dao.getUsers(0, 100);
        Collection<Userinfo> userinfos = dao.getUserItemCache().getUsers(0, 2);

        //缓存条件
        Assert.assertTrue(userinfos != null);
        Assert.assertTrue(userinfos.size() == 2);
        Assert.assertTrue(dao.getUserItemCache().size() >= 2);

        //通过数据库查询后，缓存可以正常工作的条件
        Assert.assertTrue(dao.findUser(u1.getUsername()) != null);
        Assert.assertTrue(dao.findUser(u2.getUsername()) != null);
        Assert.assertTrue(dao.getUserItemCache().findUser(u1.getUsername()) != null);
        Assert.assertTrue(dao.getUserItemCache().findUser(u2.getUsername()) != null);

        //清理临时数据
        dao.delete(u1.getUsername());
        dao.delete(u2.getUsername());

        //计时器不含初始化的时间
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        StopWatch stopWatch2 = new StopWatch();
        stopWatch2.start();

        Collection<Userinfo> oldItems = dao.getUsers(0, 2);

        int total = 2500;
        testSQL(total, executor, dao, generator);

        long completed;
        do {
            completed = executor.getCompletedTaskCount();
            System.out.println("userItemDaoInAnyCast() tested: " + completed + "/" + total);
            Thread.sleep(500);
        } while (completed != total);

        Assert.assertTrue(dao.getUsers(0, 2).size() == oldItems.size());

        //最后一条记录要能执行完
        stopWatch.stop();

        dao.waitAction();
        stopWatch2.stop();

        System.out.println("userItemDaoInAnyCast() elapsed time per exec: "
                + stopWatch.getTotalTimeMillis()/(double)total + "ms");
        System.out.println("userItemDaoInAnyCast() elapsed time per exec until: "
                + stopWatch2.getTotalTimeMillis()/(double)total + "ms");
    }

    @Test
    public void userItemDaoWithoutCacheInAnyCast() throws Exception {
        //初始化配置
        JavaUuidSessionIdGenerator generator = new JavaUuidSessionIdGenerator();
        dao.setUserItemCache(null);

        //线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 100,
                60, TimeUnit.SECONDS, new LinkedBlockingDeque<>());

        //计时器不含初始化的时间
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        StopWatch stopWatch2 = new StopWatch();
        stopWatch2.start();

        Collection<Userinfo> oldItems = dao.getUsers(0, 2);

        int total = 1000;
        testSQL(total, executor, dao, generator);

        long completed;
        do {
            completed = executor.getCompletedTaskCount();
            System.out.println("userItemDaoWithoutCacheInAnyCast() tested: " + completed + "/" + total);
            Thread.sleep(500);
        } while (completed != total);

        Assert.assertTrue(dao.getUsers(0, 2).size() == oldItems.size());

        //最后一条记录要能执行完
        stopWatch.stop();

        dao.waitAction();
        stopWatch2.stop();

        System.out.println("userItemDaoWithoutCacheInAnyCast() elapsed time per exec: "
                + stopWatch.getTotalTimeMillis()/(double)total + "ms");
        System.out.println("userItemDaoWithoutCacheInAnyCast() elapsed time per exec until: "
                + stopWatch2.getTotalTimeMillis()/(double)total + "ms");
    }

    @After
    public void userItemDaoInAnyCastEnd() {
        Assert.assertEquals("has errors", 0, counter.getCount());
    }
}
