# 🌿 KräuterTee App – Deine persönliche Kräutertee-Begleiterin

Eine vollständige Android-App (Kotlin + Jetpack Compose) für Kräuterliebhaber.

---

## 📱 Funktionen

| # | Funktion | Beschreibung |
|---|---|---|
| 1 | **Kräuter-Lexikon** | 20 Heilkräuter mit Details, Wirkung, Ernte & Trocknung |
| 2 | **KI-Erklärungen** | Claude AI erklärt jedes Kraut individuell |
| 3 | **Interaktive Karte** | OpenStreetMap – Fundstellen pinnen, Notizen & Menge |
| 4 | **Erntekalender** | GPS-basiert, klimazonengerecht, mit Erinnerungen |
| 5 | **Trocknungskalender** | Fortschrittsanzeige, Methoden, Fertigmeldung |
| 6 | **Rezepte** | Vollständiges CRUD – erstellen, bearbeiten, löschen |
| 7 | **KI-Mischungsvorschläge** | Claude schlägt harmonische Teemischungen vor |
| 8 | **KI-Chat** | Freier Chat mit dem Kräuterexperten |
| 9 | **Favoriten & Garten** | Kräuter als Favorit oder Gartenpflanze markieren |
| 10 | **Persönliche Notizen** | Pro Kraut eigene Beobachtungen notieren |

---

## 🚀 Einrichtung in Android Studio

### 1. Voraussetzungen
- **Android Studio** Ladybug (2024.2.1) oder neuer
- **JDK 17** (in Android Studio integriert)
- Android SDK API 26+

### 2. Projekt öffnen
1. Diesen Ordner `KraeuterTeeApp/` in Android Studio öffnen
2. Warten bis Gradle sync abgeschlossen ist (kann 2–3 Min dauern beim ersten Mal)
3. `Run ▶` drücken oder `Shift+F10`

### 3. API-Schlüssel einrichten (für KI-Funktionen)
1. Kostenlos anmelden auf [console.anthropic.com](https://console.anthropic.com)
2. Unter "API Keys" einen neuen Schlüssel erstellen (beginnt mit `sk-ant-...`)
3. In der App: **Einstellungen ⚙️ → API-Schlüssel eingeben → Speichern**

> ℹ️ Ohne API-Schlüssel funktioniert die App vollständig – nur KI-Funktionen sind deaktiviert.

### 4. Berechtigungen beim ersten Start
Die App fragt nach:
- 📍 **Standort** – für GPS-basierten Erntekalender & Karte
- 🔔 **Benachrichtigungen** – für Ernte- und Trocknungserinnerungen

---

## 📁 Projektstruktur

```
app/src/main/
├── kotlin/com/kraeutertee/
│   ├── MainActivity.kt              # Haupt-Activity + Navigation
│   ├── KraeuterTeeApplication.kt   # App-Klasse, Notification Channels
│   ├── api/
│   │   └── AnthropicService.kt     # Claude AI Integration
│   ├── data/
│   │   ├── AppDatabase.kt          # Room Datenbank
│   │   ├── dao/Daos.kt             # Alle DAOs
│   │   └── entities/Entities.kt    # Datenbankmodelle
│   ├── ui/
│   │   ├── theme/Theme.kt          # Botanisches Grün-Design
│   │   ├── navigation/Navigation.kt
│   │   └── screens/
│   │       ├── HerbsScreen.kt      # Kräuter-Lexikon + Detail
│   │       ├── MapScreen.kt        # OSM-Karte + Fundstellen
│   │       ├── CalendarScreen.kt   # Ernte + Trocknung
│   │       ├── RecipesScreen.kt    # Rezepte CRUD
│   │       ├── KIScreen.kt         # KI-Chat + Mischungen
│   │       └── SettingsScreen.kt   # Einstellungen
│   ├── util/
│   │   ├── HerbDatabase.kt         # 20 Kräuter (statische Daten)
│   │   ├── LocationHelper.kt       # GPS + Klimazone
│   │   └── PreferencesManager.kt  # DataStore
│   └── viewmodel/ViewModels.kt     # Alle ViewModels
└── res/
    ├── values/strings.xml
    ├── values/themes.xml
    ├── values/colors.xml
    └── drawable/ic_launcher_foreground.xml
```

---

## 🌿 Enthaltene Kräuter (20)

| Kraut | Kategorie | Ernte |
|---|---|---|
| 🌼 Echte Kamille | Beruhigung | Mai–Aug |
| 🌿 Pfefferminze | Verdauung | Mai–Sep |
| 🍃 Zitronenmelisse | Beruhigung | Mai–Sep |
| 💜 Baldrian | Schlaf | Sep–Nov |
| 💐 Lavendel | Beruhigung | Jun–Aug |
| ☀️ Johanniskraut | Stimmung | Jun–Aug |
| 🔴 Hagebutte | Immunsystem | Sep–Nov |
| 🤍 Holunderblüte | Erkältung | Mai–Jun |
| 🌱 Brennnessel | Entgiftung | Mär–Jun, Sep–Okt |
| ⚪ Schafgarbe | Verdauung | Jun–Sep |
| 🌻 Löwenzahn | Entgiftung | Mär–Mai, Sep–Okt |
| 🌿 Thymian | Erkältung | Apr–Sep |
| 🟢 Echter Salbei | Erkältung | Apr–Sep |
| 🌲 Rosmarin | Kreislauf | Ganzjährig |
| 🌾 Fenchel | Verdauung | Aug–Okt |
| 🫚 Ingwer | Immunsystem | Okt–Nov |
| 🌳 Lindenblüte | Erkältung | Jun–Jul |
| 🧡 Ringelblume | Verdauung | Jun–Okt |
| 🌸 Echinacea | Immunsystem | Jul–Sep |
| 🟣 Eisenkraut | Beruhigung | Jul–Sep |

---

## 🗺️ Karte (OpenStreetMap)
- **Kein API-Schlüssel nötig!**
- Lange drücken → Fundstelle hinzufügen
- Kraut wählen, Menge & Notiz eingeben
- Marker anklicken → Detail mit Ernteinfo
- Listenansicht aller Fundstellen

---

## 🔒 Datenschutz
- **Alle Daten lokal** auf deinem Gerät (Room-Datenbank)
- API-Schlüssel verschlüsselt in DataStore
- Keine Daten werden an Dritte übermittelt (außer API-Anfragen an Anthropic)
- OpenStreetMap-Kartenkacheln werden gecacht

---

## ⚠️ Medizinischer Hinweis
Diese App dient ausschließlich der persönlichen Information und ersetzt keine medizinische Beratung. Bei Beschwerden immer einen Arzt aufsuchen.

---

*Erstellt mit ❤️ für den persönlichen Gebrauch*
