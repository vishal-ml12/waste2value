package com.waste2value.shared;

public record ValidationError(
        String field,
        String message
) {
}
