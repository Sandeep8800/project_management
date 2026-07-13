/**
 * Reporting module (HLD S4/S5/S8.2, LLD S7, Database Design S8, API Design S8).
 *
 * Owns burndown/velocity/CFD/sprint-summary projections -- always
 * pre-aggregated, never computed live from raw issue history at read time.
 * CFD updates are a true incremental delta per event (ProjectionUpdateService);
 * burndown is a targeted per-sprint recompute, bounded by sprint size.
 */
package com.nexuspms.reporting;
