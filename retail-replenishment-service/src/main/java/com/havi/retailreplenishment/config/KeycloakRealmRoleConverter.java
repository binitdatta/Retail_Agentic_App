package com.havi.retailreplenishment.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Keycloak puts realm roles under the nested "realm_access.roles" claim,
 * not the flat "scope" claim Spring Security's default JWT converter
 * expects. This pulls them out and maps each to a ROLE_-prefixed
 * GrantedAuthority so @PreAuthorize / hasRole(...) work the normal way.
 *
 * Matches the "realm roles via protocol mappers" convention used on the
 * other Keycloak-secured services — the realm export in
 * keycloak/retail-replenishment-realm.json defines the mapper that puts
 * these roles on the token in the first place.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream()
            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
            .toList();
    }
}
