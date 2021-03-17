package com.lyf.lock.etcd;

import lombok.Data;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 锁对象封装
 *
 * @author 罗宇峰
 * @version V1.0
 */
@Data
public class LockData {
    private String lockKey;
    private boolean lockSuccess;
    private long leaseId;
    private ScheduledExecutorService service;
    private Thread owningThread;
    private String lockPath;
    final AtomicInteger lockCount = new AtomicInteger(1);

    public LockData() {
    }

    public LockData(Thread owningThread, String lockPath) {
        this.owningThread = owningThread;
        this.lockPath = lockPath;
    }
}
