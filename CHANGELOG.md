Change Log
==========

Version 0.7.0
-------------
 - Breaking change: Multiple `@PrimaryKey` annotations on same table is no longer allowed.
 Instead use `@PrimaryKeyConstraint`.
 - Added annotations for table constraints
   - `@Constraints`
   - `@ForeignKeyConstraint`
   - `@PrimaryKeyConstraints`
   - `@UniqueConstraint`


Version 0.6.8
-------------
 - Support passing a `Context` to methods annotated with `@Where`.


Version 0.6.7
-------------
 - Fix issue where spaces was not added between table and joins.


Version 0.6.6
-------------
 - Added helper class for getting values from `Cursors`.
 - Ensure application `Context` is always used.
 - Added `@Join` annotation.


Version 0.6.5
-------------
Doesn't exist, for some reason.


Version 0.6.4
-------------
 - Added `IfNotExists` annotation for use with table definitions.
