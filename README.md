# OreTop

Plugin na **Paper 26.2** z ładnym GUI topek:

- zabójstwa graczy, śmierci, zabite moby, wykopane bloki,
- wykopane rudy: diamenty, szmaragdy, netheryt (ancient debris), złoto, żelazo, miedź, redstone, lapis, węgiel, kwarc,
- menu główne, ranking z paginacją (28 graczy na stronę, głowy ze skinami, złoto/srebro/brąz dla top 3),
- okno statystyk gracza z jego miejscami w każdej kategorii,
- zapis w SQLite (plik `plugins/OreTop/stats.db`), zapis asynchroniczny, bez lagów,
- brak zakładki balance / zależności od Vault.

## Komendy

| Komenda | Opis |
|---|---|
| `/top` (`/topki`, `/oretop`, `/leaderboard`) | otwiera menu główne |
| `/top <kategoria>` | otwiera konkretny ranking, np. `/top diamond` |
| `/top reload` | przeładowuje config (`oretop.admin`) |
| `/stats [gracz]` (`/statystyki`) | statystyki swoje lub innego gracza |

Kategorie: `kills deaths mobs blocks diamond emerald netherite gold iron copper redstone lapis coal quartz`

## Uprawnienia

- `oretop.use` – domyślnie wszyscy
- `oretop.admin` – domyślnie OP

## Budowanie

Wymagana Java 25 i Maven:

```
mvn clean package
```

Gotowy plik: `target/OreTop-1.0.0.jar`. Na GitHubie build robi się sam (zakładka **Actions**, artefakt `OreTop-jar`).

## Instalacja

1. Wrzuć `OreTop-1.0.0.jar` do folderu `plugins` serwera.
2. Zrestartuj serwer.
3. Config znajdziesz w `plugins/OreTop/config.yml`.

Statystyki liczą się od momentu instalacji pluginu.
