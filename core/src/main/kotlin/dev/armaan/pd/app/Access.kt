package dev.armaan.pd.app

/** SPEC §5.1: a source's permission state. */
enum class Access { Granted, NotAsked, Denied, PermanentlyDenied }

/** What plumbing reads from the system: granted now, and whether it would show a rationale. */
data class PermissionCheck(val granted: Boolean, val rationale: Boolean)

/**
 * SPEC §5.1. [asked] is the source's `asked_*` flag. Null until both the check and the flag have
 * been read, so nothing is requested off a default.
 */
fun access(check: PermissionCheck?, asked: Boolean?): Access? = when {
    check == null || asked == null -> null
    check.granted -> Access.Granted
    !asked -> Access.NotAsked
    check.rationale -> Access.Denied
    else -> Access.PermanentlyDenied
}
