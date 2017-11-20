package org.servin;

import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by servin on 2017/11/9.
 */
public class RedisLockUtils {

    private static final String TRY_LOCK_SCRIPT;
    private String key;
    private String value;
    private String expireSeconds;
    static {
        StringBuilder sb = new StringBuilder();
        sb.append("if(redis.call('setnx',KEYS[1],ARGV[1]) == 1) then\n");
        sb.append("\tredis.call('expire',KEYS[1],tonumber(ARGV[2])\n)");
        sb.append("\treturn true\n");
        sb.append("else\n");
        sb.append("\treturn false\n");
        sb.append("end");
        TRY_LOCK_SCRIPT = sb.toString();
    }

    public RedisLockUtils(String key,String expireSeconds) {
        this.key = key;
        this.value = UUID.randomUUID().toString() + System.currentTimeMillis();;
        this.expireSeconds = expireSeconds;
    }

    public RedisLockUtils(String key){

        new RedisLockUtils(key,"2");
    }

    private static final Jedis jedis;
    static {
        jedis = RedisConnect.getJedis();
    }

    private static final String UN_LOCK_SCRIPT;
    static {
        StringBuilder sb = new StringBuilder();
        sb.append("if (redis.call('get', KEYS[1]) == ARGV[1]) then\n");
        sb.append("\tredis.call('del', KEYS[1])\n");
        sb.append("\treturn true\n");
        sb.append("else\n");
        sb.append("\treturn false\n");
        sb.append("end");
        UN_LOCK_SCRIPT = sb.toString();
    }

    public Boolean doTryLock(){
        Boolean lock = true;
        List<String> keyList = new ArrayList<String>();
        List<String> argvList = new ArrayList<String>();
        keyList.add(key);
        argvList.add(value);
        argvList.add(expireSeconds);
        Object object = jedis.eval(TRY_LOCK_SCRIPT, keyList,argvList);
        System.out.println(object);
        return lock;

    }

    public void freeLock(){
        List<String> keyList = new ArrayList<String>();
        List<String> argvList = new ArrayList<String>();

        keyList.add(key);
        argvList.add(value);
        jedis.eval(UN_LOCK_SCRIPT, keyList, argvList);
    }

    public static void main(String[] args) {
       RedisLockUtils redisLockUtils =  new RedisLockUtils("orderId","20");
       redisLockUtils.doTryLock();
    }

}
