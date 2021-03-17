package com.lyf.lock.mysql.lock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 看门狗
 *
 * @author 罗宇峰
 * @version V1.0
 */
@Slf4j
public class WatchDog {
    private volatile boolean isStop;
    private long watchTime;
    private Runnable target;
    private Thread thread;

    public WatchDog(Runnable target, long watchTime, TimeUnit timeUnit) {
        this.watchTime = timeUnit.toMillis(watchTime);
        this.target = target;
        this.thread = new Thread(() -> {
            while (!isStop) {
                try {
                    Thread.sleep(this.watchTime);
                } catch (InterruptedException e) {
                }
                this.target.run();
            }
        }, "lock-watch-dog");
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        this.isStop = true;
    }
}
