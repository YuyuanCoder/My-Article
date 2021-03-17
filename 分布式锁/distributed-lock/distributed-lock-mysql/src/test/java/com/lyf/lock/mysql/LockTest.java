package com.lyf.lock.mysql;

import com.alibaba.druid.pool.DruidDataSource;
import com.lyf.lock.mysql.lock.MysqlFUDistributeLock;
import com.lyf.lock.mysql.lock.MysqlIDDistributeLock;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Test
 *
 * @author 罗宇峰
 * @version V1.0
 */
public class LockTest {
    private DataSource dataSource;
    private String key = "test_lock";
    private ExecutorService executorService = Executors.newCachedThreadPool();

    @Before
    public void before() throws Exception {
        initDataSource();
    }

    private void initDataSource() throws SQLException {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUrl("jdbc:mysql://127.0.0.1:3306/test?characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        druidDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        druidDataSource.setPassword("root");
        druidDataSource.setUsername("root");
        druidDataSource.setMaxActive(1000);
        druidDataSource.setInitialSize(1000);
        druidDataSource.setMaxWait(50000);
        druidDataSource.init();
        dataSource = druidDataSource;
    }

    @Test
    public void testMysqlFUDistributeLock() throws Exception {
        int[] count = {0};
        for (int i = 0; i < 10000; i++) {
            executorService.submit(() -> {
                final MysqlFUDistributeLock lock = new MysqlFUDistributeLock(dataSource, key);
                try {
                    lock.lock();
//                    if (lock.tryLock(schedulerLockInfo, 1000, TimeUnit.MILLISECONDS)) {
                    count[0]++;
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        lock.unlock();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        System.out.println(count[0]);
    }

    @Test
    public void testMysqlIDDistributeLock() throws Exception {
        int[] count = {0};
        for (int i = 0; i < 10000; i++) {
            executorService.submit(() -> {
                final MysqlIDDistributeLock lock = new MysqlIDDistributeLock(dataSource, 3000, key);
                try {
                    lock.lock();
//                    if (lock.tryLock(schedulerLockInfo, 1000, TimeUnit.MILLISECONDS)) {
                    count[0]++;
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        lock.unlock();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        System.out.println(count[0]);
    }
}
