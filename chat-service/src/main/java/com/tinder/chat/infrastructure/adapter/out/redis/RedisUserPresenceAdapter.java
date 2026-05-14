package com.tinder.chat.infrastructure.adapter.out.redis;

import com.tinder.chat.application.port.out.presence.UserPresencePort;
import com.tinder.chat.infrastructure.config.properties.RedisPresenceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisUserPresenceAdapter implements UserPresencePort {
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisPresenceProperties presenceProperties;

    @Override
    public boolean isUserOnline(UUID userId) {
        String key = presenceProperties.sessionKeyPrefix() + userId;
        Long sessions = stringRedisTemplate.opsForSet().size(key);
        return sessions != null && sessions > 0;
    }

    @Override
    public Map<UUID, Boolean> getPresenceBatch(Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> idList = new ArrayList<>(userIds);

        List<Object> results = stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> Object execute(RedisOperations<K, V> operations) {
                RedisOperations<String, String> stringOps = (RedisOperations<String, String>) operations;

                for (UUID id : idList) {
                    stringOps.hasKey(presenceProperties.sessionKeyPrefix() + id);
                }
                return null;
            }
        });
        Map<UUID, Boolean> presenceMap = new HashMap<>();
        for (int i = 0; i < idList.size(); i++) {
            Boolean isOnline = (Boolean) results.get(i);
            presenceMap.put(idList.get(i), Boolean.TRUE.equals(isOnline));
        }

        return presenceMap;
    }
}