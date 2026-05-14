package com.tinder.chat.shared.mapper;

import com.tinder.chat.domain.enums.MessageContentType;
import com.tinder.chat.domain.model.Message;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Component
public class MessageContentResolver {

    @Named("resolveContent")
    public String resolveContent(Message message) {
        if (message == null) return null;

        if (message.isDeleted()) {
            return "TOMBSTONE";
        }

        if (message.getContentType() != MessageContentType.TEXT) {
            String fileNameWithExt = Paths.get(message.getContent()).getFileName().toString();
            return String.format("/api/v1/chats/%s/media/%s", message.getChatId(), fileNameWithExt);
        }

        return message.getContent();
    }
}