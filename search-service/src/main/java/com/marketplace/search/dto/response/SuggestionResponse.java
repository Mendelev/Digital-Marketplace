package com.marketplace.search.dto.response;

import java.util.List;

/**
 * Autocomplete suggestions response.
 */
public record SuggestionResponse(
        List<String> suggestions,
        int count
) {}
