# MyFinances - Documentazione

Questa cartella contiene la documentazione viva del progetto. Le specifiche vengono aggiornate quando cambia una decisione funzionale, di dominio o architetturale.

Da questa versione i file Markdown sono la sorgente canonica dei documenti principali. Le versioni PDF vengono rigenerate da queste sorgenti quando serve una copia da leggere/condividere.

## Documenti correnti

- [Specifiche generali - v4](./MyFinances_Specifiche_Progetto_v4.md) - Obiettivi, sezioni, regole di dominio, Casa/Pianificazione, Dashboard, architettura e roadmap.
- [Schema Room Casa - v2](./MyFinances_Schema_Room_Casa_v2.md) - Tabelle correnti, invarianti e prossime estensioni necessarie a stato mese, chiusura, movimenti e storico.
- [Flussi UX, stati ed edge case - v1](./MyFinances_Flussi_UX_Edge_Case_v1.md) - Responsabilita' delle schermate, flussi operativi, casi limite e piano ordinato per chiudere Pianificazione v1.

## Decisioni aggiunte nell'ultimo aggiornamento

- Casa atterra sempre su Pianificazione quando viene selezionata dalla bottom navigation.
- `house_months` deve avere stato `OPEN/CLOSED` e `closedAt`.
- Un mese successivo non viene pianificato se il precedente esiste ed e' ancora aperto.
- L'opening balance del nuovo mese deriva dal residuo finale del precedente chiuso, con fallback a zero e possibilita' di correzione manuale.
- Allocazioni e posizioni non possono superare le risorse del mese.
- Le posizioni rappresentano dove si trova il denaro adesso; i movimenti saranno modellati separatamente.
- Pianificazione diventa la schermata operativa Casa con card categoria, posizioni, modifica e chiusura.
- Una categoria puo' essere nascosta da uno specifico mese senza archiviarla/eliminarla globalmente.
- Delete futura con denaro residuo richiede prima riallocazione e deve preservare lo storico.
- `Disponibile da spendere` in Dashboard e' personale; `Da allocare` e' il residuo delle nuove risorse Casa non ancora assegnate.

## Regola di manutenzione

Quando cambiamo una decisione importante:

1. aggiorniamo la specifica interessata;
2. aggiorniamo Flussi/Edge Case se cambia il comportamento utente;
3. verifichiamo che lo schema dati resti coerente;
4. implementiamo il codice;
5. rigeneriamo la copia PDF quando serve.

La documentazione deve descrivere lo stato deciso del progetto, non soltanto il codice gia implementato.

## Versioni precedenti

Le vecchie versioni restano recuperabili dalla cronologia Git. I precedenti PDF v3/"Definitivo" sono stati rimossi dal branch corrente per evitare che documentazione superata sembri ancora valida.
