package com.lyf.lock.etcd;

import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;

import java.util.concurrent.*;

/**
 * @author 罗宇峰
 * @version V1.0
 */
public class Test {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // create client
        Client client = Client.builder().endpoints("http://127.0.0.1:2379").build();
        KV kvClient = client.getKVClient();

        ByteSequence key = ByteSequence.from("leolee_key".getBytes());
        ByteSequence value = ByteSequence.from("leolee_value".getBytes());

        // put the key-value
        kvClient.put(key, value).get();

        // get the CompletableFuture
        CompletableFuture<GetResponse> getFuture = kvClient.get(key);

        // get the value from CompletableFuture
        GetResponse response = getFuture.get();
        response.getKvs().stream().forEach(v -> System.out.println(new String(v.getValue().getBytes())));
        // delete the key
        // kvClient.delete(key).get();
//        client.close();
//        long TTL = 10;
//        Lease leaseClient = client.getLeaseClient();
//        long  leaseId = leaseClient.grant(TTL).get().getID();
//        Lock lockClient = client.getLockClient();
//        lockClient.lock(ByteSequence.from("lock".getBytes()),leaseId);
//        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
//        long period = TTL - TTL / 5;
//        executorService.scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//                leaseClient.keepAliveOnce(leaseId);
//            }
//        },period,period,TimeUnit.SECONDS);
//        lockClient.close();


    }
}
