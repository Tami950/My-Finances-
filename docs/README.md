# MyFinances - Documentazione

Questa cartella contiene la documentazione viva del progetto. Le specifiche vengono aggiornate quando cambia una decisione funzionale, di dominio o architetturale.

I file Markdown sono la sorgente canonica dei documenti principali. Le versioni PDF vengono rigenerate quando serve una copia da leggere/condividere.

## Documenti correnti

- [Specifiche generali - v4](./MyFinances_Specifiche_Progetto_v4.md) - Obiettivi, sezioni, regole di dominio, Casa/Pianificazione, Dashboard, Analisi futura, architettura e roadmap.
- [Schema Room Casa - v2](./MyFinances_Schema_Room_Casa_v2.md) - Tabelle correnti, modello di chiusura, invarianti, movimenti e requisiti di preservazione storica.
- [Flussi UX, stati ed edge case - v1](./MyFinances_Flussi_UX_Edge_Case_v1.md) - Responsabilita' delle schermate, flussi operativi, chiusura mese, casi limite e piano per completare Casa.

## Decisioni aggiunte nell'ultimo aggiornamento

- `Disponibile` e' liquidita' Casa non vincolata, non denaro che deve necessariamente essere allocato.
- La chiusura distingue sempre saldo calcolato e saldo finale confermato dall'utente.
- Il saldo confermato resta sempre correggibile manualmente anche dopo l'introduzione dei movimenti.
- Le discrepanze non vengono nascoste: si conserva una rettifica di chiusura e, opzionalmente, una nota.
- Ogni residuo confermato deve essere distribuito completamente tra stessa categoria, altre categorie e/o Disponibile del mese successivo.
- Gli split sono consentiti, ma la somma delle destinazioni deve essere esattamente uguale al residuo.
- I Fondi Casa separati sono esclusi per ora: non e' stata identificata una semantica sufficientemente diversa da una normale categoria.
- Gli opening del mese successivo derivano dalle distribuzioni della chiusura precedente e restano modificabili.
- Dopo chiusura + autocompletamento si implementano navigazione mesi, movimenti categorie/Disponibile, movimenti posizioni e infine le rifiniture Personalizzazione.
- `Analisi & Suggerimenti` e' ora un requisito mandatorio futuro, non una feature opzionale.
- Prima di considerare definitivamente chiusa Casa, verra' eseguito un audit dello schema e di tutti i flussi per verificare che vengano conservati i dati grezzi necessari a indicatori e suggerimenti futuri.
- Principio per l'analisi: dato grezzo -> indicatore derivato -> suggerimento. Allocazioni, movimenti, rettifiche e distribuzioni non devono essere sovrascritti o persi se possono avere valore storico.

## Regola di manutenzione

Quando cambiamo una decisione importante:

1. aggiorniamo la specifica interessata;
2. aggiorniamo Flussi/Edge Case se cambia il comportamento utente;
3. verifichiamo che lo schema dati resti coerente;
4. implementiamo il codice;
5. rigeneriamo la copia PDF quando serve.

La documentazione deve descrivere lo stato deciso del progetto, non soltanto il codice gia implementato.

## Versioni precedenti

Le vecchie versioni restano recuperabili dalla cronologia Git.
