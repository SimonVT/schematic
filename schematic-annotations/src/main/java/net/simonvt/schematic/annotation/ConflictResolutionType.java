package net.simonvt.schematic.annotation;

/**
 * Conflict resolution algorithms.
 */
public enum ConflictResolutionType {
  NONE,
  ROLLBACK,
  ABORT,
  FAIL,
  IGNORE,
  REPLACE,
}
