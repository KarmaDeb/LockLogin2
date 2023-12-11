package es.karmadev.redis.test;

import com.google.gson.JsonObject;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.common.api.packet.COutPacket;
import es.karmadev.locklogin.redis.RedisServiceProvider;
import es.karmadev.locklogin.redis.api.RedisService;
import es.karmadev.locklogin.redis.api.options.RedisClusterOptions;
import es.karmadev.locklogin.redis.network.RedisChannel;
import es.karmadev.locklogin.redis.network.RedisMessageQue;
import org.junit.Test;
import redis.clients.jedis.HostAndPort;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class TestCom1 {

    @Test
    public void testConnection() {
        RedisServiceProvider provider = new RedisServiceProvider();

        RedisClusterOptions options = RedisClusterOptions.builder()
                .withCluster(new HostAndPort("127.0.0.1", 6379))
                .ssl(false)
                .build();

        RedisService service = provider.serve(options);
        assertNotNull(service);
    }

    @Test
    public void testSecurity() {
        RedisServiceProvider provider = new RedisServiceProvider();

        RedisClusterOptions options = RedisClusterOptions.builder()
                .withCluster(new HostAndPort("127.0.0.1", 6379))
                .ssl(false)
                .build();

        RedisService service = provider.serve(options);
        assertNotNull(service);

        AtomicBoolean work = new AtomicBoolean(true);
        new Thread(() -> {
            Thread current = Thread.currentThread();
            synchronized (current) {
                while (work.get()) {
                    try {
                        current.wait(1);
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }).start();

        RedisChannel channel = service.createChannel("test").join();
        RedisMessageQue que = (RedisMessageQue) channel.getProcessingQue();

        COutPacket out = new COutPacket(DataType.HELLO);
        JsonObject result = out.build();

        assertThrows(SecurityException.class, () -> {
            UnsafePacket packet = new UnsafePacket(result.toString().getBytes());
            que.appendPacket(packet);
        });

        /*NetworkPacket p = que.nextPacket();
        assertNotNull(p);

        assertTrue(que.flushPacket());
        work.set(false);*/
    }
}
