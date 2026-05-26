package com.tinder.chat.infrastructure.adapter.in.web;

import com.tinder.chat.application.port.in.room.GetChatHistoryQuery;
import com.tinder.chat.application.port.in.room.GetChatListQuery;
import com.tinder.chat.application.port.in.room.InitChatQuery;
import com.tinder.chat.config.GatewayAuthFilter;
import com.tinder.chat.shared.dto.room.ChatHistoryResponseDto;
import com.tinder.chat.shared.dto.room.ChatInitResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ChatController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GatewayAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    private final String userIdHeader = "X-User-Id";
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private InitChatQuery initChatQuery;
    @MockitoBean
    private GetChatListQuery getChatListQuery;
    @MockitoBean
    private GetChatHistoryQuery getChatHistoryQuery;
    private UUID chatId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Nested
    class InitChat {

        @Test
        void initChat_ValidRequest_ReturnsOk() throws Exception {
            when(initChatQuery.initChat(chatId, userId, 20))
                    .thenReturn(new ChatInitResponseDto(Collections.emptyList(), false, 0L, 0L, null, false));

            mockMvc.perform(get("/api/v1/chats/{chatId}/init", chatId)
                            .header(userIdHeader, userId.toString()))
                    .andExpect(status().isOk());

            verify(initChatQuery).initChat(chatId, userId, 20);
        }

        @Test
        void initChat_LimitExceedsMax_CapsLimitTo50() throws Exception {
            mockMvc.perform(get("/api/v1/chats/{chatId}/init", chatId)
                            .param("limit", "100")
                            .header(userIdHeader, userId.toString()))
                    .andExpect(status().isOk());

            verify(initChatQuery).initChat(chatId, userId, 50);
        }
    }

    @Nested
    class GetChatsList {

        @Test
        void getChatsList_ValidRequest_ReturnsOk() throws Exception {
            when(getChatListQuery.getChatsList(userId, 0, 20))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/chats")
                            .header(userIdHeader, userId.toString()))
                    .andExpect(status().isOk());

            verify(getChatListQuery).getChatsList(userId, 0, 20);
        }

        @Test
        void getChatsList_NegativePage_CapsPageToZero() throws Exception {
            mockMvc.perform(get("/api/v1/chats")
                            .param("page", "-5")
                            .header(userIdHeader, userId.toString()))
                    .andExpect(status().isOk());

            verify(getChatListQuery).getChatsList(userId, 0, 20);
        }
    }

    @Nested
    class GetChatHistory {

        @Test
        void getChatHistory_ValidRequest_ReturnsOk() throws Exception {
            when(getChatHistoryQuery.getChatHistory(chatId, userId, 10L, 20))
                    .thenReturn(new ChatHistoryResponseDto(Collections.emptyList(), null, false));

            mockMvc.perform(get("/api/v1/chats/{chatId}/messages", chatId)
                            .param("cursor", "10")
                            .header(userIdHeader, userId.toString()))
                    .andExpect(status().isOk());

            verify(getChatHistoryQuery).getChatHistory(chatId, userId, 10L, 20);
        }
    }
}