package com.tinder.chat.infrastructure.adapter.out.provider;

import com.tinder.chat.application.port.out.profile.ProfilePort;
import com.tinder.chat.infrastructure.adapter.out.cache.ProfileCacheClient;
import com.tinder.chat.infrastructure.adapter.out.http.ProfileRestAdapter;
import com.tinder.chat.shared.dto.external.ProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileProviderAdapter implements ProfilePort {

    private final ProfileCacheClient profileCacheClient;
    private final ProfileRestAdapter profileRestAdapter;

    public Map<UUID, ProfileResponse> getProfilesMap(Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> requestIds = new ArrayList<>(userIds);

        List<ProfileResponse> cachedProfiles = profileCacheClient.getCachedProfiles(requestIds);

        Map<UUID, ProfileResponse> resultMap = cachedProfiles.stream()
                .collect(Collectors.toMap(ProfileResponse::userId, Function.identity()));

        List<UUID> missingIds = requestIds.stream()
                .filter(id -> !resultMap.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            log.debug("Cache miss for {} profiles. Fetching from Profile Service via HTTP", missingIds.size());
            List<ProfileResponse> fetchedProfiles = profileRestAdapter.batchProfiles(missingIds);

            if (fetchedProfiles != null && !fetchedProfiles.isEmpty()) {
                fetchedProfiles.forEach(p -> resultMap.put(p.userId(), p));
                profileCacheClient.cacheProfiles(fetchedProfiles);
            }
        }

        return resultMap;
    }
}