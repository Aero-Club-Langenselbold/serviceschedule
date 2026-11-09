package de.ct.manager.schedule.request;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestCommentResponse {
    private String id;
    private String content;
    private Instant createdAt;
    private String author;
}
