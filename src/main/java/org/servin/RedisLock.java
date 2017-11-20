package org.servin;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DigestUtils;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.UUID;

/**
 * Created by servin on 2017/11/8.
 */
public class RedisLock {
    private static StringRedisTemplate stringRedisTemplate;
    private static String lockKey;
    private static String lockValue;
    private boolean locked = true;
    private static final RedisScript<Boolean> SETNX_AND_EXPIRE_SCRIPT;
    static {
        StringBuilder sb = new StringBuilder();
        sb.append("if (redis.call('setnx', KEYS[1], ARGV[1]) == 1) then\n");
        sb.append("\tredis.call('expire', KEYS[1], tonumber(ARGV[2]))\n");
        sb.append("\treturn true\n");
        sb.append("else\n");
        sb.append("\treturn false\n");
        sb.append("end");
        SETNX_AND_EXPIRE_SCRIPT = new RedisScriptImpl<Boolean>(sb.toString(), Boolean.class);
    }

    private static final RedisScript<Boolean> DEL_IF_GET_EQUALS;
    static {
        StringBuilder sb = new StringBuilder();
        sb.append("if (redis.call('get', KEYS[1]) == ARGV[1]) then\n");
        sb.append("\tredis.call('del', KEYS[1])\n");
        sb.append("\treturn true\n");
        sb.append("else\n");
        sb.append("\treturn false\n");
        sb.append("end");
        DEL_IF_GET_EQUALS = new RedisScriptImpl<Boolean>(sb.toString(), Boolean.class);
    }

    public RedisLock(StringRedisTemplate stringRedisTemplate, String lockKey) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.lockKey = lockKey;
        this.lockValue = UUID.randomUUID().toString() + "." + System.currentTimeMillis();
    }
    private boolean doTryLock(int lockSeconds) throws Exception {
        if (locked) {
            throw new IllegalStateException("already locked!");
        }
        locked = stringRedisTemplate.execute(SETNX_AND_EXPIRE_SCRIPT, Collections.singletonList(lockKey), lockValue,
                String.valueOf(lockSeconds));
        return locked;
    }
    /**
     * 尝试获得锁，成功返回true，如果失败立即返回false
     *
     * @param lockSeconds 加锁的时间(秒)，超过这个时间后锁会自动释放
     */
    public boolean tryLock(int lockSeconds) {
        try {
            return doTryLock(lockSeconds);
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * 轮询的方式去获得锁，成功返回true，超过轮询次数或异常返回false
     *
     * @param lockSeconds       加锁的时间(秒)，超过这个时间后锁会自动释放
     * @param tryIntervalMillis 轮询的时间间隔(毫秒)
     * @param maxTryCount       最大的轮询次数
     */
    public boolean tryLock(final int lockSeconds, final long tryIntervalMillis, final int maxTryCount) {
        int tryCount = 0;
        while (true) {
            if (++tryCount >= maxTryCount) {
                // 获取锁超时
                return false;
            }
            try {
                if (doTryLock(lockSeconds)) {
                    return true;
                }
            } catch (Exception e) {
                return false;
            }
            try {
                Thread.sleep(tryIntervalMillis);
            } catch (InterruptedException e) {
                return false;
            }
        }
    }
    /**
     * 解锁操作
     */
    public void unlock() {
        if (!locked) {
            throw new IllegalStateException("not locked yet!");
        }
        locked = false;
        // 忽略结果
        stringRedisTemplate.execute(DEL_IF_GET_EQUALS, Collections.singletonList(lockKey), lockValue);
    }
    private static class RedisScriptImpl<T> implements RedisScript<T> {
        private final String script;
        private final String sha1;
        private final Class<T> resultType;
        public RedisScriptImpl(String script, Class<T> resultType) {
            this.script = script;
            this.sha1 = DigestUtils.sha1DigestAsHex(script);
            this.resultType = resultType;
        }
        @Override
        public String getSha1() {
            return sha1;
        }
        @Override
        public Class<T> getResultType() {
            return resultType;
        }
        @Override
        public String getScriptAsString() {
            return script;
        }
    }

}

