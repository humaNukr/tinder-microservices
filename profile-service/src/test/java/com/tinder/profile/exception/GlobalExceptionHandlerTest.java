package com.tinder.profile.exception;

import com.tinder.profile.config.GatewayAuthFilter;
import com.tinder.profile.contoller.ProfileController;
import com.tinder.profile.properties.GatewayAuthProperties;
import com.tinder.profile.service.interfaces.ProfileService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProfileController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GatewayAuthFilter.class
        )
)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(GatewayAuthProperties.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @Nested
    @DisplayName("HTTP error mapping")
    class HttpErrorMapping {

        @Test
        @DisplayName("returns 404 when profile is not found")
        void profileNotFound_Returns404() throws Exception {
            UUID userId = UUID.randomUUID();
            when(profileService.getMyProfile(userId))
                    .thenThrow(new ProfileNotFoundException("not found"));

            mockMvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("returns 400 when X-User-Id is invalid")
        void invalidUserId_Returns400() throws Exception {
            mockMvc.perform(get("/api/v1/profiles/me").header("X-User-Id", "not-uuid"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when request body fails validation")
        void invalidBody_Returns400() throws Exception {
            String body = """
                    {
                      "name": "",
                      "birthDate": "2030-01-01",
                      "gender": "MALE",
                      "targetGender": "FEMALE"
                    }
                    """;

            mockMvc.perform(post("/api/v1/profiles/onboarding")
                            .header("X-User-Id", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 409 on duplicate profile")
        void conflict_Returns409() throws Exception {
            UUID userId = UUID.randomUUID();
            when(profileService.createProfile(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any()))
                    .thenThrow(new IllegalStateException("duplicate"));

            mockMvc.perform(post("/api/v1/profiles/onboarding")
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Alex",
                                      "birthDate": "%s",
                                      "gender": "MALE",
                                      "targetGender": "FEMALE",
                                      "bio": "bio",
                                      "interests": []
                                    }
                                    """.formatted(LocalDate.now().minusYears(25))))
                    .andExpect(status().isConflict());
        }
    }
}
