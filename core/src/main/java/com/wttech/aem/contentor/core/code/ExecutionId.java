package com.wttech.aem.contentor.core.code;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class ExecutionId {

  private ExecutionId() {
    // intentionally empty
  }

  /**
   * Generate something looking Sling job compatible e.g
   * '2024-11-13-9-15-6c393bf1-97ea-4e52-a1db-55c0be12d155_177'
   */
  public static String generate() {
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
    String formattedDate = now.format(formatter);
    String uuid = UUID.randomUUID().toString();
    return String.format("%s-%s_%d", formattedDate, uuid, now.getNano());
  }
}
