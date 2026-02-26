package com.paiad.mcp.registry;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Platform descriptor for canonical metadata.
 */
public record PlatformDescriptor(
        String id,
        String name,
        String url,
        Set<String> aliases,
        boolean enabled,
        int priority,
        String description) {

    public PlatformDescriptor {
        aliases = aliases == null ? Collections.emptySet() : normalizeAliases(aliases);
    }

    private static Set<String> normalizeAliases(Set<String> source) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String alias : source) {
            if (alias != null && !alias.isBlank()) {
                normalized.add(alias.trim().toLowerCase());
            }
        }
        return Collections.unmodifiableSet(normalized);
    }
}
