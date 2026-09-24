# CSV-Import für Karteikarten

Diese Datei beschreibt das Format für den CSV-Import in bnki. Der Import fügt
Karten in den aktuell geöffneten **Kartenstapel** ein. In einem Stapel mit
Unterstapeln ist ein Import absichtlich nicht möglich.

## Spalten

Die erste Zeile kann diese Kopfzeile enthalten (empfohlen):

```csv
front;back;hint;tags
```

| Spalte | Pflicht | Inhalt |
| --- | --- | --- |
| `front` | ja | Vorderseite bzw. Frage |
| `back` | ja | Rückseite bzw. richtige Antwort |
| `hint` | nein | Tipp, der während des Lernens eingeblendet werden kann |
| `tags` | nein | Kommagetrennte Schlagwörter, z. B. `bio,zelle,prüfung` |

Die Trennung der Spalten erfolgt mit einem **Semikolon** (`;`), nicht mit einem
Komma. Zeilen ohne Vorder- oder Rückseite werden nicht importiert.

## Einfaches Beispiel

```csv
front;back;hint;tags
Was ist die Hauptstadt von Frankreich?;Paris;Liegt an der Seine.;geografie,europa
2 + 2;4;;mathe,grundlagen
```

## Mehrzeilige Inhalte und Sonderzeichen

Ein Feld muss in doppelte Anführungszeichen gesetzt werden, wenn es einen
Zeilenumbruch, ein Semikolon oder ein doppeltes Anführungszeichen enthält.
Ein doppeltes Anführungszeichen innerhalb eines solchen Feldes wird verdoppelt
geschrieben (`""`).

```csv
front;back;hint;tags
"Nenne zwei Farben; die in der Flagge vorkommen.";"Schwarz und Rot";"Die dritte Farbe ist Gold.";deutschland,flagge
"Ein Zitat mit ""Anführungszeichen"".";Antwort;;beispiel
```

## LaTeX-Formeln

In `front`, `back` und `hint` sind LaTeX-Formeln möglich:

- Inline-Formel: `$x^2$`
- Abgesetzte Formel: `$$\frac{a}{b}$$`

```csv
front;back;hint;tags
"Löse $x^2 = 9$.";"$x = \pm 3$";"Ziehe die Quadratwurzel.";mathe,latex
```

## Codeblöcke

Codeblöcke funktionieren ebenfalls in `front`, `back` und `hint`. Sie werden
mit drei Backticks eingeschlossen. Eine optionale Sprachangabe steht direkt
hinter den öffnenden Backticks, z. B. `kotlin`, `python`, `sql` oder `javascript`.

Da Codeblöcke mehrzeilig sind, muss das komplette CSV-Feld in Anführungszeichen
stehen. Doppelte Anführungszeichen im Code müssen dabei verdoppelt werden.

````csv
front;back;hint;tags
"Was gibt dieser Kotlin-Code aus?
```kotlin
println(6 * 7)
```";"42";"Der Operator `*` multipliziert.";code,kotlin
"Schreibe eine SQL-Abfrage für alle Namen.";"```sql
SELECT name
FROM users;
```";"Nutze SELECT und FROM.";code,sql
````

