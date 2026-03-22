package com.founderlink.gateway.config;

import com.founderlink.gateway.security.Role;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "rbac")
public class RbacProperties {

    private DefaultPolicy defaultPolicy = DefaultPolicy.DENY;
    private List<Rule> rules = new ArrayList<>();

    public DefaultPolicy getDefaultPolicy() {
        return defaultPolicy;
    }

    public void setDefaultPolicy(DefaultPolicy defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public static class Rule {

        private HttpMethod method;
        private String path;
        private List<Role> roles = new ArrayList<>();

        public HttpMethod getMethod() {
            return method;
        }

        public void setMethod(HttpMethod method) {
            this.method = method;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public List<Role> getRoles() {
            return roles;
        }

        public void setRoles(List<Role> roles) {
            this.roles = roles;
        }
    }

    public enum DefaultPolicy {
        DENY,
        ALLOW_AUTHENTICATED
    }
}
