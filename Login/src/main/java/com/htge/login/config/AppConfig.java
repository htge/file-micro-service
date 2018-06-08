package com.htge.login.config;

import com.htge.login.model.*;
import com.htge.login.rabbit.AuthRPCData;
import com.htge.login.rabbit.RPCServer;
import com.mchange.v2.c3p0.DataSources;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
@EnableAutoConfiguration
public class AppConfig implements ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /* Mybatis configuration */
    @Bean
    SqlSessionFactoryBean sqlSessionFactoryBean() throws SQLException {
        UserinfoDao dao = context.getBean(UserinfoDao.class);

        BasicDataSource basicDataSource = context.getBean(BasicDataSource.class);
        DataSource dataSource = DataSources.pooledDataSource(basicDataSource);

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTransactionFactory(new JdbcTransactionFactory());

        //UserinfoDao
        dao.setFactoryBean(factoryBean);

        updateRedisConfiguration();

        return factoryBean;
    }

    @Bean
    MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("com.htge.login.model.mapper");
        configurer.setSqlSessionFactoryBeanName("sqlSessionFactoryBean");
        return configurer;
    }

    /* Redis configuration */
    private void updateRedisConfiguration() {
        RedisSessionDB sessionDB = context.getBean(RedisSessionDB.class);
        RedisSessionDao sessionDao = context.getBean(RedisSessionDao.class);
        RedisUserItemCache cache = context.getBean(RedisUserItemCache.class);
        RedisFactoryManager manager = context.getBean(RedisFactoryManager.class);
        UserinfoDao dao = context.getBean(UserinfoDao.class);
        AuthRPCData rpcData = context.getBean(AuthRPCData.class);

        //以下Bean是由Spring内部自动生成的，详细见application.properties或者yml
        JedisConnectionFactory connectionFactory = context.getBean(JedisConnectionFactory.class);
        CachingConnectionFactory factory = context.getBean(CachingConnectionFactory.class);

        //RedisFactoryManager
        manager.setJedisConnectionFactory(connectionFactory);

        //RedisSessionDB
        sessionDB.setTemplate(manager.getTemplate(1));
        sessionDB.setUserTemplate(manager.getTemplate(2));

        //RedisSessionDao
        sessionDao.setSessionDB(sessionDB);

        //RedisUserItemCache
        cache.setTemplate(manager.getTemplate(0));

        //UserinfoDao
        dao.setUserItemCache(cache);

        //RPCData
        rpcData.setSessionDB(sessionDB);
        rpcData.setUserinfoDao(dao);

        //RPCServer
        RPCServer.createInstance(factory, rpcData);
    }
}
