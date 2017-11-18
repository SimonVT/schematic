package net.simonvt.schematic.annotation;

/**
 * Action triggered with foreign key ON DELETE and ON UPDATE clauses.
 */
public enum ForeignKeyAction {
  NONE,
  NO_ACTION,
  RESTRICT,
  SET_NULL,
  SET_DEFAULT,
  CASCADE,
}
