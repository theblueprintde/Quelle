package com.payas.quelle.data

data class Quote(val text: String, val author: String)

/** Günün sözü: her gün listeden sıradaki söz gösterilir. */
val QUOTES = listOf(
    Quote("Bin millik yolculuk tek bir adımla başlar.", "Lao Tzu"),
    Quote("Damlaya damlaya göl olur.", "Atasözü"),
    Quote("Hayatta en hakiki mürşit ilimdir.", "Mustafa Kemal Atatürk"),
    Quote("Yapabileceğine inan, yolun yarısını geçmiş olursun.", "Theodore Roosevelt"),
    Quote("Bugünün işini yarına bırakma.", "Atasözü"),
    Quote("Başarı, her gün tekrarlanan küçük çabaların toplamıdır.", "Robert Collier"),
    Quote("İşleyen demir ışıldar.", "Atasözü"),
    Quote("Her şey, yapılana kadar imkânsız görünür.", "Nelson Mandela"),
    Quote("Disiplin, hedeflerle başarı arasındaki köprüdür.", "Jim Rohn"),
    Quote("Sabreden derviş muradına ermiş.", "Atasözü"),
    Quote("Bilgiye yapılan yatırım en iyi faizi getirir.", "Benjamin Franklin"),
    Quote("Emek olmadan yemek olmaz.", "Atasözü"),
    Quote("Hayal gücü bilgiden daha önemlidir.", "Albert Einstein"),
    Quote("Ağaç yaşken eğilir.", "Atasözü"),
    Quote("Az olsun, öz olsun.", "Atasözü"),
)

fun quoteFor(dayOfYear: Int): Quote = QUOTES[Math.floorMod(dayOfYear, QUOTES.size)]
