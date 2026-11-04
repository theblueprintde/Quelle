package com.payas.quelle.data

import java.net.URLEncoder

data class Song(val title: String, val artist: String) {
    /** YouTube Music'te bu şarkıyı arayan bağlantı. */
    val youtubeMusicUrl: String
        get() = "https://music.youtube.com/search?q=" + URLEncoder.encode("$title $artist", "UTF-8")
}

/** Günün şarkısı: her gün listeden sıradaki şarkı. */
val SONGS = listOf(
    Song("Here Comes the Sun", "The Beatles"),
    Song("Kuzu Kuzu", "Tarkan"),
    Song("99 Luftballons", "Nena"),
    Song("Don't Stop Me Now", "Queen"),
    Song("Senden Daha Güzel", "Duman"),
    Song("Lovely Day", "Bill Withers"),
    Song("Bir Derdim Var", "mor ve ötesi"),
    Song("Three Little Birds", "Bob Marley & The Wailers"),
    Song("Atemlos durch die Nacht", "Helene Fischer"),
    Song("Walking on Sunshine", "Katrina and the Waves"),
    Song("Şımarık", "Tarkan"),
    Song("Happy", "Pharrell Williams"),
    Song("Cambaz", "mor ve ötesi"),
    Song("Eye of the Tiger", "Survivor"),
    Song("Major Tom (Völlig losgelöst)", "Peter Schilling"),
    Song("Unwritten", "Natasha Bedingfield"),
    Song("Aşkın Olayım", "Simge"),
    Song("Viva la Vida", "Coldplay"),
    Song("Hall of Fame", "The Script"),
    Song("Firework", "Katy Perry"),
)

fun songFor(dayOfYear: Int): Song = SONGS[Math.floorMod(dayOfYear, SONGS.size)]
