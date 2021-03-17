package com.lyf.lock.zk;

import com.lyf.lock.base.AbstractLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.RetryNTimes;

import java.util.concurrent.TimeUnit;

/**
 * ZooKeeper 分布式锁
 *
 * @author 罗宇峰
 * @version V1.0
 */
@Slf4j
public class ZKLock extends AbstractLock {

    /**
     * 1.Connect to zk
     */
    private CuratorFramework client;

    private InterProcessLock lock;

    public ZKLock(String zkAddress, String lockPath) {
        // 1.Connect to zk
        client = CuratorFrameworkFactory.newClient(
                zkAddress,
                new RetryNTimes(5, 5000)
        );
        client.start();
        if (client.getState() == CuratorFrameworkState.STARTED) {
            log.info("zk client start successfully!");
            log.info("zkAddress:{},lockPath:{}", zkAddress, lockPath);
        } else {
            throw new RuntimeException("客户端启动失败。。。");
        }
        this.lock = defaultLock(lockPath);
    }

    private InterProcessLock defaultLock(String lockPath) {
        return new InterProcessMutex(client, lockPath);
    }

    @Override
    public void lock() {
        try {
            this.lock.acquire();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean tryLock() {
        boolean flag;
        try {
            flag = this.lock.acquire(0, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return flag;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        boolean flag;
        try {
            flag = this.lock.acquire(time, unit);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return flag;
    }

    @Override
    public void unlock() {
        try {
            this.lock.release();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
