package com.payas.quelle.pdf

import com.payas.quelle.data.PdfWord
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.InputStream

class PdfReadException(message: String) : Exception(message)

/** PDF'teki kelimeleri sayfadaki konumlarıyla birlikte okur. Ağ bağlantısı kullanmaz. */
object PdfWords {
    private const val MAX_PAGES = 20

    fun read(input: InputStream): List<PdfWord> {
        val doc = try {
            PDDocument.load(input)
        } catch (e: InvalidPasswordException) {
            throw PdfReadException("PDF şifreli. Şifresiz bir kopyasını yükle.")
        } catch (e: Exception) {
            throw PdfReadException("Dosya PDF olarak açılamadı.")
        }
        return doc.use {
            if (it.numberOfPages > MAX_PAGES) throw PdfReadException("PDF $MAX_PAGES sayfadan uzun; haftalık plan için fazla.")
            val collector = Collector()
            collector.sortByPosition = true
            collector.getText(it)
            collector.words
        }
    }

    private class Collector : PDFTextStripper() {
        val words = mutableListOf<PdfWord>()

        override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
            val positions = textPositions ?: return
            // Parçayı boşluklardan kelimelere ayır; her kelimenin konumu ilk ve son harfinden gelir.
            var current = mutableListOf<TextPosition>()
            fun flush() {
                if (current.isEmpty()) return
                val first = current.first()
                val last = current.last()
                // heightDir harfin gerçek yüksekliğidir ve yazı tipine göre çok değişir (14 pt yazıda ~6);
                // satır gruplamak ve not/başlık ayırmak için yazı boyutu (yScale) daha güvenilir.
                val height = current.maxOf { p -> p.yScale.takeIf { it > 0f } ?: p.heightDir }
                val word = current.joinToString("") { p -> p.unicode ?: "" }.trim()
                if (word.isNotEmpty()) {
                    words += PdfWord(
                        page = currentPageNo,
                        x = first.xDirAdj,
                        // yDirAdj harfin taban çizgisidir; üst kenarı kullanıyoruz.
                        y = first.yDirAdj - height,
                        width = (last.xDirAdj + last.widthDirAdj) - first.xDirAdj,
                        height = height,
                        text = word,
                    )
                }
                current = mutableListOf()
            }
            for (p in positions) {
                if (p.unicode.isNullOrBlank()) flush() else current += p
            }
            flush()
            super.writeString(text, textPositions)
        }
    }
}
