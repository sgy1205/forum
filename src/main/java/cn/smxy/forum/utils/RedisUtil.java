package cn.smxy.forum.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * spring redis 工具类
 *
 * @author ruoyi
 **/
@SuppressWarnings(value = {"unchecked", "rawtypes"})
@Component
public class RedisUtil {
    private static final Logger log = LoggerFactory.getLogger(RedisUtil.class);
    @Autowired
    public RedisTemplate redisTemplate;

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key) {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    public long deleteObject(final Collection collection) {
        return redisTemplate.delete(collection);
    }

    /**
     * 缓存List数据
     *
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList) {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getCacheList(final String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 缓存Set
     *
     * @param key     缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet) {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext()) {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    public <T> Map<String, T> getCacheMap(final String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key   Redis键
     * @param hKey  Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey) {
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * 删除Hash中的数据
     *
     * @param key
     * @param hKey
     */
    public void delCacheMapValue(final String key, final String hKey) {
        HashOperations hashOperations = redisTemplate.opsForHash();
        hashOperations.delete(key, hKey);
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys) {
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 弹出数据并删除取出的数据
     * @param key Redis键
     * @return
     * @param <T>
     */
    public <T> T popFromList(String key) {
        return (T) redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 批量弹出并反序列化为对象列表（原子 Lua）
     * @param key Redis键
     * @param count 取出数量
     * @return
     * @param <T>
     */
    public <T> List<T> popFromList(String key, long count, Class<T> clazz) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        String luaScript =
                "local results = {} " +
                        "local listLen = redis.call('llen', KEYS[1]) " +
                        "if listLen == 0 then return results end " +
                        "local popCount = math.min(tonumber(ARGV[1]), listLen) " +
                        "for i = 1, popCount do " +
                        "    local val = redis.call('lpop', KEYS[1]) " +
                        "    if val then " +
                        "        table.insert(results, val) " +
                        "    else " +
                        "        break " +
                        "    end " +
                        "end " +
                        "return results";

        // 直接返回List类型，而不是byte[][]
        RedisScript<List> script = RedisScript.of(luaScript, List.class);

        try {
            // 直接获取反序列化后的对象列表
            List<T> results = (List<T>) redisTemplate.execute(
                    script,
                    Collections.singletonList(key),
                    count
            );

            return results != null ? results : Collections.emptyList();

        } catch (Exception e) {
            log.error("批量弹出列表失败, key={}, count={}", key, count, e);
            return Collections.emptyList();
        }
    }

    /**
     * 往 List 右侧追加一条数据（不会覆盖原有数据）
     * @param key   键
     * @param value 单条数据
     * @return 追加后列表长度
     */
    public <T> void addToListTail(String key, T value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 往 List 右侧批量追加数据（不会覆盖原有数据）
     * @param key   键
     * @param valueList 数据列表
     * @return 追加后列表长度
     */
    public <T> void addAllToListTail(String key, List<T> valueList) {
        redisTemplate.opsForList().rightPushAll(key, valueList);
    }

    /**
     * 通用计数器：为指定 key 下某个 field 原子累加 delta
     * @param key   Redis Hash 的 key（如 "postLikesUpdate"、"goodsStock" 等）
     * @param id    业务主键，转成 String 作为 Hash 的 field
     * @param delta 变化量：+1、-1、+5、-10 均可
     */
    public void incrCount(String key, Serializable id, long delta) {
        redisTemplate.opsForHash().increment(key, String.valueOf(id), delta);
    }

    /**
     * 原子读取整个 Hash 并立即删除，返回 Map<String, Object>
     * @param key Redis Hash 的 key
     * @return Map<String, Object>（空则返回 emptyMap）
     */
    public Map<Object, Object> popHashAndDelete(String key) {
        String lua =
                "local data = redis.call('HGETALL', KEYS[1]) " +
                        "redis.call('DEL', KEYS[1]) " +
                        "return data";

        List<String> raw = (List<String>) redisTemplate.execute(
                RedisScript.of(lua, List.class),
                Collections.singletonList(key));

        if (raw == null || raw.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Object, Object> result = new HashMap<>(raw.size() >> 1);
        for (int i = 0; i < raw.size(); i += 2) {
            result.put(raw.get(i), raw.get(i + 1));
        }
        return result;
    }
}
