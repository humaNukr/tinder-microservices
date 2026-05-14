package com.tinder.chat.shared.dto.common;

import java.util.List;

public record CursorPage<T>(
        List<T> data,
        Long nextCursor,
        boolean hasNext
) {
}