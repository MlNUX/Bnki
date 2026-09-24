# Konzept: „bnki" – Karteikarten-Lern-App

> Eine schlanke Android-App zum Lernen mit Karteikarten und intelligenter
> Wiederholung (Spaced Repetition). Privates Hobbyprojekt.

---

## 1. Idee in einem Satz

Eine schlanke Android-App, mit der du eigene Karteikarten-Stapel anlegst und
sie per **Spaced Repetition** (SM-2, das Prinzip hinter Anki) so lernst, dass du
genau dann wiederholst, wenn du kurz vorm Vergessen bist.

## 2. Kernnutzen

- Eigene Karten erstellen: Vorderseite (Frage) / Rückseite (Antwort)
- In Stapel/Decks organisieren (z. B. „Spanisch Vokabeln", „Bio Kapitel 3")
- Lernsession: Karte antippen → umdrehen → selbst bewerten
- Der Algorithmus plant automatisch, wann jede Karte wieder dran ist
- Alles offline, alles auf dem Handy – keine Anmeldung nötig

## 3. Haupt-Screens (MVP)

1. **Deck-Übersicht** – Liste aller Stapel, je mit „X Karten fällig heute"
2. **Deck-Detail** – Karten ansehen/bearbeiten, „Jetzt lernen"-Button
3. **Karte erstellen/bearbeiten** – zwei Textfelder + Speichern
4. **Lern-Session** – Lernschleife mit Karte + Bewertungsknöpfen
5. **(später)** Statistik – Streak, gelernte Karten, Fortschritt

## 4. Lernlogik (Herzstück)

Vereinfachter **SM-2-Algorithmus** (Prinzip hinter Anki):

- Jede Karte hat: Intervall (Tage), „Easiness"-Faktor, Wiederholungszähler,
  nächstes Fälligkeitsdatum
- Bewertung „Nochmal / Gut / Leicht" verändert Intervall & Faktor
- Fällige Karten = alle mit `dueDate <= heute`

---

## 5. Technische Umsetzung

| Baustein     | Empfehlung                  | Warum                                      |
| ------------ | --------------------------- | ------------------------------------------ |
| UI           | **Jetpack Compose**         | Moderner Standard, weniger Boilerplate     |
| Datenbank    | **Room** (SQLite)           | Perfekt für Decks/Cards, offline, robust   |
| Architektur  | MVVM + Repository           | Sauber, testbar, überschaubar              |
| Sprache      | Kotlin + Coroutines/Flow    | Standard für modernes Android              |
| Navigation   | Navigation Compose          | Einfaches Routing zwischen Screens         |
| Formeln      | **Markwon + JLatexMath**    | Text mit LaTeX (`$...$`), offline, kein WebView |
| Handschrift  | **Compose Canvas (Stylus)** | Antwort mit dem Stift schreiben, dann vergleichen |
| Erinnerungen | **WorkManager + Notifications** | Tägliche Lern-Erinnerung, auch nach Neustart |
| Import/Export| **Storage Access Framework** | Dateien wählen/speichern ohne Extra-Rechte |
| Widget       | **Glance (Compose für Widgets)** | Homescreen-Widget im Compose-Stil |

### Datenmodell (Grundgerüst)

**Deck**
- `id`, `name`, `createdAt`
- `newCardsPerDay` – tägliches Limit an **neuen** Karten (Default z. B. 20)
- `maxReviewsPerDay` – tägliches Limit an **Wiederholungen** (Default z. B. 100, 0 = unbegrenzt)

**StudyLog** (fürs tägliche Limit & Statistik)
- `id`, `deckId`, `cardId`, `reviewedAt`, `quality`, `wasNew`
- daraus lässt sich „heute schon gelernt" pro Deck zählen und die Statistik bauen

