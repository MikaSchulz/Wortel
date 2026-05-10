# Offene / optionale Aufgaben (Wortel)

Diese Datei fasst die offenen und optionalen Punkte zusammen, priorisiert sie grob und beschreibt konkrete Umsetzungsschritte sowie Beispielbefehle.

## Kurz-Checklist (Prioritätsempfehlung)
- [ ] High: Sicherstellen, dass JDK 25 zuverlässig verwendet werden kann (Toolchain / Install)
- [ ] High: `application.properties` → `application.yml` konvertieren
- [ ] Medium: Saubere Paketstruktur aus Beispiel-Layout machen
- [ ] Medium: `./gradlew wrapper` auf gewünschte Gradle-Version prüfen/locken (>= 9.1)
- [ ] Medium: Optional — Projekt auf Kotlin umstellen (größere Aufgabe)
- [ ] Low: Optional — `run` Task per `application` Plugin konfigurieren (falls gewünscht)

---

## Detailaufgaben

### 1) JDK 25 / Gradle Toolchain
- Ziel: Gradle kompiliert konsistent mit Java 25 (wie gewünscht).
- Beschreibung: Wir haben die Gradle toolchain in `build.gradle.kts` auf Java 25 gesetzt. Lokal muss entweder JDK 25 installiert sein oder eine Gradle-Toolchain-Installation konfiguriert werden.
- Schritte:
  1. Prüfe systemweit: `java -version` und `echo $JAVA_HOME`.
  2. Falls JDK 25 fehlt, installiere es (z. B. SDKMAN, Homebrew, Adoptium) oder konfiguriere Gradle, eine JDK-Installation zu verwenden.
  3. Build testen: `./gradlew clean build -x test`.
- Risiken: Wenn JDK 25 nicht installiert ist, kann Gradle fehlschlagen, wenn nicht per Toolchain heruntergeladen/verwaltet.

### 2) `application.properties` → `application.yml`
- Ziel: Konfigurationsdatei in YAML statt properties; populär bei Spring Boot.
- Beschreibung: Falls aktuell eine `src/main/resources/application.properties` existiert, konvertiere die Inhalte nach YAML und lege `application.yml` an.
- Schritte:
  1. Falls vorhanden, open `src/main/resources/application.properties` und prüfe Schlüssel.
  2. Erstelle `src/main/resources/application.yml` mit äquivalenten Einträgen, z. B.:

```yaml
server:
  port: 8080
spring:
  application:
    name: wortel
```

  3. Entferne oder behalte die alte `.properties` als Backup (Spring lädt `application.yml` bevorzugt, wenn vorhanden).

### 3) Paketstruktur aufräumen / aus Beispiel in sinnvolle Struktur überführen
- Ziel: Beispiel-/stub-Pakete in sinnvolle modulare Struktur überführen (z. B. `org.example.wordle` oder `org.example.app`).
- Beschreibung: Derzeit liegen Java-Klassen unter `org.example`. Für ein realeres Projekt empfiehlt sich mindestens eine Trennung in `controller`, `service`, `model`, `config`, `util`.
- Schritte:
  1. Entscheide Zielpaket, z. B. `org.example.wordle`.
  2. Verschiebe Klassen: z. B. `src/main/java/org/example/WordleApplication.java` → `src/main/java/org/example/wordle/WordleApplication.java` und passe package-Deklarationen an.
  3. Erzeuge Unterordner `controller`, `service`, `model`, `util` und verteile Klassen entsprechend.
  4. Update `build.gradle.kts` falls spezielle sourceSets erwünscht sind (normalerweise nicht nötig).
- Beispielstruktur:

```
src/main/java/org/example/wordle/
  Application.kt (oder .java)
  controller/
    HealthController.kt
  service/
    GameService.kt
  model/
    WordleState.kt
  util/
    IO.kt
```

### 4) Gradle Wrapper prüfen / optional updaten (>= 9.1)
- Ziel: Sicherstellen, dass Wrapper mindestens Gradle 9.1 verwendet.
- Status: `gradle/wrapper/gradle-wrapper.properties` im Repo zeigt aktuell `distributionUrl=...gradle-9.3.0-bin.zip` (bereits >= 9.1).
- Falls du eine andere Version möchtest, führe lokal aus:

```bash
./gradlew wrapper --gradle-version 9.3.0
```

oder ersetze `9.3.0` durch gewünschte Version. Danach committe die Änderungen an `gradle-wrapper.properties` und `gradle/wrapper/gradle-wrapper.jar` bleibt unverändert.

### 5) `run` Task / `application` Plugin
- Ziel: Falls du `./gradlew run` statt `bootRun` nutzen willst, kannst du das `application`-Plugin hinzufügen.
- Schritte:
  1. `build.gradle.kts` Plugins-Block ergänzen mit `application`.
  2. `application { mainClass.set("org.example.wordle.WordleApplication") }`

Hinweis: `bootRun` ist für Spring Boot typischer und startet den eingebetteten Server korrekt.

### 6) Projekt auf Kotlin umstellen (umfangreich)
- Ziel: Code von Java nach Kotlin migrieren und Kotlin-idiomatische Spring Boot App verwenden.
- Beschreibung: Migration kann schrittweise (Hybrid) oder komplett (alle Dateien) erfolgen. Erfordert Build-Änderungen und Abhängigkeiten.
- Grober Plan (schrittweise):
  1. `build.gradle.kts` anpassen: Kotlin Plugin hinzufügen

```kotlin
plugins {
  kotlin("jvm") version "1.9.20" // oder aktuellste
  kotlin("plugin.spring") version "1.9.20"
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
}
```

  2. Quellen: `src/main/java` parallel `src/main/kotlin` anlegen und erste Klassen nach Kotlin konvertieren (z. B. `WordleApplication.kt`, `IO.kt`). IntelliJ kann Java→Kotlin Konvertierung automatisieren.
  3. Tests und alle weiteren Klassen schrittweise migrieren.

- Risiken & Aufwand: mittelhoch bis hoch (je nach Projektgröße). Manche Java-Bibliotheken benötigen Kotlin-Interop-Feintuning.

### 7) Sonstige Empfehlungen / Notizen
- Wenn du die Migration auf Kotlin möchtest, empfehle ich eine Zweig-Strategie (`feature/kotlin-migration`) und schrittweises Portieren mit CI-Builds.
- Backup der momentanen `application.properties` vor Konvertierung.
- Wenn du möchtest, erledige ich eine PoC-Migration einer einzigen Klasse (`WordleApplication` und `IO`) in Kotlin, aktualisiere `build.gradle.kts` und mache einen erfolgreichen `./gradlew bootRun` Durchlauf.

---

Wenn du mir sagst, welche dieser Aufgaben ich als Nächstes automatisch anpacken soll (z. B. "Konvertiere properties → yml" oder "Erstelle neue Paketstruktur" oder "Mach eine PoC-Kotlin-Migration für Application + IO"), setze ich das direkt um und teste es lokal. Ich kann auch alles Schritt für Schritt automatisiert machen, fange dann mit der höchsten Priorität an (JDK/Toolchain / YAML-Konvertierung).

