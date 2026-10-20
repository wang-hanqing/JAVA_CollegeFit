/**
 * Statuses a lens can return. INFO is neutral context (for example open admission or an admission rate);
 * the academic lens uses it so that school statistics are never shown as a personal "match".
 */
public enum FitStatus { MATCH, MISMATCH, INFO, UNKNOWN, NEEDS_VERIFICATION }
