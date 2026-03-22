package com.founderlink.gateway.service;

import com.founderlink.gateway.config.RbacProperties;
import com.founderlink.gateway.security.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RbacServiceTest {

    @Test
    void allowsFounderToInviteTeam() {
        RbacService service = new RbacService(properties(
                RbacProperties.DefaultPolicy.DENY,
                rule(HttpMethod.POST, "/teams/invite", Role.FOUNDER)
        ));

        assertThatCode(() -> service.verifyAccess(HttpMethod.POST, "/teams/invite", Role.FOUNDER))
                .doesNotThrowAnyException();
    }

    @Test
    void deniesInvestorFromInvitingTeam() {
        RbacService service = new RbacService(properties(
                RbacProperties.DefaultPolicy.DENY,
                rule(HttpMethod.POST, "/teams/invite", Role.FOUNDER)
        ));

        assertForbidden(() -> service.verifyAccess(HttpMethod.POST, "/teams/invite", Role.INVESTOR),
                "Role not allowed for request");
    }

    @Test
    void allowsFounderToReadStartupInvestments() {
        RbacService service = new RbacService(properties(
                RbacProperties.DefaultPolicy.DENY,
                rule(HttpMethod.GET, "/investments/startup/*", Role.FOUNDER)
        ));

        assertThatCode(() -> service.verifyAccess(HttpMethod.GET, "/investments/startup/42", Role.FOUNDER))
                .doesNotThrowAnyException();
    }

    @Test
    void deniesInvestorFromReadingStartupInvestments() {
        RbacService service = new RbacService(properties(
                RbacProperties.DefaultPolicy.DENY,
                rule(HttpMethod.GET, "/investments/startup/*", Role.FOUNDER)
        ));

        assertForbidden(() -> service.verifyAccess(HttpMethod.GET, "/investments/startup/42", Role.INVESTOR),
                "Role not allowed for request");
    }

    @Test
    void prefersMoreSpecificStartupRuleOverGenericInvestmentsRule() {
        RbacService service = new RbacService(properties(
                RbacProperties.DefaultPolicy.DENY,
                rule(HttpMethod.GET, "/investments/*", Role.FOUNDER, Role.INVESTOR),
                rule(HttpMethod.GET, "/investments/startup/*", Role.FOUNDER)
        ));

        assertThatCode(() -> service.verifyAccess(HttpMethod.GET, "/investments/startup/42", Role.FOUNDER))
                .doesNotThrowAnyException();

        assertForbidden(() -> service.verifyAccess(HttpMethod.GET, "/investments/startup/42", Role.INVESTOR),
                "Role not allowed for request");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/teams/invitations/123/cancel",
            "/teams/invitations/123/cancel/",
            "/teams//invitations//123//cancel",
            "/TEAMS/INVITATIONS/123/CANCEL"
    })
    void matchesWildcardAndNormalizesRequestPath(String requestPath) {
        RbacService service = new RbacService(properties(
                RbacProperties.DefaultPolicy.DENY,
                rule(HttpMethod.PUT, "/teams/invitations/*/cancel", Role.FOUNDER, Role.ADMIN)
        ));

        assertThatCode(() -> service.verifyAccess(HttpMethod.PUT, requestPath, Role.FOUNDER))
                .doesNotThrowAnyException();
    }

    @Test
    void treatsMatchingAsCaseInsensitiveAfterNormalization() {
        RbacService service = new RbacService(properties(
                RbacProperties.DefaultPolicy.DENY,
                rule(HttpMethod.POST, "/Teams/Invite", Role.FOUNDER)
        ));

        assertThatCode(() -> service.verifyAccess(HttpMethod.POST, "/teams/invite", Role.FOUNDER))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.verifyAccess(HttpMethod.POST, "/TEAMS/INVITE", Role.FOUNDER))
                .doesNotThrowAnyException();
    }

    @Test
    void deniesWhenNoMatchingRuleAndDefaultPolicyIsDeny() {
        RbacService service = new RbacService(properties(
                RbacProperties.DefaultPolicy.DENY,
                rule(HttpMethod.POST, "/teams/invite", Role.FOUNDER)
        ));

        assertForbidden(() -> service.verifyAccess(HttpMethod.GET, "/teams/invite", Role.FOUNDER),
                "No RBAC rule configured for request");
    }

    @Test
    void allowsAuthenticatedUserWhenNoRuleMatchesAndDefaultPolicyAllows() {
        RbacService service = new RbacService(properties(
                RbacProperties.DefaultPolicy.ALLOW_AUTHENTICATED,
                rule(HttpMethod.POST, "/teams/invite", Role.FOUNDER)
        ));

        assertThatCode(() -> service.verifyAccess(HttpMethod.GET, "/reports/export", Role.ADMIN))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnsupportedHttpMethod() {
        RbacService service = new RbacService(properties(
                RbacProperties.DefaultPolicy.DENY,
                rule(HttpMethod.POST, "/teams/invite", Role.FOUNDER)
        ));

        assertForbidden(() -> service.verifyAccess(null, "/teams/invite", Role.FOUNDER),
                "Unsupported HTTP method");
    }

    @Test
    void treatsBlankPathAsRootDuringEvaluation() {
        RbacService service = new RbacService(properties(
                RbacProperties.DefaultPolicy.DENY,
                rule(HttpMethod.GET, "/health", Role.ADMIN)
        ));

        assertForbidden(() -> service.verifyAccess(HttpMethod.GET, "   ", Role.ADMIN),
                "No RBAC rule configured for request");
    }

    @Test
    void treatsNullPathAsRootDuringEvaluation() {
        RbacService service = new RbacService(properties(
                RbacProperties.DefaultPolicy.DENY,
                rule(HttpMethod.GET, "/", Role.ADMIN)
        ));

        assertThatCode(() -> service.verifyAccess(HttpMethod.GET, null, Role.ADMIN))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAmbiguousOverlappingRulesWithEqualSpecificity() {
        assertThatThrownBy(() -> new RbacService(properties(
                RbacProperties.DefaultPolicy.DENY,
                rule(HttpMethod.GET, "/teams/*", Role.FOUNDER),
                rule(HttpMethod.GET, "/teams/?", Role.ADMIN)
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ambiguous RBAC rules");
    }

    @ParameterizedTest(name = "{index}: allows {2} for {0} {1}")
    @MethodSource("allowedConfiguredRules")
    void allowsConfiguredRolesForEveryApplicationRule(HttpMethod method, String path, Role role) {
        RbacService service = new RbacService(applicationRulesProperties());

        assertThatCode(() -> service.verifyAccess(method, path, role))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{index}: denies {2} for {0} {1}")
    @MethodSource("deniedConfiguredRules")
    void deniesUnconfiguredRolesForApplicationRules(HttpMethod method, String path, Role role) {
        RbacService service = new RbacService(applicationRulesProperties());

        assertForbidden(() -> service.verifyAccess(method, path, role), "Role not allowed for request");
    }

    private static Stream<Arguments> allowedConfiguredRules() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/users", Role.FOUNDER),
                Arguments.of(HttpMethod.GET, "/users/12", Role.ADMIN),
                Arguments.of(HttpMethod.PUT, "/users/12", Role.COFOUNDER),
                Arguments.of(HttpMethod.POST, "/startup", Role.FOUNDER),
                Arguments.of(HttpMethod.GET, "/startup/42", Role.INVESTOR),
                Arguments.of(HttpMethod.GET, "/startup/details/42", Role.ADMIN),
                Arguments.of(HttpMethod.GET, "/startup/founder", Role.FOUNDER),
                Arguments.of(HttpMethod.PUT, "/startup/42", Role.FOUNDER),
                Arguments.of(HttpMethod.DELETE, "/startup/42", Role.FOUNDER),
                Arguments.of(HttpMethod.GET, "/startup/search", Role.COFOUNDER),
                Arguments.of(HttpMethod.POST, "/teams/invite", Role.FOUNDER),
                Arguments.of(HttpMethod.PUT, "/teams/invitations/123/cancel", Role.FOUNDER),
                Arguments.of(HttpMethod.PUT, "/teams/invitations/123/reject", Role.COFOUNDER),
                Arguments.of(HttpMethod.GET, "/teams/invitations/user", Role.COFOUNDER),
                Arguments.of(HttpMethod.GET, "/teams/invitations/startup/77", Role.FOUNDER),
                Arguments.of(HttpMethod.POST, "/teams/join", Role.COFOUNDER),
                Arguments.of(HttpMethod.GET, "/teams/startup/77", Role.FOUNDER),
                Arguments.of(HttpMethod.GET, "/teams/startup/77", Role.INVESTOR),
                Arguments.of(HttpMethod.GET, "/teams/startup/77", Role.COFOUNDER),
                Arguments.of(HttpMethod.GET, "/teams/startup/77", Role.ADMIN),
                Arguments.of(HttpMethod.DELETE, "/teams/45", Role.FOUNDER),
                Arguments.of(HttpMethod.GET, "/teams/member/history", Role.COFOUNDER),
                Arguments.of(HttpMethod.GET, "/teams/member/active", Role.ADMIN),
                Arguments.of(HttpMethod.POST, "/investments", Role.INVESTOR),
                Arguments.of(HttpMethod.GET, "/investments/startup/88", Role.FOUNDER),
                Arguments.of(HttpMethod.GET, "/investments/startup/88", Role.ADMIN),
                Arguments.of(HttpMethod.GET, "/investments/investor", Role.INVESTOR),
                Arguments.of(HttpMethod.GET, "/investments/investor", Role.ADMIN),
                Arguments.of(HttpMethod.PUT, "/investments/88/status", Role.FOUNDER),
                Arguments.of(HttpMethod.PUT, "/investments/88/status", Role.ADMIN),
                Arguments.of(HttpMethod.GET, "/investments/88", Role.FOUNDER),
                Arguments.of(HttpMethod.GET, "/investments/88", Role.INVESTOR),
                Arguments.of(HttpMethod.GET, "/investments/88", Role.ADMIN),
                Arguments.of(HttpMethod.POST, "/messages", Role.FOUNDER),
                Arguments.of(HttpMethod.GET, "/messages/88", Role.ADMIN),
                Arguments.of(HttpMethod.GET, "/messages/conversation/1/2", Role.COFOUNDER),
                Arguments.of(HttpMethod.GET, "/messages/partners/1", Role.INVESTOR),
                Arguments.of(HttpMethod.GET, "/notifications/4", Role.ADMIN),
                Arguments.of(HttpMethod.GET, "/notifications/4/unread", Role.FOUNDER),
                Arguments.of(HttpMethod.PUT, "/notifications/4/read", Role.COFOUNDER)
        );
    }

    private static Stream<Arguments> deniedConfiguredRules() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/startup", Role.ADMIN),
                Arguments.of(HttpMethod.GET, "/startup/founder", Role.INVESTOR),
                Arguments.of(HttpMethod.PUT, "/startup/42", Role.COFOUNDER),
                Arguments.of(HttpMethod.DELETE, "/startup/42", Role.ADMIN),
                Arguments.of(HttpMethod.POST, "/teams/invite", Role.INVESTOR),
                Arguments.of(HttpMethod.PUT, "/teams/invitations/123/cancel", Role.COFOUNDER),
                Arguments.of(HttpMethod.PUT, "/teams/invitations/123/reject", Role.FOUNDER),
                Arguments.of(HttpMethod.GET, "/teams/invitations/user", Role.ADMIN),
                Arguments.of(HttpMethod.POST, "/teams/join", Role.ADMIN),
                Arguments.of(HttpMethod.DELETE, "/teams/45", Role.INVESTOR),
                Arguments.of(HttpMethod.GET, "/teams/member/history", Role.FOUNDER),
                Arguments.of(HttpMethod.POST, "/investments", Role.FOUNDER),
                Arguments.of(HttpMethod.GET, "/investments/startup/88", Role.INVESTOR),
                Arguments.of(HttpMethod.GET, "/investments/investor", Role.FOUNDER),
                Arguments.of(HttpMethod.GET, "/investments/88", Role.COFOUNDER),
                Arguments.of(HttpMethod.GET, "/teams/invitations/startup/77", Role.ADMIN)
        );
    }

    private static RbacProperties applicationRulesProperties() {
        return properties(
                RbacProperties.DefaultPolicy.DENY,
                rule(HttpMethod.GET, "/users", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.GET, "/users/*", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.PUT, "/users/*", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.POST, "/startup", Role.FOUNDER),
                rule(HttpMethod.GET, "/startup/details/*", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.GET, "/startup/founder", Role.FOUNDER),
                rule(HttpMethod.GET, "/startup/search", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.GET, "/startup/*", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.PUT, "/startup/*", Role.FOUNDER),
                rule(HttpMethod.DELETE, "/startup/*", Role.FOUNDER),
                rule(HttpMethod.POST, "/teams/invite", Role.FOUNDER),
                rule(HttpMethod.PUT, "/teams/invitations/*/cancel", Role.FOUNDER),
                rule(HttpMethod.PUT, "/teams/invitations/*/reject", Role.COFOUNDER),
                rule(HttpMethod.GET, "/teams/invitations/user", Role.COFOUNDER),
                rule(HttpMethod.GET, "/teams/invitations/startup/*", Role.FOUNDER),
                rule(HttpMethod.POST, "/teams/join", Role.COFOUNDER),
                rule(HttpMethod.GET, "/teams/startup/*", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.DELETE, "/teams/*", Role.FOUNDER),
                rule(HttpMethod.GET, "/teams/member/history", Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.GET, "/teams/member/active", Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.POST, "/investments", Role.INVESTOR),
                rule(HttpMethod.GET, "/investments/startup/*", Role.FOUNDER, Role.ADMIN),
                rule(HttpMethod.GET, "/investments/investor", Role.INVESTOR, Role.ADMIN),
                rule(HttpMethod.PUT, "/investments/*/status", Role.FOUNDER, Role.ADMIN),
                rule(HttpMethod.GET, "/investments/*", Role.FOUNDER, Role.INVESTOR, Role.ADMIN),
                rule(HttpMethod.POST, "/messages", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.GET, "/messages/conversation/*/*", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.GET, "/messages/partners/*", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.GET, "/messages/*", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.GET, "/notifications/*/unread", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.GET, "/notifications/*", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN),
                rule(HttpMethod.PUT, "/notifications/*/read", Role.FOUNDER, Role.INVESTOR, Role.COFOUNDER, Role.ADMIN)
        );
    }

    private static RbacProperties properties(RbacProperties.DefaultPolicy defaultPolicy, RbacProperties.Rule... rules) {
        RbacProperties properties = new RbacProperties();
        properties.setDefaultPolicy(defaultPolicy);
        properties.setRules(List.of(rules));
        return properties;
    }

    private static RbacProperties.Rule rule(HttpMethod method, String path, Role... roles) {
        RbacProperties.Rule rule = new RbacProperties.Rule();
        rule.setMethod(method);
        rule.setPath(path);
        rule.setRoles(List.of(roles));
        return rule;
    }

    private static void assertForbidden(ThrowingRunnable invocation, String reason) {
        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                invocation::run
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getReason()).isEqualTo(reason);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
