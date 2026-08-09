package com.eldora25.tayfnotes.shared

import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType

object MockNoteProvider {
    fun getMockNotes(): List<Note> = listOf(
        Note(
            id = "1",
            title = "Hoş Geldiniz",
            content = "TayfNotes ile hem renkli hem de profesyonel notlar tutabilirsiniz.",
            colorHex = "#FFAB40", // Spectrum Orange
            type = NoteType.TEXT,
            tags = listOf("Genel", "Giriş")
        ),
        Note(
            id = "2",
            title = "Alışveriş Listesi",
            content = "- Süt\n- Ekmek\n- Kahve (Premium)",
            colorHex = "#69F0AE", // Spectrum Green
            type = NoteType.CHECKLIST,
            tags = listOf("Kişisel")
        ),
        Note(
            id = "3",
            title = "Toplantı Notları",
            content = "Yeni mimari KMP üzerine kurulacak. Tüm platformlar desteklenecek.",
            colorHex = "#40C4FF", // Spectrum Blue
            type = NoteType.TEXT,
            tags = listOf("İş", "KMP")
        ),
        Note(
            id = "4",
            title = "Hatırlatma",
            content = "Akşam sporu unutma!",
            colorHex = "#B388FF", // Spectrum Purple
            type = NoteType.TEXT,
            tags = listOf("Spor")
        ),
        Note(
            id = "5",
            title = "Fikirler",
            content = "AI entegrasyonu ile notları özetle.",
            colorHex = "#FF5252", // Spectrum Red
            type = NoteType.TEXT,
            tags = listOf("Gelecek")
        ),
        Note(
            id = "6",
            title = "Özel Not",
            content = "Bu not gizli ve kilitli.",
            colorHex = "#D4AF37", // Premium Gold
            type = NoteType.TEXT,
            tags = listOf("Gizli"),
            isLocked = true
        )
    )
}
