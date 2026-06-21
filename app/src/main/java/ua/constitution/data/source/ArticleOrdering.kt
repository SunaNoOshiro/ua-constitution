package ua.constitution.data.source

/**
 * Sort key for an *encoded* article id (see [ConstitutionJsonDeserializer] toArticleId, which encodes
 * a fractional id like 16.1 as round(16.1 * 10) = 161).
 *
 * Behavior is preserved verbatim from the former inline `sortBy`: ids above 1000 are treated as
 * encoded fractionals and divided by 10 (so 1291 = "129.1" sorts between 129 and 130), everything
 * else sorts by its integer value.
 *
 * KNOWN QUIRK (pinned, not fixed here): a *low* fractional id such as 16.1 encodes to 161, which is
 * <= 1000, so it is NOT divided — it sorts as 161.0 (after article 160) instead of between 16 and 17,
 * and it also collides with the real Article 161. Fixing this needs a change to the id ENCODING
 * (the encoded value is lossy/ambiguous), which is an observable-ordering change deferred for an
 * explicit decision. Extracted here so the rule is unit-tested and lives in one place.
 */
fun articleSortKey(id: Int): Double =
    if (id > 1000) id.toDouble() / 10.0 else id.toDouble()
