package com.tinder.chat.chat.dto;

import java.util.List;

public record CursorPage<T>(
        List<T> data,
        Long nextCursor,
        boolean hasNext
) {
}