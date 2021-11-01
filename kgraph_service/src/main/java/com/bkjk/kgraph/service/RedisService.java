package com.bkjk.kgraph.service;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;


@Service
public class RedisService {
    public static final Logger logger = LoggerFactory.getLogger(RedisService.class);

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    public <T> boolean set(String key ,T value){

        try {
            String val = beanToString(value);

            if(val == null || val.length() <= 0){
                return false;
            }

            stringRedisTemplate.opsForValue().set(key, val);
            return true;
        }catch (Exception e){
            logger.warn("Fail to set redis, key = %s, error = [%s]", key, e.getMessage());
            return false;
        }
    }

    public <T> T get(String key, Class<T> clazz){
        try {
            String value = stringRedisTemplate.opsForValue().get(key);

            return stringToBean(value, clazz);
        } catch (Exception e){
            logger.warn("Fail to get redis, key = %s, error = [%s]", key, e.getMessage());
            return null;
        }
    }

    public boolean remove(String key) {
        try {
            stringRedisTemplate.delete(key);
            return true;
        } catch (Exception e){
            logger.warn("Fail to get redis, key = %s, error = [%s]", key, e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T stringToBean(String value, Class<T> clazz) {
        if(value == null || value.length() <= 0 || clazz == null){
            return null;
        }

        if(clazz == int.class || clazz == Integer.class){
            return (T)Integer.valueOf(value);
        }
        else if(clazz == long.class || clazz == Long.class){
            return (T)Long.valueOf(value);
        }
        else if(clazz == String.class){
            return (T)value;
        } else {
            return JSONObject.toJavaObject(JSONObject.parseObject(value),clazz);
        }
    }


    private <T> String beanToString(T value) {

        if(value == null) {
            return null;
        }

        Class <?> clazz = value.getClass();
        if(clazz == int.class || clazz == Integer.class){
            return "" + value;
        }
        else if(clazz == long.class || clazz == Long.class){
            return "" + value;
        } else if(clazz == String.class){
            return (String)value;
        } else {
            return JSONObject.toJSONString(value);
        }
    }

}
