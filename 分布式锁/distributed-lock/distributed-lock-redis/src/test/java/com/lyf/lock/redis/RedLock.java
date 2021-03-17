package com.lyf.lock.redis;

import com.lyf.lock.redis.lock.RedisRedLock;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author 罗宇峰
 * @version V1.0
 */
public class RedLock {

    public static RLock create (String url, String key){
        Config config = new Config();
        config.useSingleServer().setAddress(url);
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient.getLock(key);
    }

    RedissonRedLock redissonRedLock = new RedissonRedLock(
            create("redis://xxxx:xxxx","lock_key1"),
            create("redis://xxxx:xxxx","lock_key2"),
            create("redis://xxxx:xxxx","lock_key3"));
    RedisRedLock redLock = new RedisRedLock(redissonRedLock);

    private ExecutorService executorService = Executors.newCachedThreadPool();

    @Test
    public void test() throws Exception {
        int[] count = {0};
        for (int i = 0; i < 2; i++) {
            executorService.submit(() -> {
                try {
                    redLock.lock();
                    count[0]++;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        redLock.unlock();
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
