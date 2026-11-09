package com.example.itsm.incident.dto;

import com.example.itsm.incident.IncidentComment;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long authorId,
        String authorName,
        String authorRole,
        String message,
        boolean internal,
        Instant createdAt
) {

    public static CommentResponse from(IncidentComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getName(),
                comment.getAuthor().getRole().name(),
                comment.getMessage(),
                comment.isInternal(),
                comment.getCreatedAt()
        );
    }
}
