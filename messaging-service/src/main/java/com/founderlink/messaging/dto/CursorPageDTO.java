package com.founderlink.messaging.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response shape for cursor-based pagination.
 * <ul>
 *   <li>{@code nextCursor} — pass as {@code ?before=} to load older messages</li>
 *   <li>{@code prevCursor} — pass as {@code ?after=}  to load newer messages / catch-up</li>
 * </ul>
 * Both cursors are {@code null} when the respective end of history is reached.
 */
@Data
@Builder
public class CursorPageDTO<T> {

    private List<T> content;

    /** ID of the oldest message in this batch — use as {@code ?before=} for next "load older" call. */
    private Long nextCursor;

    /** ID of the newest message in this batch — use as {@code ?after=} for real-time catch-up. */
    private Long prevCursor;
}
