package com.rollingstone.retailreplenishment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Resource-server security for the retail replenishment API. Three roles,
 * each mapped from a Keycloak realm role via KeycloakRealmRoleConverter:
 *
 *   REPLENISHMENT_AGENT    - the LangGraph agent's confidential-client
 *                             service account (client_credentials grant).
 *                             Everything the agent does autonomously:
 *                             reads + order/shipment/escalation creation +
 *                             the agent-run/decision/llm-call audit trail.
 *   SUPPLY_CHAIN_MANAGER   - a human. Approves/rejects orders the agent
 *                             didn't auto-approve, resolves escalations.
 *   DASHBOARD_VIEWER       - read-only, for the cost/audit dashboard.
 *
 * See keycloak/retail-replenishment-realm.json for the realm/client/role
 * definitions this expects.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String AGENT = "REPLENISHMENT_AGENT";
    private static final String MANAGER = "SUPPLY_CHAIN_MANAGER";
    private static final String VIEWER = "DASHBOARD_VIEWER";

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Master/read data and every read endpoint: any authenticated role.
                        .requestMatchers("GET", "/api/stores/**", "/api/products/**", "/api/suppliers/**",
                                "/api/inventory/**", "/api/demand-forecasts", "/api/replenishment-orders/**",
                                "/api/escalations", "/api/agent-runs/**","/api/audit/**")
                        .hasAnyRole(AGENT, MANAGER, VIEWER)

                        // Agent-only writes: the pipeline stages the agent executes autonomously.
                        .requestMatchers("POST", "/api/demand-forecasts", "/api/replenishment-orders",
                                "/api/replenishment-orders/*/shipments", "/api/escalations",
                                "/api/agent-runs", "/api/agent-runs/*/decisions", "/api/agent-runs/*/llm-calls",
                                "/api/agent-runs/*/llm-calls/*/http-trace")
                        .hasRole(AGENT)
                        .requestMatchers("PATCH", "/api/agent-runs/*", "/api/shipments/*/status")
                        .hasRole(AGENT)

                        // Human-in-the-loop actions: order approval and escalation resolution
                        // can also be exercised by the agent for its own auto-approval path.
                        .requestMatchers("PATCH", "/api/replenishment-orders/*/status")
                        .hasAnyRole(AGENT, MANAGER)
                        .requestMatchers("PATCH", "/api/escalations/*/status")
                        .hasRole(MANAGER)

                        // Demo/rehearsal reset — deliberately manager-only, never the
                        // agent's service account. Also gated at the service layer by
                        // app.demo-reset.enabled; being authorized here is necessary
                        // but not sufficient.
                        .requestMatchers("POST", "/api/admin/demo-reset")
                        .hasRole(MANAGER)

                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }
}