package com.tinder.chat.infrastructure.provider;

import com.tinder.chat.chat.dto.ProfileResponse;
import com.tinder.chat.infrastructure.adapter.ProfileServiceClient;
import com.tinder.chat.infrastructure.redis.ProfileRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileProvider {

    private final ProfileRedisService profileRedisService;
    private final ProfileServiceClient profileServiceClient;

    public Map<UUID, ProfileResponse> getProfilesMap(Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> requestIds = new ArrayList<>(userIds);

        List<ProfileResponse> cachedProfiles = profileRedisService.getCachedProfiles(requestIds);

        Map<UUID, ProfileResponse> resultMap = cachedProfiles.stream()
                .collect(Collectors.toMap(ProfileResponse::userId, Function.identity()));

        List<UUID> missingIds = requestIds.stream()
                .filter(id -> !resultMap.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            log.debug("Cache miss for {} profiles. Fetching from Profile Service via HTTP", missingIds.size());
            List<ProfileResponse> fetchedProfiles = profileServiceClient.batchProfiles(missingIds);

            if (fetchedProfiles != null && !fetchedProfiles.isEmpty()) {
                fetchedProfiles.forEach(p -> resultMap.put(p.userId(), p));
                profileRedisService.cacheProfiles(fetchedProfiles);
            }
        }

        return resultMap;
    }
}