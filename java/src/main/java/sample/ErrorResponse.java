package sample;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Uniform JSON error body for this sample (handlers and global exception mapping).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String message, String exception, String path) {}
