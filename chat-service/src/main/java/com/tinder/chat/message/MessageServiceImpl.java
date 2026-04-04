package com.tinder.chat.message;

import com.tinder.chat.message.dto.ChatRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;

    @Override
    public Message saveMessage(UUID senderId, ChatRequestDto requestDto) {
        Message message = new Message();
        message.setChatId(requestDto.chatId());
        message.setSenderId(senderId);
        message.setContentType(requestDto.type());
        message.setContent(requestDto.payload());

        return messageRepository.save(message);
    }
}