package ua.constitution.domain.content

/**
 * Initialization / integrity diagnostics for the loaded constitution content.
 *
 * Segregated (ISP) so UI that surfaces a load-error banner depends only on these status fields,
 * not on the content interfaces. The exact semantics (e.g. verification reported as passing even
 * on a hash mismatch) are implementation details of the concrete source and are pinned by its tests.
 */
interface IntegrityStatus {
    val integrityVerificationPass: Boolean
    val computedHash: String
    val usedFallback: Boolean
    val initializationError: String
}
