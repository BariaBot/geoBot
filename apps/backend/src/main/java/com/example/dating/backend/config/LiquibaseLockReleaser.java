package com.example.dating.backend.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

/**
 * Releases stale Liquibase changelog locks before the migrations kick in.
 */
@Component
public class LiquibaseLockReleaser implements BeanPostProcessor, PriorityOrdered {

    private static final Logger log = LoggerFactory.getLogger(LiquibaseLockReleaser.class);

    private final Method createLiquibaseMethod;

    public LiquibaseLockReleaser() {
        try {
            this.createLiquibaseMethod = SpringLiquibase.class.getDeclaredMethod("createLiquibase", Connection.class);
            this.createLiquibaseMethod.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("SpringLiquibase#createLiquibase(Connection) not found", ex);
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SpringLiquibase liquibaseBean) {
            releaseLocks(liquibaseBean);
        }
        return bean;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private void releaseLocks(SpringLiquibase liquibaseBean) {
        DataSource dataSource = liquibaseBean.getDataSource();
        if (dataSource == null) {
            return;
        }
        try (Connection connection = dataSource.getConnection();
             Liquibase liquibase = createLiquibase(liquibaseBean, connection)) {
            liquibase.forceReleaseLocks();
        } catch (SQLException | LiquibaseException ex) {
            log.warn("Failed to release Liquibase changelog lock before startup", ex);
        }
    }

    private Liquibase createLiquibase(SpringLiquibase liquibaseBean, Connection connection) throws LiquibaseException {
        try {
            return (Liquibase) this.createLiquibaseMethod.invoke(liquibaseBean, connection);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new LiquibaseException("Unable to create Liquibase instance for lock release", ex);
        }
    }
}
