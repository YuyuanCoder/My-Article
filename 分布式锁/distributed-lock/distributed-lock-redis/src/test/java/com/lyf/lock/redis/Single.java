package com.lyf.lock.redis;

import com.lyf.lock.redis.lock.RedisLock;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.*;

/**
 * @author 罗宇峰
 * @version V1.0
 */
public class Single {

    private RedissonClient getClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://xxxx:xxxx").setPassword("xxxxxx");//.setConnectionMinimumIdleSize(10).setConnectionPoolSize(10);//.setConnectionPoolSize();
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }

    private ExecutorService executorService = Executors.newCachedThreadPool();

    @Test
    public void test() throws Exception {
        int[] count = {0};
        for (int i = 0; i < 10; i++) {
            RedissonClient client = getClient();
            final RedisLock redisLock = new RedisLock(client, "lock_key");
            executorService.submit(() -> {
                try {
                    redisLock.lock();
                    count[0]++;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        redisLock.unlock();
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