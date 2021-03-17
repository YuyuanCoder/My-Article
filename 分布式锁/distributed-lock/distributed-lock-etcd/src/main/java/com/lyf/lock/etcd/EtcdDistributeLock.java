package com.lyf.lock.etcd;

import com.google.common.collect.Maps;
import com.lyf.lock.base.AbstractLock;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.lock.LockResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;

/**
 * 基于ETCD实现分布式锁
 *
 * @author 罗宇峰
 * @version V1.0
 */
@Slf4j
@Data
public class EtcdDistributeLock extends AbstractLock {

    private Client client;
    private Lock lockClient;
    private Lease leaseClient;
    private String lockKey;
    private String lockPath;
    /**
     * 锁的次数
     */
    private AtomicInteger lockCount;
    /**
     * 租约有效期,防止客户端崩溃，可在租约到期后自动释放锁；另一方面，正常执行过程中，会自动进行续租,单位 ns
     */
    private Long leaseTTL;
    /**
     * 续约锁租期的定时任务，初次启动延迟，单位默认为 s,默认为1s，可根据业务定制设置
     */
    private Long initialDelay = 0L;
    /**
     * 定时任务线程池类
     */
    ScheduledExecutorService service = null;
    /**
     * 保存线程与锁对象的映射，锁对象包含重入次数，重入次数的最大限制为Int的最大值
     */
    private final ConcurrentMap<Thread, LockData> threadData = Maps.newConcurrentMap();

    public EtcdDistributeLock() {
    }

    public EtcdDistributeLock(Client client, String lockKey, long leaseTTL, TimeUnit unit) {
        this.client = client;
        lockClient = client.getLockClient();
        leaseClient = client.getLeaseClient();
        this.lockKey = lockKey;
        // 转纳秒
        this.leaseTTL = unit.toNanos(leaseTTL);
        service = Executors.newSingleThreadScheduledExecutor();
    }


    @Override
    public void lock() {
        // 检查重入性
        Thread currentThread = Thread.currentThread();
        LockData oldLockData = threadData.get(currentThread);
        if (oldLockData != null && oldLockData.isLockSuccess()) {
            // re-entering
            int lockCount = oldLockData.lockCount.incrementAndGet();
            if (lockCount < 0) {
                throw new Error("超出可重入次数限制");
            }
            return;
        }

        // 记录租约 ID
        Long leaseId = 0L;
        try {
            leaseId = leaseClient.grant(TimeUnit.NANOSECONDS.toSeconds(leaseTTL)).get().getID();
            // 续租心跳周期
            long period = leaseTTL - leaseTTL / 5;
            // 启动定时任务续约
            service.scheduleAtFixedRate(new EtcdDistributeLock.KeepAliveRunnable(leaseClient, leaseId),
                    initialDelay, period, TimeUnit.NANOSECONDS);
            LockResponse lockResponse = lockClient.lock(ByteSequence.from(lockKey.getBytes()), leaseId).get();
            if (lockResponse != null) {
                lockPath = lockResponse.getKey().toString(Charset.forName("utf-8"));
                log.info("获取锁成功,锁路径:{},线程:{}", lockPath, currentThread.getName());
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("获取锁失败", e);
            return;
        }
        // 获取锁成功，锁对象设置
        LockData newLockData = new LockData(currentThread, lockKey);
        newLockData.setLeaseId(leaseId);
        newLockData.setService(service);
        threadData.put(currentThread, newLockData);
        newLockData.setLockSuccess(true);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        super.lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return super.tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return super.tryLock(time, unit);
    }


    @Override
    public void unlock() {
        Thread currentThread = Thread.currentThread();
        LockData lockData = threadData.get(currentThread);
        if (lockData == null) {
            throw new IllegalMonitorStateException("You do not own the lock: " + lockKey);
        }
        int newLockCount = lockData.lockCount.decrementAndGet();
        if (newLockCount > 0) {
            return;
        }
        if (newLockCount < 0) {
            throw new IllegalMonitorStateException("Lock count has gone negative for lock: " + lockKey);
        }
        try {
            // 释放锁
            if (lockPath != null) {
                lockClient.unlock(ByteSequence.from(lockPath.getBytes())).get();
            }
            if (lockData != null) {
                // 关闭定时任务
                lockData.getService().shutdown();
                // 删除租约
                if (lockData.getLeaseId() != 0L) {
                    leaseClient.revoke(lockData.getLeaseId());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("解锁失败", e);
        } finally {
            // 移除当前线程资源
            threadData.remove(currentThread);
        }
    }


    @Override
    public Condition newCondition() {
        return super.newCondition();
    }

    /**
     * 心跳续约线程类
     */
    public static class KeepAliveRunnable implements Runnable {
        private Lease leaseClient;
        private long leaseId;

        public KeepAliveRunnable(Lease leaseClient, long leaseId) {
            this.leaseClient = leaseClient;
            this.leaseId = leaseId;
        }

        @Override
        public void run() {
            // 对该leaseid进行一次续约
            leaseClient.keepAliveOnce(leaseId);
        }
    }

}
