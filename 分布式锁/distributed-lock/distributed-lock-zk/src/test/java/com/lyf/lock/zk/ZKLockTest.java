package com.lyf.lock.zk;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ZKLock Test
 *
 * @author 罗宇峰
 * @version V1.0
 */
@Slf4j
public class ZKLockTest {

    private ExecutorService executorService = Executors.newCachedThreadPool();

    @Test
    public void testLock() throws Exception {
        ZKLock zkLock = new ZKLock("192.168.19.100:2181", "/lockPath");
        int[] num = {0};
        long start = System.currentTimeMillis();
        for (int i = 0; i < 200; i++) {
            executorService.submit(() -> {
                try {
                    zkLock.lock();
                    num[0]++;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    zkLock.unlock();
                }
            });

        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        log.info("耗时:{}", System.currentTimeMillis() - start);
        System.out.println(num[0]);
    }

    @Test
    public void testNoLock() throws Exception {
        long start = System.currentTimeMillis();
        int[] num = {0};
        for (int i = 0; i < 1000; i++) {
            executorService.submit(() -> {
                num[0]++;
            });

        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        log.info("耗时:{}", System.currentTimeMillis() - start);
        System.out.println(num[0]);
    }

    /**
     * 锁的可重入测试
     */
    @Test
    public void testRe() {
        ZKLock zkLock = new ZKLock("192.168.19.100:2181", "/lockPath");
        Thread thread1 = new Thread(() -> {
            zkLock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + "第一次入锁");
                Thread.sleep(1000);
                zkLock.lock();
                System.out.println(Thread.currentThread().getName() + "第二次入锁");
            } catch (Exception e) {
            } finally {
                zkLock.unlock();
                zkLock.unlock();
            }
        }, "thread1");
        thread1.start();

        Thread thread2 = new Thread(() -> {
            zkLock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + "第一次入锁");
            } catch (Exception e) {
            } finally {
                zkLock.unlock();
            }
        }, "thread2");
        thread2.start();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testTrySuccess() {
        ZKLock zkLock = new ZKLock("192.168.19.100:2181", "/lockPath");
        boolean b = zkLock.tryLock();
        try {
            if (b) {
                log.info("{}获取锁成功", Thread.currentThread().getName());
            } else {
                log.info("{}获取锁失败", Thread.currentThread().getName());
            }
        } finally {
            zkLock.unlock();
        }
    }

    @Test
    public void testTryFalse() {
        ZKLock zkLock = new ZKLock("192.168.19.100:2181", "/lockPath");
        Thread thread1 = new Thread(() -> {
            zkLock.lock();
            try {
                log.info("{}获得锁", Thread.currentThread().getName());
                Thread.sleep(10000);
            } catch (Exception e) {
            } finally {
                zkLock.unlock();
                log.info("{}释放锁", Thread.currentThread().getName());
            }
        }, "thread1");
        thread1.start();
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
        }
        Thread thread2 = new Thread(() -> {
            log.info("{}尝试获得锁", Thread.currentThread().getName());
            boolean b = zkLock.tryLock();
            if (b) {
                try {
                    log.info("{}获取锁成功", Thread.currentThread().getName());
                } catch (Exception e) {
                } finally {
                    zkLock.unlock();
                }
            } else {
                log.info("{}获取锁失败", Thread.currentThread().getName());
            }
        }, "thread2");
        thread2.start();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testTryTimeOut() {
        ZKLock zkLock = new ZKLock("192.168.19.100:2181", "/lockPath");
        Thread thread1 = new Thread(() -> {
            zkLock.lock();
            try {
                log.info("{}获得锁", Thread.currentThread().getName());
                Thread.sleep(10000);
            } catch (Exception e) {
            } finally {
                zkLock.unlock();
                log.info("{}释放锁", Thread.currentThread().getName());
            }
        }, "thread1");
        thread1.start();
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
        }
        Thread thread2 = new Thread(() -> {
            log.info("{}尝试获得锁", Thread.currentThread().getName());
            boolean b = false;
            try {
                b = zkLock.tryLock(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (b) {
                try {
                    log.info("{}获取锁成功", Thread.currentThread().getName());
                } catch (Exception e) {
                } finally {
                    zkLock.unlock();
                }
            } else {
                log.info("{}获取锁失败", Thread.currentThread().getName());
            }
        }, "thread2");
        thread2.start();


        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void Test() {
        ZKLock zkLock = new ZKLock("192.168.19.100:2181", "/lockPath");
        zkLock.lock();
        try {
            //do something
        } finally {
            zkLock.unlock();
        }
    }

}
