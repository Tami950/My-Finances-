# MyFinances - Documentazione

Questa cartella contiene la documentazione viva del progetto. Le specifiche vengono aggiornate quando cambia una decisione funzionale, di dominio o architetturale.

I file Markdown sono la sorgente canonica dei documenti principali. Le versioni PDF vengono rigenerate quando serve una copia da leggere/condividere.

## Documenti correnti

- [Specifiche generali - v4](./MyFinances_Specifiche_Progetto_v4.md) - Obiettivi, sezioni, regole di dominio, Casa/Pianificazione, Dashboard, Analisi futura, architettura e roadmap.
- [Schema Room Casa - v2](./MyFinances_Schema_Room_Casa_v2.md) - Schema Room v9, chiusura, prefunding, pendenti, carryover e invarianti.
- [Flussi UX, stati ed edge case - v1](./MyFinances_Flussi_UX_Edge_Case_v1.md) - Responsabilita' delle schermate, flussi operativi, chiusura mese, casi limite e piano per completare Casa.
- [Spese fisse Casa - v1](./MyFinances_Spese_Fisse_Casa_v1.md) - Specifica canonica della feature FIXED_EXPENSE: importi abituali, prefunding, riconciliazione, pendenti e regole UX dei bottom sheet.

## Decisioni correnti rilevanti

- `Disponibile` e' liquidita' Casa non vincolata e non una categoria fittizia.
- La chiusura conserva sempre saldo calcolato, saldo reale confermato e relative rettifiche senza riscrivere la storia.
- Per una categoria `BUDGET`, se non vengono indicate destinazioni esplicite tutto il residuo resta nella stessa categoria; `Mantieni` e' derivato automaticamente dal saldo reale e dalle destinazioni inserite.
- Il Disponibile resta Disponibile per default e puo' essere redistribuito. Nel suo bottom sheet `Mantieni Disponibile` e' mostrato subito dopo il saldo reale e si aggiorna live.
- Le categorie usano `HouseCategoryBehavior.BUDGET` o `FIXED_EXPENSE`; in UI la scelta e' una semplice checkbox `Spesa fissa`, disattivata per default.
- Una `FIXED_EXPENSE` non usa Libera/Obiettivo: possiede un importo mensile abituale, proposto automaticamente nei nuovi mesi e sempre modificabile nel singolo mese.
- Il planner puo' usare `Nuove risorse Casa abituali` salvate in Personalizzazione. Il Disponibile ereditato resta separato e nel primo mese senza precedente parte da 0.
- Una spesa fissa distingue `planned`, `prefunded` e quota da nuove risorse. Il prefunding non e' opening e non puo' superare l'importo previsto.
- In chiusura una FIXED_EXPENSE puo' ricevere denaro come prefunding del mese successivo. Il limite viene controllato sulla somma di tutte le sorgenti del wizard.
- Cambiando l'importo di una spesa fissa nel planner, il flag `Usa questo importo come nuovo valore abituale` e' opt-in e richiede conferma prima di modificare il default globale.
- Cambiando l'importo abituale da Personalizzazione, l'utente sceglie se applicarlo dal prossimo mese o anche al mese OPEN corrente. Il riallineamento corrente richiede una sorgente/destinazione esplicita tra Disponibile e categorie BUDGET valide.
- Una spesa fissa puo' essere `Pagata` o `Da pagare`; lo stato operativo non modifica il budget gia' vincolato.
- In chiusura una spesa fissa non pagata deve essere risolta come pagata, ancora pendente o non piu' dovuta. Una pendente passa al mese successivo come obbligo gia' finanziato, non come opening o Disponibile.
- Un deficit fixed consuma automaticamente il Disponibile. Se non basta, la parte scoperta viene conservata come discrepanza e richiede conferma, senza bloccare definitivamente la chiusura.
- I Fondi Casa complessivi includono nuove risorse, Disponibile ereditato, opening BUDGET, prefunding FIXED_EXPENSE e pendenti ancora finanziate.
- I bottom sheet dell'app non si chiudono con tap sullo sfondo o swipe: hanno una X esplicita. X annulla il draft del foglio; Fatto/Salva/Conferma applica solo modifiche valide.
- Le spese fisse restano separate dalla sezione Bollette: Bollette riguarda scadenze/promemoria, Casa il comportamento finanziario nella pianificazione.
- I Fondi Casa separati sono esclusi per ora.
- Dopo chiusura + autocompletamento si implementano navigazione mesi, movimenti categorie/Disponibile, movimenti posizioni e poi le rifiniture Personalizzazione.
- `Analisi & Suggerimenti` e' un requisito mandatorio futuro. Prima di considerare Casa stabile verra' eseguito un audit dello schema e dei dati grezzi preservati.

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
