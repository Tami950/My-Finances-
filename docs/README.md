# MyFinances - Documentazione

Questa cartella contiene la documentazione viva del progetto. Le specifiche vengono aggiornate quando cambia una decisione funzionale, di dominio o architetturale.

Da questa versione i file Markdown sono la sorgente canonica dei documenti principali. Le versioni PDF vengono rigenerate da queste sorgenti quando serve una copia da leggere/condividere.

## Documenti correnti

- [Specifiche generali - v4](./MyFinances_Specifiche_Progetto_v4.md) - Obiettivi, sezioni, regole di dominio, Casa/Pianificazione, Dashboard, architettura e roadmap.
- [Schema Room Casa - v2](./MyFinances_Schema_Room_Casa_v2.md) - Tabelle correnti, invarianti e prossime estensioni necessarie a stato mese, chiusura, movimenti e storico.
- [Flussi UX, stati ed edge case - v1](./MyFinances_Flussi_UX_Edge_Case_v1.md) - Responsabilita' delle schermate, flussi operativi, casi limite e piano ordinato per chiudere Pianificazione v1.

## Decisioni aggiunte nell'ultimo aggiornamento

- Casa atterra sempre su Pianificazione quando viene selezionata dalla bottom navigation.
- `house_months` ha stato `OPEN/CLOSED` e `closedAt`.
- Un mese successivo non viene pianificato se il precedente esiste ed e' ancora aperto.
- L'opening balance del nuovo mese deriva dal residuo finale del precedente chiuso, con fallback a zero e possibilita' di correzione manuale.
- Allocazioni e posizioni non possono superare le risorse del mese.
- Le posizioni rappresentano dove si trova il denaro adesso; i movimenti saranno modellati separatamente.
- Pianificazione e' la schermata operativa Casa con card categoria, posizioni e flussi di modifica separati.
- Il flag persistente `isHouseSetupCompleted` indica solo che l'onboarding iniziale e' stato completato. La possibilita' di creare una pianificazione richiede comunque almeno una categoria attiva e una posizione attiva; se vengono tutte archiviate il setup non viene dimenticato, ma il planner viene bloccato finche' la configurazione non torna valida.
- Il residuo delle nuove risorse Casa non assegnate viene chiamato `Disponibile`, non `Da allocare`: puo' restare volutamente non allocato ed e' liquidita' Casa ancora libera.
- Le future spese Casa potranno essere registrate anche senza categoria, scalando direttamente il `Disponibile`; questo richiede un modello di movimento/spesa che distingua spese categorizzate e spese sul disponibile.
- Una categoria puo' essere nascosta da uno specifico mese senza archiviarla/eliminarla globalmente.
- Delete futura con denaro residuo richiede prima riallocazione e deve preservare lo storico.
- `Disponibile da spendere` in Dashboard resta invece un KPI personale: non va confuso con il `Disponibile` della pianificazione Casa.

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
