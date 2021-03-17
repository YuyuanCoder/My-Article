package com.lyf.lock.mysql.lock;

import com.lyf.lock.base.AbstractLock;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * create table id_distribute_lock(
 * id int unsigned auto_increment primary key,
 * lock_name varchar(100) not null,
 * expire_time bigint not null,
 * thread_id varchar(100) not null,
 * unique(lock_name)
 * ) engine=myisam;
 */
@Slf4j
public class MysqlIDDistributeLock extends AbstractLock {
    private static final String SELECT_SQL_FORMAT = "select * from id_distribute_lock where lock_name=?";
    private static final String UPDATE_SQL_FORMAT = "update id_distribute_lock set expire_time=? where lock_name=?";
    private static final String INSERT_SQL_FORMAT = "insert into id_distribute_lock(lock_name,expire_time,thread_id) values(?,?,?)";
    private static final String DELETE_SQL_FORMAT = "delete from id_distribute_lock where lock_name=?";
    private final DataSource dataSource;
    private ExecutorService threadPoolExecutor = Executors.newSingleThreadExecutor();
    private int expireTime;
    private long reletTime;
    private WatchDog watchDog;
    private String threadId;
    private String lockName;

    public MysqlIDDistributeLock(DataSource dataSource, int expireTime, String lockName) {
        this.dataSource = dataSource;
        this.expireTime = expireTime;
        this.reletTime = this.expireTime / 2;
        this.lockName = lockName;
        threadId = Thread.currentThread().getId() + "-" + UUID.randomUUID().toString();
        watchDog = new WatchDog(() -> reletAndCheck(), reletTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public void lock() {
        watchDog.start();
        while (true) {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(INSERT_SQL_FORMAT)) {
                statement.setString(1, this.lockName);
                statement.setLong(2, System.currentTimeMillis() + expireTime);
                statement.setString(3, this.threadId);

                try {
                    // 获取锁成功
                    if (statement.executeUpdate() > 0) {
                        log.info("获取锁成功");
                        break;
                    }
                } catch (SQLIntegrityConstraintViolationException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 睡眠100ms继续获取锁，可根据竞争情况修改
                Thread.sleep(100);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void reletAndCheck() {
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        try {
            log.info("开始查询锁");
            connection = dataSource.getConnection();
            preparedStatement = connection.prepareStatement(SELECT_SQL_FORMAT);
            preparedStatement.setString(1, this.lockName);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                final String threadId = resultSet.getString("thread_id");
                final long expireTime = resultSet.getLong("expire_time");
                log.debug("thread_id:{}", threadId);
                if (this.threadId.equals(threadId)) {
                    // 续租
                    log.info("续租中");
                    try (PreparedStatement updatePreparedStatement = connection.prepareStatement(UPDATE_SQL_FORMAT)) {
                        updatePreparedStatement.setLong(1, expireTime + reletTime);
                        updatePreparedStatement.setString(2, this.lockName);
                        updatePreparedStatement.executeUpdate();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // 检查时间是否过期
                    if (System.currentTimeMillis() > expireTime) {
                        log.info("锁：{}过期删除中", lockName);
                        delete();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Utils.gracefulClose(resultSet, preparedStatement, connection);
        }
    }

    private void delete() {
        log.info("删除锁");
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = this.dataSource.getConnection();
            preparedStatement = connection.prepareStatement(DELETE_SQL_FORMAT);
            preparedStatement.setString(1, this.lockName);
            if (preparedStatement.executeUpdate() == 1) {
                log.info("刪除成功");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Utils.gracefulClose(preparedStatement, connection);
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
            if (future.get(time, unit) == null) {
                future.cancel(true);
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void unlock() {
        watchDog.stop();
        delete();
    }

}
