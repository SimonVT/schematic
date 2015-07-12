package net.simonvt.schematic.annotation;

/**
 * conflict resolution algorithms
 */
public enum ConflictResolutionType {
  NONE,
  ROLLBACK,
  ABORT,
  FAIL,
  IGNORE,
  REPLACE,
}
