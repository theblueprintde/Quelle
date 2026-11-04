package com.payas.quelle

/** Saate göre selamlama: Günaydın, Tünaydın, İyi akşamlar, İyi geceler. */
fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Günaydın"
    in 12..16 -> "Tünaydın"
    in 17..21 -> "İyi akşamlar"
    else -> "İyi geceler"
}
