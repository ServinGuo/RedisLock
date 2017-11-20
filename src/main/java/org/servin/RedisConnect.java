package org.servin;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.locks.ReentrantLock;

public class RedisConnect {
    private static final ReentrantLock localPool = new ReentrantLock();
    private static final ReentrantLock localJedit = new ReentrantLock();
    private static  String ADDR_ARRY = "10.22.0.160";
    private static int PORT = 6379;
    private static String AUTH = "sunline@160";
    private static int MAX_ACTIVE = 100;
    private static int MAX_IDLE = 80;
    private static int MAX_WAIT=3000;
    private static int TIMEOUT=10000;
    private static boolean TEST_ON_BORROW = false;
    private static JedisPool jedisPool = null;

    private final static int EXRP_HOUR = 60 * 60;
    private final static int EXRP_DAY = EXRP_HOUR * 24;
    private final static  int EXRP_MOUTH = EXRP_DAY * 30;

    private static void initialPool(){
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxIdle(MAX_IDLE);
            config.setMaxTotal(MAX_ACTIVE);
            config.setMaxWaitMillis(MAX_WAIT);
            config.setTestOnBorrow(TEST_ON_BORROW);
            jedisPool = new JedisPool(config,ADDR_ARRY.split(",")[0],PORT,TIMEOUT,AUTH);
        }catch (Exception e){
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxIdle(MAX_IDLE);
            config.setMaxTotal(MAX_ACTIVE);
            config.setMaxWaitMillis(MAX_WAIT);
            config.setTestOnBorrow(TEST_ON_BORROW);
            jedisPool = new JedisPool(config,ADDR_ARRY.split(",")[1],PORT,TIMEOUT,AUTH);
        }
    }

    private static void poolInit(){
        localPool.lock();
        try {
            if (jedisPool == null){
                initialPool();
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            localPool.unlock();
        }
    }

    public static Jedis getJedis(){
        localJedit.lock();
        if (jedisPool == null){
            poolInit();
        }
        Jedis jedis = null;
        try {
            if(jedisPool != null){
                jedis = jedisPool.getResource();
            }else {
                throw new Exception("连接池没有被整确初始化");
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            returnJedisResource(jedis);
            localJedit.unlock();
        }
        return jedis;
    }
    private static void returnJedisResource(final Jedis jedis){
        if (jedis != null && jedisPool != null){
            jedisPool.returnResource(jedis);
        }
    }


}
