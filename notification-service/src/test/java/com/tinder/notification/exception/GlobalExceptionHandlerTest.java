package com.tinder.notification.exception;

import com.tinder.notification.config.GatewayAuthFilter;
import com.tinder.notification.controller.NotificationController;
import com.tinder.notification.properties.GatewayAuthProperties;
import com.tinder.notification.service.interfaces.NotificationLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = NotificationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GatewayAuthFilter.class
        )
)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(GatewayAuthProperties.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "app.security.gateway-auth.enabled=false",
        "app.security.gateway-auth.header-name=X-Gateway-Authenticated",
        "app.security.gateway-auth.expected-value=true",
        "app.security.gateway-auth.user-id-header=X-User-Id"
})
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationLogService notificationLogService;

    private static final UUID USER_ID = UUID.randomUUID();

    @Nested
    @DisplayName("HTTP error mapping")
    class HttpErrorMapping {

        @Test
        @DisplayName("returns 400 when X-User-Id is not a valid UUID")
        void invalidUserId_Returns400() throws Exception {
            mockMvc.perform(get("/api/v1/notifications")
                            .header("X-User-Id", "not-a-uuid")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("returns 404 when notification is not found")
        void notificationNotFound_Returns404() throws Exception {
            UUID notificationId = UUID.randomUUID();
            doThrow(new EntityNotFoundException("not found"))
                    .when(notificationLogService).markAsRead(eq(notificationId), eq(USER_ID));

            mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId)
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("returns 403 when user does not own the notification")
        void accessDenied_Returns403() throws Exception {
            UUID notificationId = UUID.randomUUID();
            doThrow(new AccessDeniedException("forbidden"))
                    .when(notificationLogService).markAsRead(eq(notificationId), eq(USER_ID));

            mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId)
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("returns paginated payload for GET /notifications")
        void getNotifications_ReturnsPage() throws Exception {
            when(notificationLogService.getUserNotifications(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/v1/notifications")
                            .header("X-User-Id", USER_ID)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }
}
