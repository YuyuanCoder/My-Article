package com.lyf.lock.redis.lock;

import com.lyf.lock.base.AbstractLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * 基于Redis实现锁
 *
 * @author 罗宇峰
 * @version V1.0
 */
public class RedisLock extends AbstractLock {

    private RedissonClient client;
    private String key;

    public RedisLock(RedissonClient client, String key) {
        this.client = client;
        this.key = key;
    }

    @Override
    public void lock() {
        client.getLock(key).lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        client.getLock(key).lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return client.getLock(key).tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return client.getLock(key).tryLock(time, unit);
    }

    @Override
    public void unlock() {
        client.getLock(key).unlock();
    }

    @Override
    public Condition newCondition() {
        return client.getLock(key).newCondition();
    }
}
