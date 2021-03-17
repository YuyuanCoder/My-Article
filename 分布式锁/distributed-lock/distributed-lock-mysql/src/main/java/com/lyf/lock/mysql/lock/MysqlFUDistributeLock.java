package com.lyf.lock.mysql.lock;

import com.lyf.lock.base.AbstractLock;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * fu(for update)锁，可重入基于行锁，不支持行锁的无效或锁表，支持阻塞和非阻塞
 * create table fu_distribute_lock(
 * id int unsigned auto_increment primary key,
 * lock_name varchar(100) not null,
 * unique(lock_name)
 * ) engine=innodb;
 */
@Slf4j
public class MysqlFUDistributeLock extends AbstractLock {
    public static final String SELECT_SQL = "select * from fu_distribute_lock where `lock_name`=? for update";
    public static final String INSERT_SQL = "insert into fu_distribute_lock(lock_name) values(?)";
    private final DataSource dataSource;
    private ExecutorService threadPoolExecutor = Executors.newSingleThreadExecutor();
    private Connection connection;
    private String lockName;

    public MysqlFUDistributeLock(DataSource dataSource, String lockName) {
        this.lockName = lockName;
        this.dataSource = dataSource;
    }

    @Override
    public void lock() {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            for (; ; ) {
                connection = dataSource.getConnection();
                connection.setAutoCommit(false);
                statement = connection.prepareStatement(SELECT_SQL);
                statement.setString(1, lockName);
                resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return;
                }
                Utils.gracefulClose(resultSet, statement, connection);
                log.info("锁记录不存在，正在创建");
                Connection insertConnection = dataSource.getConnection();
                PreparedStatement insertStatement = null;
                try {
                    insertStatement = insertConnection.prepareStatement(INSERT_SQL);
                    insertStatement.setString(1, lockName);
                    if (insertStatement.executeUpdate() == 1) {
                        log.info("创建锁记录成功");
                    }
                } catch (Exception e) {
                } finally {
                    Utils.gracefulClose(insertStatement, insertConnection);
                }
            }
        } catch (Exception e) {
            log.error("获取锁异常", e);
        } finally {
            Utils.gracefulClose(resultSet, statement);
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        final Future<?> future = threadPoolExecutor.submit(() -> {
            try {
                lock();
                return 1;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        try {
            final Object o = future.get(time, unit);
            if (o == null) {
                future.cancel(true);
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SneakyThrows
    @Override
    public void unlock() {
        connection.commit();
        connection.close();
    }

}
