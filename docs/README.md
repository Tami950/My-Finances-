# MyFinances - Documentazione

Questa cartella contiene la documentazione viva del progetto. Le specifiche vengono aggiornate quando cambia una decisione funzionale, di dominio o architetturale.

I file Markdown sono la sorgente canonica dei documenti principali. Le versioni PDF vengono rigenerate quando serve una copia da leggere/condividere.

## Documenti correnti

- [Specifiche generali - v4](./MyFinances_Specifiche_Progetto_v4.md) - Obiettivi, sezioni, regole di dominio, Casa/Pianificazione, Dashboard, Analisi futura, architettura e roadmap.
- [Schema Room Casa - v2](./MyFinances_Schema_Room_Casa_v2.md) - Tabelle correnti, modello di chiusura, invarianti, movimenti e requisiti di preservazione storica.
- [Flussi UX, stati ed edge case - v1](./MyFinances_Flussi_UX_Edge_Case_v1.md) - Responsabilita' delle schermate, flussi operativi, chiusura mese, casi limite e piano per completare Casa.
- [Spese fisse Casa - v1](./MyFinances_Spese_Fisse_Casa_v1.md) - Comportamento FIXED_EXPENSE, stato pagata/non pagata, riconciliazione in chiusura, deficit/surplus e spese pendenti.

## Decisioni aggiunte nell'ultimo aggiornamento

- `Disponibile` e' liquidita' Casa non vincolata, non denaro che deve necessariamente essere allocato.
- La chiusura distingue sempre saldo calcolato e saldo finale confermato dall'utente.
- Il saldo confermato resta sempre correggibile manualmente anche dopo l'introduzione dei movimenti.
- Le discrepanze non vengono nascoste: si conserva una rettifica di chiusura e, opzionalmente, una nota.
- Ogni residuo confermato di una categoria BUDGET deve essere distribuito completamente tra stessa categoria, altre categorie e/o Disponibile del mese successivo.
- Il valore `Mantieni` e' derivato automaticamente: se cambia il saldo reale o se vengono inserite altre destinazioni, l'app ricalcola il residuo senza richiedere conti manuali.
- Il Disponibile puo' restare libero oppure essere riallocato verso categorie; la parte non spostata resta Disponibile per default.
- Le categorie possono avere comportamento `BUDGET` o `FIXED_EXPENSE`. In UI `FIXED_EXPENSE` viene esposto con la checkbox `Spesa fissa`; default false/BUDGET per categorie esistenti e nuove.
- Una spesa fissa e' denaro impegnato nella pianificazione e non richiede movimenti ordinari. Puo' essere segnata Pagata o Da pagare.
- In chiusura una spesa fissa confronta importo pianificato e reale. Un importo liberato va al Disponibile per default ma puo' essere riallocato; un extra riduce automaticamente il Disponibile.
- Se l'extra di una spesa fissa supera il Disponibile, la chiusura non viene bloccata definitivamente: viene mostrato un warning e la parte non coperta viene conservata come discrepanza esplicita.
- Una spesa fissa ancora non pagata deve essere riconciliata come pagata, ancora pendente oppure non piu' dovuta.
- Una spesa pendente passa al mese successivo come obbligo gia' finanziato, con categoria, mese di origine e nota opzionale. Non diventa opening e non viene riallocata di nuovo.
- Le spese fisse restano concettualmente separate dalla sezione Bollette: Bollette riguarda scadenze/promemoria, Casa riguarda il comportamento finanziario nella pianificazione.
- I Fondi Casa separati sono esclusi per ora: non e' stata identificata una semantica sufficientemente diversa da una normale categoria.
- Gli opening del mese successivo derivano dalle distribuzioni della chiusura precedente e restano modificabili.
- Dopo chiusura + autocompletamento si implementano navigazione mesi, movimenti categorie/Disponibile, movimenti posizioni e infine le rifiniture Personalizzazione.
- `Analisi & Suggerimenti` e' ora un requisito mandatorio futuro, non una feature opzionale.
- Prima di considerare definitivamente chiusa Casa, verra' eseguito un audit dello schema e di tutti i flussi per verificare che vengano conservati i dati grezzi necessari a indicatori e suggerimenti futuri.
- Principio per l'analisi: dato grezzo -> indicatore derivato -> suggerimento. Allocazioni, movimenti, rettifiche, stato delle spese fisse, pendenti e distribuzioni non devono essere sovrascritti o persi se possono avere valore storico.

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
