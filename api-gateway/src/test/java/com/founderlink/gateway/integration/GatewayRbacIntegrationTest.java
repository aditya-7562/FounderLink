package com.founderlink.gateway.integration;

import com.founderlink.gateway.ApiGatewayApplication;
import com.founderlink.gateway.security.AuthenticatedUser;
import com.founderlink.gateway.security.Role;
import com.founderlink.gateway.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = ApiGatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(GatewayRbacIntegrationTest.DownstreamEchoController.class)
class GatewayRbacIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void setUpJwtService() {
        when(jwtService.authenticate(anyString())).thenAnswer(invocation -> switch (invocation.getArgument(0, String.class)) {
            case "founder-token" -> new AuthenticatedUser("founder-1", Role.FOUNDER);
            case "investor-token" -> new AuthenticatedUser("investor-1", Role.INVESTOR);
            case "cofounder-token" -> new AuthenticatedUser("cofounder-1", Role.COFOUNDER);
            case "admin-token" -> new AuthenticatedUser("admin-1", Role.ADMIN);
            case "invalid-token", "expired-token" ->
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            case "missing-role-token" ->
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token role is missing");
            case "invalid-role-token" ->
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Simulated role is invalid");
            default -> throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown test token");
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rbacCases")
    void enforcesRbacMatrix(TestCase testCase) {
        exchange(testCase.method(), testCase.path(), tokenFor(testCase.role()))
                .expectStatus().isEqualTo(testCase.expectedStatus());
    }

    @Test
    void overwritesSpoofedAuthenticationHeadersBeforeForwarding() {
        exchange(HttpMethod.POST, "/teams/invite", "founder-token",
                Map.of(
                        "X-User-Id", "spoofed-user",
                        "X-User-Role", "ROLE_ADMIN",
                        "X-Auth-Source", "browser",
                        "X-User-Name", "spoofed-name"
                ))
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.xUserId").isEqualTo("founder-1")
                .jsonPath("$.xUserRole").isEqualTo("ROLE_FOUNDER")
                .jsonPath("$.xAuthSource").isEqualTo("gateway")
                .jsonPath("$.xUserNamePresent").isEqualTo(false);
    }

    @Test
    void returnsUnauthorizedWhenAuthorizationHeaderIsMissing() {
        exchangeWithoutAuthorization(HttpMethod.GET, "/users/42")
                .expectStatus().isUnauthorized();
    }

    @Test
    void returnsUnauthorizedWhenBearerTokenIsBlank() {
        webTestClient.get()
                .uri("/users/42")
                .header(HttpHeaders.AUTHORIZATION, "Bearer   ")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void returnsUnauthorizedWhenJwtValidationFails() {
        exchange(HttpMethod.GET, "/users/42", "invalid-token")
                .expectStatus().isUnauthorized();
    }

    @Test
    void returnsUnauthorizedWhenTokenRoleIsMissing() {
        exchange(HttpMethod.GET, "/investments/42", "missing-role-token")
                .expectStatus().isUnauthorized();
    }

    @Test
    void returnsForbiddenWhenAuthSimulationProvidesUnknownRole() {
        exchange(HttpMethod.GET, "/users/42", "invalid-role-token")
                .expectStatus().isForbidden();
    }

    @Test
    void returnsForbiddenWhenRouteExistsButNoRbacRuleMatches() {
        exchange(HttpMethod.GET, "/reports/export", "founder-token")
                .expectStatus().isForbidden();
    }

    private WebTestClient.ResponseSpec exchange(HttpMethod method, String path, String token) {
        return exchange(method, path, token, Map.of());
    }

    private WebTestClient.ResponseSpec exchange(
            HttpMethod method,
            String path,
            String token,
            Map<String, String> extraHeaders
    ) {
        WebTestClient.RequestHeadersSpec<?> request = requestSpec(method, path, extraHeaders)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request.exchange();
    }

    private WebTestClient.ResponseSpec exchangeWithoutAuthorization(HttpMethod method, String path) {
        return requestSpec(method, path, Map.of()).exchange();
    }

    private WebTestClient.RequestHeadersSpec<?> requestSpec(
            HttpMethod method,
            String path,
            Map<String, String> extraHeaders
    ) {
        WebTestClient.RequestHeadersSpec<?> request;
        if (HttpMethod.GET.equals(method)) {
            request = webTestClient.get().uri(path);
        } else if (HttpMethod.DELETE.equals(method)) {
            request = webTestClient.delete().uri(path);
        } else if (HttpMethod.POST.equals(method)) {
            request = webTestClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("probe", true));
        } else if (HttpMethod.PUT.equals(method)) {
            request = webTestClient.put()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("probe", true));
        } else {
            throw new IllegalArgumentException("Unsupported method: " + method);
        }

        extraHeaders.forEach(request::header);
        return request;
    }

    private static Stream<TestCase> rbacCases() {
        return endpointPolicies().stream()
                .flatMap(policy -> Stream.of(Role.values())
                        .map(role -> new TestCase(
                                policy.method(),
                                policy.path(),
                                role,
                                policy.allowedRoles().contains(role) ? HttpStatus.OK : HttpStatus.FORBIDDEN
                        )));
    }

    private static List<EndpointPolicy> endpointPolicies() {
        return List.of(
                policy(HttpMethod.GET, "/users", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.GET, "/users/42", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.PUT, "/users/42", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.POST, "/startup", Role.FOUNDER),
                policy(HttpMethod.GET, "/startup/42", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.GET, "/startup/details/42", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.GET, "/startup/founder", Role.FOUNDER),
                policy(HttpMethod.PUT, "/startup/42", Role.FOUNDER),
                policy(HttpMethod.DELETE, "/startup/42", Role.FOUNDER),
                policy(HttpMethod.GET, "/startup/search?industry=FinTech&stage=MVP", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.POST, "/investments", Role.INVESTOR),
                policy(HttpMethod.GET, "/investments/startup/42", Role.FOUNDER, Role.ADMIN),
                policy(HttpMethod.GET, "/investments/investor", Role.INVESTOR, Role.ADMIN),
                policy(HttpMethod.PUT, "/investments/42/status", Role.FOUNDER, Role.ADMIN),
                policy(HttpMethod.GET, "/investments/42", Role.FOUNDER, Role.INVESTOR, Role.ADMIN),
                policy(HttpMethod.POST, "/teams/invite", Role.FOUNDER),
                policy(HttpMethod.PUT, "/teams/invitations/42/cancel", Role.FOUNDER),
                policy(HttpMethod.PUT, "/teams/invitations/42/reject", Role.COFOUNDER),
                policy(HttpMethod.GET, "/teams/invitations/user", Role.COFOUNDER),
                policy(HttpMethod.GET, "/teams/invitations/startup/42", Role.FOUNDER),
                policy(HttpMethod.POST, "/teams/join", Role.COFOUNDER),
                policy(HttpMethod.GET, "/teams/startup/42", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.DELETE, "/teams/42", Role.FOUNDER),
                policy(HttpMethod.GET, "/teams/member/history", Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.GET, "/teams/member/active", Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.POST, "/messages", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.GET, "/messages/42", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.GET, "/messages/conversation/10/20", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.GET, "/messages/partners/10", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.GET, "/notifications/10", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.GET, "/notifications/10/unread", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                policy(HttpMethod.PUT, "/notifications/42/read", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN)
        );
    }

    private static EndpointPolicy policy(HttpMethod method, String path, Role... allowedRoles) {
        return new EndpointPolicy(method, path, EnumSet.copyOf(Set.of(allowedRoles)));
    }

    private static String tokenFor(Role role) {
        return switch (role) {
            case FOUNDER -> "founder-token";
            case INVESTOR -> "investor-token";
            case COFOUNDER -> "cofounder-token";
            case ADMIN -> "admin-token";
        };
    }

    private record EndpointPolicy(HttpMethod method, String path, EnumSet<Role> allowedRoles) {
    }

    private record TestCase(HttpMethod method, String path, Role role, HttpStatus expectedStatus) {
        @Override
        public String toString() {
            return method + " " + path + " as " + role + " -> " + expectedStatus.value();
        }
    }

    @RestController
    static class DownstreamEchoController {

        @RequestMapping(path = "/__downstream", method = {
                RequestMethod.GET,
                RequestMethod.POST,
                RequestMethod.PUT,
                RequestMethod.DELETE
        })
        Map<String, Object> echo(
                @RequestHeader(value = "X-User-Id", required = false) String userId,
                @RequestHeader(value = "X-User-Role", required = false) String userRole,
                @RequestHeader(value = "X-Auth-Source", required = false) String authSource,
                @RequestHeader HttpHeaders headers,
                org.springframework.http.server.reactive.ServerHttpRequest request
        ) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("method", request.getMethod().name());
            payload.put("xUserId", userId);
            payload.put("xUserRole", userRole);
            payload.put("xAuthSource", authSource);
            payload.put("xUserNamePresent", headers.containsKey("X-User-Name"));
            return payload;
        }
    }
}
