package com.tgm.raidwatcher

data class RaidData(
    val timer: String?, val bruiser: Long?, val hitmen: Long?, val bikers: Long?,
    val confidence: Int, val found: Boolean
) {
    val fingerprint get() = listOf(timer, bruiser, hitmen, bikers).joinToString("|")

    fun display(): String {
        val total = listOfNotNull(bruiser, hitmen, bikers).sum()
        fun f(n: Long) = when {
            n >= 1_000_000 -> "%.2fM".format(n / 1_000_000.0)
            n >= 1_000 -> "%.1fK".format(n / 1_000.0)
            else -> n.toString()
        }
        val rows = listOf(
            bruiser?.let { "🥊 Bruiser  ${f(it)}" },
            hitmen?.let { "🔫 Hitmen/Sniper  ${f(it)}" },
            bikers?.let { "🏍 Bikers  ${f(it)}" }
        ).filterNotNull()
        val pct = listOf("Bruiser" to bruiser, "Hitmen/Sniper" to hitmen, "Bikers" to bikers)
            .mapNotNull { (n,v) -> v?.let { "$n ${"%.0f".format(it*100.0/total)}%" } }
        return "🚨 INCOMING RAID\n⏱ ${timer ?: "??:??"}\n👥 ${f(total)} total\n\n" +
            rows.joinToString("\n") + "\n\n📊 " + pct.joinToString(" • ") +
            "\n🎯 " + classify(bruiser, hitmen, bikers) +
            "\n📈 Confidence: $confidence%"
    }
}

object RaidParser {
    private val timer = Regex("""(?<!\d)(\d{1,2}):([0-5]\d)(?!\d)""")
    private val number = Regex("""(?<!\d)(\d{1,3}(?:[,.]\d{3})+|\d+(?:[,.]\d+)?\s*[MKmk]?)(?![\dMKmk])""")
    private val names = mapOf(
        "b" to listOf("bruiser","brawler","hoodlum","hard hitter","gorilla"),
        "h" to listOf("hitmen","hitman","gangster","gunner","marksman","sniper","deadeye"),
        "k" to listOf("biker","bikers","dirt devil","street stormer","speed demon","hell on wheels")
    )

    fun parse(raw: String): RaidData {
        val t = raw.replace('\n',' ').lowercase()
        val tm = timer.find(t)?.value
        val b = near(t, names["b"]!!)
        val h = near(t, names["h"]!!)
        val k = near(t, names["k"]!!)
        var c = 25 + if (tm != null) 25 else 0
        if (b != null) c += 15
        if (h != null) c += 15
        if (k != null) c += 15
        return RaidData(tm,b,h,k,c.coerceAtMost(99),tm != null && listOf(b,h,k).any { it != null })
    }

    private fun near(t:String, ns:List<String>):Long? {
        for (n in ns) {
            val i=t.indexOf(n)
            if (i<0) continue
            val w=t.substring(maxOf(0,i-80),minOf(t.length,i+n.length+140))
            val m=number.find(w) ?: continue
            val s=m.value.replace(",","").replace(" ","")
            val v=try {
                when {
                    s.endsWith("m",true)->(s.dropLast(1).toDouble()*1_000_000).toLong()
                    s.endsWith("k",true)->(s.dropLast(1).toDouble()*1_000).toLong()
                    else->s.replace(".","").toLong()
                }
            } catch(_:Exception){null}
            if (v != null && v in 1..3_500_000) return v
        }
        return null
    }

    fun classify(b:Long?,h:Long?,k:Long?):String {
        val v=listOf("BRUISER" to b,"HITMEN/SNIPER" to h,"BIKER" to k)
            .mapNotNull{(n,x)->x?.let{n to it}}
        if(v.isEmpty()) return "UNKNOWN"
        val s=v.filter{it.second>=100_000}
        return when {
            s.size>=3 -> "3-WAY MIX"
            s.size==2 -> "${s[0].first} + ${s[1].first} MIX"
            else -> "${v.maxBy{it.second}.first} RAID"
        }
    }
}
