package com.founderlink.gateway.service;

import com.founderlink.gateway.config.RbacProperties;
import com.founderlink.gateway.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class RbacService {

    private static final Logger log = LoggerFactory.getLogger(RbacService.class);
    private static final Comparator<RbacRule> RULE_SPECIFICITY = Comparator
            .comparingInt(RbacRule::depth).reversed()
            .thenComparingInt(RbacRule::wildcardCount)
            .thenComparingInt(RbacRule::doubleWildcardCount)
            .thenComparingInt(RbacRule::literalSegmentCount).reversed()
            .thenComparingInt(RbacRule::literalCharacterCount).reversed();
    private static final Comparator<RbacRule> RULE_ORDER = RULE_SPECIFICITY
            .thenComparing(RbacRule::path);

    private final Map<HttpMethod, List<RbacRule>> rulesByMethod;
    private final RbacProperties.DefaultPolicy defaultPolicy;
    private final AntPathMatcher pathMatcher;

    public RbacService(RbacProperties properties) {
        this.defaultPolicy = Objects.requireNonNullElse(properties.getDefaultPolicy(), RbacProperties.DefaultPolicy.DENY);
        this.pathMatcher = new AntPathMatcher();
        this.pathMatcher.setCaseSensitive(true);

        List<RbacRule> preparedRules = prepareRules(properties.getRules());
        validateRuleConflicts(preparedRules);
        this.rulesByMethod = indexRulesByMethod(preparedRules);
    }

    public void verifyAccess(HttpMethod method, String path, Role role) {
        String normalizedPath = normalizePath(path);
        if (method == null) {
            log.debug("RBAC check: method=null, path={}, role={}, matchedRule=NONE, decision=DENY, reason=unsupported_method",
                    normalizedPath, role);
            log.warn("RBAC denied: method=null, path={}, role={}, matchedRule=NONE, reason=unsupported_method",
                    normalizedPath, role);
            throw forbidden("Unsupported HTTP method");
        }

        RbacRule matchingRule = rulesByMethod.getOrDefault(method, List.of()).stream()
                .filter(rule -> pathMatcher.match(rule.path(), normalizedPath))
                .findFirst()
                .orElse(null);

        if (matchingRule == null) {
            if (defaultPolicy == RbacProperties.DefaultPolicy.ALLOW_AUTHENTICATED) {
                log.debug("RBAC check: method={}, path={}, role={}, matchedRule=NONE, decision=ALLOW, policy={}",
                        method, normalizedPath, role, defaultPolicy);
                return;
            }

            log.debug("RBAC check: method={}, path={}, role={}, matchedRule=NONE, decision=DENY, reason=no_matching_rule, policy={}",
                    method, normalizedPath, role, defaultPolicy);
            log.warn("RBAC denied: method={}, path={}, role={}, matchedRule=NONE, reason=no_matching_rule, policy={}",
                    method, normalizedPath, role, defaultPolicy);
            throw forbidden("No RBAC rule configured for request");
        }

        if (!matchingRule.roles().contains(role)) {
            log.debug("RBAC check: method={}, path={}, role={}, matchedRule={}, decision=DENY, reason=role_not_allowed",
                    method, normalizedPath, role, matchingRule.describe());
            log.warn("RBAC denied: method={}, path={}, role={}, matchedRule={}, reason=role_not_allowed",
                    method, normalizedPath, role, matchingRule.describe());
            throw forbidden("Role not allowed for request");
        }

        log.debug("RBAC check: method={}, path={}, role={}, matchedRule={}, decision=ALLOW",
                method, normalizedPath, role, matchingRule.describe());
    }

    private List<RbacRule> prepareRules(List<RbacProperties.Rule> configuredRules) {
        List<RbacRule> preparedRules = new ArrayList<>();

        for (int index = 0; index < configuredRules.size(); index++) {
            RbacProperties.Rule configuredRule = configuredRules.get(index);
            if (configuredRule.getMethod() == null) {
                throw new IllegalStateException("RBAC rule at index " + index + " is missing HTTP method");
            }
            if (!StringUtils.hasText(configuredRule.getPath())) {
                throw new IllegalStateException("RBAC rule at index " + index + " is missing path");
            }
            if (configuredRule.getRoles() == null || configuredRule.getRoles().isEmpty()) {
                throw new IllegalStateException("RBAC rule at index " + index + " must define at least one role");
            }

            preparedRules.add(new RbacRule(
                    configuredRule.getMethod(),
                    normalizePath(configuredRule.getPath()),
                    Set.copyOf(configuredRule.getRoles())
            ));
        }

        return preparedRules;
    }

    private Map<HttpMethod, List<RbacRule>> indexRulesByMethod(List<RbacRule> rules) {
        Map<HttpMethod, List<RbacRule>> indexedRules = new HashMap<>();

        for (RbacRule rule : rules) {
            indexedRules.computeIfAbsent(rule.method(), ignored -> new ArrayList<>()).add(rule);
        }

        indexedRules.replaceAll((method, methodRules) -> methodRules.stream()
                .sorted(RULE_ORDER)
                .toList());

        return Map.copyOf(indexedRules);
    }

    private void validateRuleConflicts(List<RbacRule> rules) {
        Map<HttpMethod, List<RbacRule>> rulesPerMethod = new HashMap<>();
        for (RbacRule rule : rules) {
            rulesPerMethod.computeIfAbsent(rule.method(), ignored -> new ArrayList<>()).add(rule);
        }

        for (Map.Entry<HttpMethod, List<RbacRule>> entry : rulesPerMethod.entrySet()) {
            List<RbacRule> methodRules = entry.getValue();
            for (int leftIndex = 0; leftIndex < methodRules.size(); leftIndex++) {
                for (int rightIndex = leftIndex + 1; rightIndex < methodRules.size(); rightIndex++) {
                    RbacRule leftRule = methodRules.get(leftIndex);
                    RbacRule rightRule = methodRules.get(rightIndex);

                    if (!pathsOverlap(leftRule.path(), rightRule.path())) {
                        continue;
                    }

                    if (RULE_SPECIFICITY.compare(leftRule, rightRule) == 0) {
                        throw new IllegalStateException(
                                "Ambiguous RBAC rules for method %s: %s conflicts with %s"
                                        .formatted(entry.getKey(), leftRule.describe(), rightRule.describe())
                        );
                    }
                }
            }
        }
    }

    private boolean pathsOverlap(String leftPath, String rightPath) {
        return pathSegmentsOverlap(pathSegments(leftPath), 0, pathSegments(rightPath), 0, new HashMap<>());
    }

    private boolean pathSegmentsOverlap(
            List<String> leftSegments,
            int leftIndex,
            List<String> rightSegments,
            int rightIndex,
            Map<String, Boolean> memo
    ) {
        String memoKey = leftIndex + ":" + rightIndex;
        Boolean cached = memo.get(memoKey);
        if (cached != null) {
            return cached;
        }

        boolean result;
        if (leftIndex == leftSegments.size() && rightIndex == rightSegments.size()) {
            result = true;
        } else if (leftIndex == leftSegments.size()) {
            result = remainingSegmentsCanMatchEmpty(rightSegments, rightIndex);
        } else if (rightIndex == rightSegments.size()) {
            result = remainingSegmentsCanMatchEmpty(leftSegments, leftIndex);
        } else {
            String leftSegment = leftSegments.get(leftIndex);
            String rightSegment = rightSegments.get(rightIndex);

            if ("**".equals(leftSegment)) {
                result = pathSegmentsOverlap(leftSegments, leftIndex + 1, rightSegments, rightIndex, memo)
                        || pathSegmentsOverlap(leftSegments, leftIndex, rightSegments, rightIndex + 1, memo);
            } else if ("**".equals(rightSegment)) {
                result = pathSegmentsOverlap(leftSegments, leftIndex, rightSegments, rightIndex + 1, memo)
                        || pathSegmentsOverlap(leftSegments, leftIndex + 1, rightSegments, rightIndex, memo);
            } else if (!segmentPatternsOverlap(leftSegment, rightSegment, new HashMap<>())) {
                result = false;
            } else {
                result = pathSegmentsOverlap(leftSegments, leftIndex + 1, rightSegments, rightIndex + 1, memo);
            }
        }

        memo.put(memoKey, result);
        return result;
    }

    private boolean remainingSegmentsCanMatchEmpty(List<String> segments, int startIndex) {
        for (int index = startIndex; index < segments.size(); index++) {
            if (!"**".equals(segments.get(index))) {
                return false;
            }
        }
        return true;
    }

    private boolean segmentPatternsOverlap(String leftPattern, String rightPattern, Map<String, Boolean> memo) {
        return segmentPatternsOverlap(leftPattern, 0, rightPattern, 0, memo);
    }

    private boolean segmentPatternsOverlap(
            String leftPattern,
            int leftIndex,
            String rightPattern,
            int rightIndex,
            Map<String, Boolean> memo
    ) {
        String memoKey = leftIndex + ":" + rightIndex;
        Boolean cached = memo.get(memoKey);
        if (cached != null) {
            return cached;
        }

        boolean result;
        if (leftIndex == leftPattern.length() && rightIndex == rightPattern.length()) {
            result = true;
        } else if (leftIndex == leftPattern.length()) {
            result = canMatchEmpty(rightPattern, rightIndex);
        } else if (rightIndex == rightPattern.length()) {
            result = canMatchEmpty(leftPattern, leftIndex);
        } else {
            char leftToken = leftPattern.charAt(leftIndex);
            char rightToken = rightPattern.charAt(rightIndex);

            if (leftToken == '*') {
                result = segmentPatternsOverlap(leftPattern, leftIndex + 1, rightPattern, rightIndex, memo)
                        || segmentPatternsOverlap(leftPattern, leftIndex, rightPattern, rightIndex + 1, memo);
            } else if (rightToken == '*') {
                result = segmentPatternsOverlap(leftPattern, leftIndex, rightPattern, rightIndex + 1, memo)
                        || segmentPatternsOverlap(leftPattern, leftIndex + 1, rightPattern, rightIndex, memo);
            } else {
                result = charactersOverlap(leftToken, rightToken)
                        && segmentPatternsOverlap(leftPattern, leftIndex + 1, rightPattern, rightIndex + 1, memo);
            }
        }

        memo.put(memoKey, result);
        return result;
    }

    private boolean canMatchEmpty(String pattern, int startIndex) {
        for (int index = startIndex; index < pattern.length(); index++) {
            if (pattern.charAt(index) != '*') {
                return false;
            }
        }
        return true;
    }

    private boolean charactersOverlap(char leftToken, char rightToken) {
        return leftToken == '?'
                || rightToken == '?'
                || leftToken == rightToken;
    }

    private List<String> pathSegments(String path) {
        if ("/".equals(path)) {
            return List.of();
        }

        String[] split = path.substring(1).split("/");
        List<String> segments = new ArrayList<>(split.length);
        for (String segment : split) {
            if (StringUtils.hasText(segment)) {
                segments.add(segment);
            }
        }
        return segments;
    }

    private static String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }

        String normalizedPath = path.trim().replace('\\', '/');
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        normalizedPath = normalizedPath.replaceAll("/+", "/").toLowerCase(Locale.ROOT);
        if (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        return normalizedPath.isEmpty() ? "/" : normalizedPath;
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private record RbacRule(
            HttpMethod method,
            String path,
            Set<Role> roles,
            int depth,
            int wildcardCount,
            int doubleWildcardCount,
            int literalSegmentCount,
            int literalCharacterCount
    ) {

        private RbacRule(HttpMethod method, String path, Set<Role> roles) {
            this(
                    method,
                    path,
                    roles,
                    calculateDepth(path),
                    calculateWildcardCount(path),
                    calculateDoubleWildcardCount(path),
                    calculateLiteralSegmentCount(path),
                    calculateLiteralCharacterCount(path)
            );
        }

        private static int calculateDepth(String path) {
            if ("/".equals(path)) {
                return 0;
            }

            int depth = 0;
            for (String segment : path.substring(1).split("/")) {
                if (StringUtils.hasText(segment)) {
                    depth++;
                }
            }
            return depth;
        }

        private static int calculateWildcardCount(String path) {
            int wildcardCount = 0;
            for (char token : path.toCharArray()) {
                if (token == '*' || token == '?') {
                    wildcardCount++;
                }
            }
            return wildcardCount;
        }

        private static int calculateDoubleWildcardCount(String path) {
            int count = 0;
            for (int index = 0; index < path.length() - 1; index++) {
                if (path.charAt(index) == '*' && path.charAt(index + 1) == '*') {
                    count++;
                }
            }
            return count;
        }

        private static int calculateLiteralSegmentCount(String path) {
            if ("/".equals(path)) {
                return 0;
            }

            int literalSegments = 0;
            for (String segment : path.substring(1).split("/")) {
                if (StringUtils.hasText(segment) && segment.indexOf('*') < 0 && segment.indexOf('?') < 0) {
                    literalSegments++;
                }
            }
            return literalSegments;
        }

        private static int calculateLiteralCharacterCount(String path) {
            int literalCharacters = 0;
            for (char token : path.toCharArray()) {
                if (token != '/' && token != '*' && token != '?') {
                    literalCharacters++;
                }
            }
            return literalCharacters;
        }

        private String describe() {
            return method + " " + path + " -> " + roles;
        }
    }
}
