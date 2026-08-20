package com.tgm.raidwatcher

data class RaidData(
    val timer: String?,
    val bruiser: Long?,
    val hitmen: Long?,
    val bikers: Long?,
    val confidence: Int,
    val found: Boolean
) {
    val fingerprint: String
        get() = listOf(timer, bruiser, hitmen, bikers).joinToString("|")

    fun display(): String {
        val total = listOfNotNull(bruiser, hitmen, bikers).sum()

        fun formatCount(n: Long): String {
            return when {
                n >= 1_000_000 ->
                    "%.2fM".format(n / 1_000_000.0)

                n >= 1_000 ->
                    "%.1fK".format(n / 1_000.0)

                else ->
                    n.toString()
            }
        }

        val rows = listOf(
            bruiser?.let { "🥊 Bruiser  ${formatCount(it)}" },
            hitmen?.let { "🔫 Hitmen/Sniper  ${formatCount(it)}" },
            bikers?.let { "🏍 Bikers  ${formatCount(it)}" }
        ).filterNotNull()

        val percentages = if (total > 0) {
            listOf(
                "Bruiser" to bruiser,
                "Hitmen/Sniper" to hitmen,
                "Bikers" to bikers
            ).mapNotNull { (name, value) ->
                value?.let {
                    "$name ${"%.0f".format(it * 100.0 / total)}%"
                }
            }
        } else {
            emptyList()
        }

        return buildString {
            append("🚨 INCOMING RAID\n")
            append("⏱ ${timer ?: "??:??"}\n")
            append("👥 ${formatCount(total)} total\n\n")

            rows.forEach {
                append(it)
                append("\n")
            }

            if (percentages.isNotEmpty()) {
                append("\n📊 ")
                append(percentages.joinToString(" • "))
                append("\n")
            }

            append("\n🎯 ")
            append(RaidParser.classify(bruiser, hitmen, bikers))

            append("\n📈 Confidence: ")
            append(confidence)
            append("%")
        }
    }
}

object RaidParser {

    private val timerRegex =
        Regex("""(?<!\d)(\d{1,2}):([0-5]\d)(?!\d)""")

    private val numberRegex =
        Regex(
            """(?<!\d)(\d{1,3}(?:[,.]\d{3})+|\d+(?:[,.]\d+)?\s*[MKmk]?)(?![\dMKmk])"""
        )

    private val troopNames = mapOf(
        "bruiser" to listOf(
            "bruiser",
            "brawler",
            "hoodlum",
            "hard hitter",
            "gorilla"
        ),

        "hitmen" to listOf(
            "hitmen",
            "hitman",
            "gangster",
            "gunner",
            "marksman",
            "sniper",
            "deadeye"
        ),

        "bikers" to listOf(
            "biker",
            "bikers",
            "dirt devil",
            "street stormer",
            "speed demon",
            "hell on wheels"
        )
    )

    fun parse(raw: String): RaidData {

        val text = raw
            .replace('\n', ' ')
            .lowercase()

        val timer =
            timerRegex.find(text)?.value

        val bruiser =
            findNearbyNumber(
                text,
                troopNames["bruiser"] ?: emptyList()
            )

        val hitmen =
            findNearbyNumber(
                text,
                troopNames["hitmen"] ?: emptyList()
            )

        val bikers =
            findNearbyNumber(
                text,
                troopNames["bikers"] ?: emptyList()
            )

        var confidence = 25

        if (timer != null) confidence += 25
        if (bruiser != null) confidence += 15
        if (hitmen != null) confidence += 15
        if (bikers != null) confidence += 15

        confidence = confidence.coerceAtMost(99)

        val found =
            timer != null &&
            listOf(
                bruiser,
                hitmen,
                bikers
            ).any { it != null }

        return RaidData(
            timer = timer,
            bruiser = bruiser,
            hitmen = hitmen,
            bikers = bikers,
            confidence = confidence,
            found = found
        )
    }

    private fun findNearbyNumber(
        text: String,
        names: List<String>
    ): Long? {

        for (name in names) {

            var start = 0

            while (true) {

                val index =
                    text.indexOf(name, start)

                if (index < 0) {
                    break
                }

                val windowStart =
                    maxOf(0, index - 80)

                val windowEnd =
                    minOf(
                        text.length,
                        index + name.length + 140
                    )

                val window =
                    text.substring(
                        windowStart,
                        windowEnd
                    )

                val match =
                    numberRegex.find(window)

                if (match != null) {

                    val value =
                        parseCount(match.value)

                    if (
                        value != null &&
                        value in 1..3_500_000
                    ) {
                        return value
                    }
                }

                start =
                    index + name.length
            }
        }

        return null
    }

    private fun parseCount(raw: String): Long? {

        val value =
            raw
                .replace(",", "")
                .replace(" ", "")

        return try {

            when {

                value.endsWith("m", true) -> {
                    (
                        value
                            .dropLast(1)
                            .toDouble() *
                            1_000_000
                    ).toLong()
                }

                value.endsWith("k", true) -> {
                    (
                        value
                            .dropLast(1)
                            .toDouble() *
                            1_000
                    ).toLong()
                }

                else -> {
                    value
                       
