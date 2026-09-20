package lab.testing.async;

/**
 * Lifecycle of a report handed to {@link AsyncReportJob}.
 *
 * <p>The states are deliberately coarse: a report is accepted ({@link #QUEUED}), picked up by a
 * worker ({@link #RUNNING}) and then either finished ({@link #COMPLETED}) or abandoned because its
 * generation threw ({@link #FAILED}). {@code COMPLETED} and {@code FAILED} are terminal, so a
 * caller that reads the status can always tell "still working" apart from "will not change again".
 */
public enum ReportStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED
}
