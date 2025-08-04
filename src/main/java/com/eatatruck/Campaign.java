package com.eatatruck;

import java.time.LocalDateTime;
import java.util.List;

public record Campaign(
    String id,
    LocalDateTime scheduledDateTime,
    String content,
    List<String> platforms,
    boolean isRecurring
) {}
