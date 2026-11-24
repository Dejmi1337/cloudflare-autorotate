<!-- coderabbit-ai-generated:v1 -->

# Elytra Pierścienie

Minigra dla serwera Paper 1.21 i Java 21. Gracze zdobywają punkty, przelatując na elytrze przez świecące pierścienie. Runda ma określony czas, a po jej zakończeniu wyświetlane są wyniki.

## Instalacja

1. Umieść plik `ElytraPierscienie-1.0.0.jar` w folderze `plugins` serwera Paper 1.21.
2. Uruchom ponownie serwer.
3. Gracze potrzebują własnej elytry i rakiet. Plugin nie rozdaje przedmiotów ani nie teleportuje graczy.

## Pierwsza gra

1. Administrator z uprawnieniem `elytrapierscienie.admin` (domyślnie operator) ustawia się w powietrzu w miejscu środka pierścienia i patrzy w kierunku, w którym gracze mają przez niego przelecieć.
2. Wpisz `/elytra dodaj start 3 5`, aby utworzyć pionowy pierścień o promieniu 3 bloków, dający 5 punktów. Powtórz z innymi nazwami, żeby stworzyć trasę. Pierścienie są widoczne jako cząsteczki i można przez nie przelatywać w obie strony.
3. Każdy gracz wpisuje `/elytra dolacz` i zakłada elytrę.
4. Administrator wpisuje `/elytra start 180`, aby rozpocząć rundę na 180 sekund. Wygrywa osoba z największą liczbą punktów; dziesięć najlepszych wyników zostanie ogłoszonych uczestnikom.

Punkty są przyznawane tylko w trakcie rundy, gdy gracz szybuje na elytrze. Ten sam pierścień może być zaliczony ponownie po 5 sekundach; ten czas można zmienić w konfiguracji. Można dołączyć także w trakcie rundy, startując od zera.

## Komendy

| Komenda | Działanie |
| --- | --- |
| `/elytra pomoc` | Wyświetla dostępne komendy. |
| `/elytra dolacz` | Dołącza do gry. |
| `/elytra wyjdz` | Opuszcza grę i usuwa wynik gracza. |
| `/elytra wyniki` | Pokazuje wyniki uczestników. |
| `/elytra dodaj <nazwa> [promień] [punkty]` | Tworzy pierścień; domyślnie promień 3, 1 punkt. Nazwa: litery bez polskich znaków, cyfry, `_` lub `-`. |
| `/elytra usun <nazwa>` | Usuwa pierścień. |
| `/elytra lista` | Wyświetla pierścienie. |
| `/elytra start [sekundy]` | Rozpoczyna rundę; czas 10–3600 sekund. |
| `/elytra stop` | Kończy rundę i ogłasza wyniki. |

Komendy dodawania i usuwania pierścieni, lista, start oraz stop wymagają uprawnienia administratora. Edycja pierścieni wymaga zakończenia rundy. Maksymalnie można utworzyć 64 pierścienie o promieniu 1–20 bloków i wartości 1–10000 punktów każdy.

## Konfiguracja i dane

Po pierwszym uruchomieniu plik `plugins/ElytraPierscienie/config.yml` zawiera `czas-rundy` (domyślnie 180 sekund) i `przerwa-sekundy` (domyślnie 5). Po edycji uruchom ponownie serwer. Pierścienie są zapisywane w `pierscienie.yml`; uczestnicy i wyniki są przechowywane w pamięci i znikają po restarcie. Nowa runda zeruje punkty.

## Kod źródłowy

Archiwum źródłowe zawiera projekt Maven. Aby zbudować plugin, użyj JDK 21 i polecenia `mvn clean package`; gotowy plik znajduje się w katalogu `target`.
