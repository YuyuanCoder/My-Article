package com.lyf.lock.redis.lock;

import com.lyf.lock.base.AbstractLock;
import org.redisson.RedissonRedLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * 红锁
 *
 * @author 罗宇峰
 * @version V1.0
 */
public class RedisRedLock extends AbstractLock {

    private RedissonRedLock redLock;

    public RedisRedLock() {
    }

    public RedisRedLock(RedissonRedLock redLock) {
        this.redLock = redLock;
    }

    @Override
    public void lock() {
        redLock.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        redLock.lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return redLock.tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return redLock.tryLock(time, unit);
    }

    @Override
    public void unlock() {
        redLock.unlock();
    }

    @Override
    public Condition newCondition() {
        return redLock.newCondition();
    }
}
