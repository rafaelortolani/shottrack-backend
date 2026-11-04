package com.shottrack.backend.application.seriesphoto.storage;

import java.time.Instant;

public record StoredObject(String key, Instant lastModified) {
}