**Card**
- `id`, `deckId` (FK) – jede Karte gehört zu genau einem Stapel
- `front` – Vorderseite (Frage)
- `back` – Rückseite (Antwort)
- `hint` – Hinweis/Tipp (optional, in der Session einblendbar)
- `tags` – Schlagworte zum Filtern/Suchen (z. B. „unregelmäßig, Kapitel 3")
- `intervalDays`, `easiness` (Start 2.5, min 1.3), `repetitions`, `dueDate`

> **Tags**: Der einfachste Weg ist ein Textfeld, das kommagetrennte Tags als
> `String` speichert. Sauberer (aber mehr Aufwand) wären eigene `Tag`- und
> `CardTag`-Tabellen (n:m). Für ein Hobby-MVP reicht das Textfeld – Umbau
> später jederzeit möglich.

### LaTeX-/Formel-Unterstützung

Die Felder **Vorderseite, Rückseite und Hinweis** unterstützen **Text gemischt mit
LaTeX-Formeln**. Man tippt normalen Text und setzt Formeln in `$...$` (inline) bzw.
`$$...$$` (abgesetzt), z. B.:

```
Die Lösung der Gleichung $ax^2 + bx + c = 0$ ist $$x = \frac{-b \pm \sqrt{b^2-4ac}}{2a}$$
```

- **Eingabe** (Editier-Screen): rohes Textfeld mit LaTeX-Syntax
- **Anzeige** (Lern-Session): gerendert als Text + Formeln
- **Technik**: [Markwon](https://noties.io/Markwon/) mit der LaTeX-Erweiterung
  (`io.noties.markwon:ext-latex`), die intern **JLatexMath** nutzt – nativ,
  komplett offline, kein WebView
- **Compose-Anbindung**: Markwon rendert in eine `TextView` → in Compose via
  `AndroidView` einbetten (kleine `LatexText`-Composable, projektweit
  wiederverwendbar)
- **Live-Vorschau** (optional, Ausbau): im Editor eine Vorschau der gerenderten
  Karte anzeigen

### Codeblöcke

Die Felder **Vorderseite, Rückseite und Hinweis** unterstützen außerdem
Markdown-Codeblöcke. Code wird in drei Backticks eingeschlossen; eine optionale
Sprache steht direkt hinter dem öffnenden Fence. Die Anzeige nutzt eine
Monospace-Schrift, einen abgesetzten Hintergrund, horizontalen Scroll bei langen
Zeilen und erlaubt das Kopieren des Codes.

```text
```kotlin
val answer = 42
```
```

### Handschriftliche Antwort (Stift-Eingabe)

Im Abfrage-Modus kann die Antwort **handschriftlich mit dem Stylus** geschrieben
werden, bevor die Lösung aufgedeckt wird.

- **Ablauf**: Frage sehen → Antwort mit Stift auf die Zeichenfläche schreiben →
  „Auflösen" → richtige Antwort erscheint über/neben der eigenen Handschrift →
  selbst bewerten (Nochmal / Gut / Leicht)
- **Nur Stift**: Es zeichnet ausschließlich echte Stylus-Eingabe
  (`PointerType.Stylus`); Finger/Handballen werden ignoriert (Palm-Rejection)
- **Technik**: eigene `HandwritingCanvas`-Composable auf Basis von
  `Canvas` + `pointerInput`/`awaitPointerEvent`. Striche werden als `Path`-Liste
  gehalten und gezeichnet
- **Bedienung**: Buttons für **Löschen** (alles) und **Rückgängig** (letzter Strich)
- **Leere Antwort = falsch**: Wurde beim „Auflösen" nichts geschrieben (keine
  Striche auf der Fläche), wird die Karte **automatisch als falsch** gewertet –
  d. h. SM-2 „Nochmal" (`repetitions = 0`, `intervalDays = 1`). Die
  Bewertungsknöpfe werden dann übersprungen bzw. deaktiviert und die richtige
  Antwort wird direkt zum Nachschauen angezeigt
- **Hinweis**: Die Handschrift dient nur zum Selbst-Vergleich – sie wird **nicht**
  als Text erkannt und **nicht gespeichert** (wird beim Kartenwechsel zurückgesetzt)
- **Ausbau (später)**: automatische Handschrifterkennung via ML Kit Digital Ink
  → Antwort automatisch prüfen

### Tägliches Lernlimit pro Deck

Pro Deck einstellbar, wie viel man an einem Tag lernt – verhindert riesige
Rückstände nach Pausen.

- **Zwei Limits**: neue Karten/Tag (`newCardsPerDay`) und Wiederholungen/Tag
  (`maxReviewsPerDay`)
- **Zählung** über `StudyLog`: „heute in diesem Deck gelernt" = Einträge mit
  `reviewedAt` seit Tagesbeginn
- **Session-Logik**: `StudyViewModel` lädt nur so viele fällige/neue Karten, bis
  das jeweilige Limit erreicht ist; danach „Für heute geschafft ✅"
- Einstellbar im **Deck-Detail** (bzw. Deck-Einstellungen)

### Import / Export

Karten sichern und zwischen Geräten/PC austauschen.

- **Format**: primär **CSV** (`front;back;hint;tags`) – einfach, mit Excel/Text
  bearbeitbar; optional später Anki-`.apkg`
- **Export**: alle oder ein Deck als Datei speichern (Ort per System-Dialog wählen)
- **Import**: CSV-Datei wählen → Vorschau → in bestehendes/neues Deck übernehmen
- **Technik**: **Storage Access Framework** (`ACTION_CREATE_DOCUMENT` /
  `ACTION_OPEN_DOCUMENT`) – keine Speicher-Berechtigungen nötig

### Tägliche Erinnerungen

Erinnerung, wenn Karten fällig sind.

- **Inhalt**: z. B. „📚 14 Karten heute fällig" (Summe über alle Decks)
- **Zeit**: vom Nutzer einstellbar (z. B. täglich 18:00 Uhr)
- **Technik**: **WorkManager** (periodischer Task) prüft fällige Karten und postet
  eine **Notification**; übersteht Neustart. Ab Android 13 Runtime-Permission
  `POST_NOTIFICATIONS` abfragen
- Tippen auf die Notification öffnet direkt den Lern-Modus

### Statistik

Fortschritt sichtbar machen (motiviert).

- **Kennzahlen** aus `StudyLog`: gelernte Karten pro Tag, Erfolgsquote
  (richtig/gesamt), **Streak** (Tage in Folge gelernt), fällige Karten pro Deck
- **Ansicht**: einfaches Balken-/Kalender-Diagramm (Compose Canvas) + Zahlen
- Erst schlicht halten – reine Zahlen reichen für den Start

### Homescreen-Widget

Fälligkeiten auf einen Blick, Einstieg mit einem Tipp.

- **Inhalt**: Anzahl heute fälliger Karten (gesamt oder pro Lieblings-Deck) +
  „Jetzt lernen"-Tap
- **Technik**: **Glance** (Jetpack – Compose-Stil für App-Widgets). Aktualisierung
  periodisch via WorkManager bzw. nach jeder Lern-Session
- Tap aufs Widget öffnet die App direkt im Lern-Modus

---

## 6. Umsetzungsplan (Phasen)

### Phase 0 – Projekt-Setup (Views → Compose)
- `gradle/libs.versions.toml`: Compose BOM, Compose-Compiler-Plugin, Room (+KSP),
  Lifecycle/ViewModel-Compose, Navigation Compose
- `app/build.gradle.kts`: `buildFeatures { compose = true }`, Plugins aktivieren,
  Java 17, Compose-/Room-/Navigation-Dependencies statt `appcompat`
- Markwon + `ext-latex` für die Formel-Darstellung ergänzen
- Für Phase 6: WorkManager (`androidx.work:work-runtime-ktx`) und
  Glance (`androidx.glance:glance-appwidget`) – erst bei Bedarf

### Phase 1 – Datenschicht (Room)
```
data/
  ├─ Deck.kt          @Entity: id, name, createdAt,
  │                   newCardsPerDay, maxReviewsPerDay
  ├─ Card.kt          @Entity: id, deckId(FK), front, back, hint, tags,
  │                   intervalDays, easiness, repetitions, dueDate
  ├─ StudyLog.kt      @Entity: id, deckId, cardId, reviewedAt, quality, wasNew
  ├─ DeckDao.kt       CRUD + "Decks mit Anzahl fälliger Karten"
  ├─ CardDao.kt       CRUD + "fällige Karten" (dueDate <= now), Limit-Abfragen
  ├─ StudyLogDao.kt   Einträge schreiben + Tages-/Statistik-Abfragen
  └─ BnkiDatabase.kt  @Database, Singleton
```

### Phase 2 – SM-2 Algorithmus (reine Kotlin-Logik, testbar)
- `domain/Sm2.kt`: `review(card, quality) -> Card`
- quality < 3 → `repetitions = 0`, `intervalDays = 1`
- quality ≥ 3 → repetitions++, Intervall wächst (1 → 6 → interval × easiness)
- Easiness neu berechnen, min. 1.3
- `dueDate = now + intervalDays`
- **Unit-Tests** in `app/src/test/...`

### Phase 3 – Architektur (MVVM)
```
ui/
  ├─ deck/    DeckListViewModel + DeckListScreen
  ├─ card/    CardEditViewModel + CardEditScreen
  ├─ study/   StudyViewModel + StudyScreen
  └─ theme/   Compose-Theme
repository/
  └─ BnkiRepository.kt   kapselt DAOs, liefert Flows
```

### Phase 4 – Screens & Navigation
1. `decks` → **DeckListScreen**: LazyColumn, FAB „+ Deck", „N fällig"
2. `deck/{id}` → **DeckDetailScreen**: Kartenliste, „Lernen", „+ Karte"
3. `deck/{id}/edit` → **CardEditScreen**: Felder für Vorderseite, Rückseite,
   Hinweis/Tipp (alle mit LaTeX-Eingabe), Tags + Stapel-Auswahl + Speichern
   (optional: Live-Vorschau der gerenderten Karte)
4. `study/{deckId}` → **StudyScreen**: Frage + Handschrift-Zeichenfläche (Stift)
   → „Auflösen" zeigt die Antwort → 3 Bewertungsbuttons
   (leere Fläche → automatisch „falsch"), respektiert das Tageslimit
5. `deck/{id}/settings` → **DeckSettingsScreen**: Name, Tageslimits, Export/Löschen
6. `stats` → **StatsScreen**: Streak, gelernt pro Tag, Erfolgsquote

### Phase 5 – Feinschliff
- Leerzustände („Noch keine Decks – leg los!")
- Session-Ende-Screen („12 Karten gelernt 🎉" / „Für heute geschafft ✅")
- Dark Mode (Compose-Theme greift `values-night` auf)

### Phase 6 – Zusatzfeatures
- **Tageslimit**: `StudyViewModel` begrenzt Karten pro Session anhand `StudyLog`
- **Import/Export** (CSV) via Storage Access Framework
- **Tägliche Erinnerung**: WorkManager + Notification (+ Permission ab Android 13)
- **Statistik**-Screen aus `StudyLog`
- **Widget** mit Glance (heute fällige Karten + „Jetzt lernen")

---

## 7. Umsetzungs-Reihenfolge (Commits)

1. Gradle/Compose-Setup + leere MainActivity mit Compose läuft
2. Room-Datenschicht + Repository
3. SM-2 + Unit-Tests (grün)
4. Deck-Liste (anlegen/anzeigen)
5. Karten anlegen/bearbeiten
6. Study-Screen mit SM-2-Anbindung + Stift-Eingabe
7. Tageslimit + StudyLog
8. Import/Export (CSV)
9. Erinnerungen (WorkManager + Notification)
10. Statistik-Screen
11. Widget (Glance)
12. Feinschliff

Jeder Schritt ist einzeln lauffähig/testbar.

---

## 8. Ausbau-Ideen (nach dem MVP)

Bilder auf Karten · Anki-`.apkg`-Import · Cloud-Backup ·
Umgekehrtes Lernen (Rück→Vorder) · Handschrifterkennung (ML Kit) ·
Widget pro Deck · Lern-Erinnerung mit Snooze

---

## 9. Eckdaten (Projekt)

- Package: `com.example.bnki`
- minSdk 27, targetSdk 36
- Plattform: Android (nativ, Kotlin)
- Rahmen: privates Lern-/Hobbyprojekt
