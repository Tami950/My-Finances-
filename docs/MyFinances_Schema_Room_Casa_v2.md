# MyFinances - Schema Room Casa v2

Data: 6 settembre 2026
Stato: schema logico corrente della Pianificazione Casa - Room v9

## 1. Principi
- Il denaro usa `Long` in centesimi.
- Le categorie sono concetti logici persistenti; i dati del singolo mese sono snapshot separati.
- I money account sono posizioni fisiche, non scopi logici.
- `Disponibile` non e' una categoria fittizia.
- Le spese fisse (`FIXED_EXPENSE`) sono obblighi pianificati, non normali contenitori di saldo.
- Il piano mensile distingue nuove risorse, opening BUDGET, prefunding FIXED_EXPENSE, Disponibile e pendenti.
- I dati grezzi utili alla futura Analisi & Suggerimenti devono restare ricostruibili.

## 2. Tabelle correnti

### 2.1 money_accounts
Campi:
- id: Long PK autoGenerate
- name: String
- type: MoneyAccountType (`CASH`, `BANK_ACCOUNT`, `CARD`, `OTHER`)
- sortOrder: Int
- isArchived: Boolean
- createdAt: Long
- updatedAt: Long

### 2.2 house_categories
Campi:
- id: Long PK autoGenerate
- name: String, unico case-insensitive
- type: HouseCategoryType (`FLEXIBLE`, `TARGET`)
- targetCents: Long?
- behavior: HouseCategoryBehavior (`BUDGET`, `FIXED_EXPENSE`), default `BUDGET`
- fixedExpenseDefaultCents: Long?
- sortOrder: Int
- isArchived: Boolean
- createdAt: Long
- updatedAt: Long

Regole:
- le categorie esistenti e nuove nascono `BUDGET` salvo scelta esplicita dell'utente;
- `BUDGET/TARGET` richiede targetCents > 0;
- `FIXED_EXPENSE` non usa semanticamente FLEXIBLE/TARGET e richiede un importo abituale > 0;
- `fixedExpenseDefaultCents` e' il valore proposto ai nuovi mesi, non la fonte di verita' dello storico.

### 2.3 house_months
Campi:
- id: Long PK autoGenerate
- year: Int
- month: Int
- totalResourcesCents: Long
- openingAvailableCents: Long NOT NULL DEFAULT 0
- note: String?
- status: HouseMonthStatus (`OPEN`, `CLOSED`)
- closedAt: Long?
- createdAt: Long
- updatedAt: Long

Vincolo unico: `(year, month)`.

`totalResourcesCents` rappresenta solo le nuove risorse del mese. `openingAvailableCents` resta separato e nel primo mese senza precedente vale automaticamente 0.

### 2.4 house_monthly_allocations
Campi:
- id: Long PK autoGenerate
- houseMonthId: Long FK -> house_months CASCADE
- categoryId: Long FK -> house_categories NO_ACTION
- categoryBehavior: HouseCategoryBehavior, snapshot mensile
- fixedExpensePaymentStatus: FixedExpensePaymentStatus? (`PLANNED`, `PAID`)
- fixedExpensePlannedCents: Long?
- fixedExpensePrefundedCents: Long NOT NULL DEFAULT 0
- openingBalanceCents: Long
- allocatedCents: Long
- createdAt: Long
- updatedAt: Long

Vincolo unico: `(houseMonthId, categoryId)`.

Per `BUDGET`:
```text
categoryTotal = openingBalanceCents + allocatedCents
```

Per `FIXED_EXPENSE`:
```text
fixedExpensePlannedCents   = importo previsto complessivo
fixedExpensePrefundedCents = quota gia' finanziata da mesi precedenti
allocatedCents             = nuove risorse necessarie nel mese
openingBalanceCents        = 0

0 <= prefunded <= planned
allocated = planned - prefunded
```

Lo stato `PAID/PLANNED` non modifica la pianificazione: indica solo se l'obbligo e' stato materialmente eseguito.

### 2.5 house_month_account_balances
Snapshot delle posizioni fisiche del denaro.

Campi principali:
- houseMonthId FK CASCADE
- moneyAccountId FK NO_ACTION
- amountCents
- timestamps

Vincolo unico: `(houseMonthId, moneyAccountId)`.

Il totale posizionato non puo' superare i Fondi Casa complessivi.

### 2.6 house_month_closings
Una riga per mese chiuso.

Conserva:
- calculatedAvailableCents
- confirmedAvailableCents
- availableAdjustmentCents
- availableAdjustmentNote
- unreconciledFixedExpenseDeficitCents
- timestamps

Le discrepanze non riscrivono retroattivamente la storia.

### 2.7 house_month_category_closings
Una riga per categoria del mese chiuso.

Conserva:
- calculatedBalanceCents
- confirmedBalanceCents
- adjustmentCents
- adjustmentNote
- categoryBehavior snapshot
- fixedExpenseClosingAction (`MARK_PAID`, `KEEP_PENDING`, `CANCELLED`)?
- fixedExpensePendingNote?
- timestamps

Per BUDGET il saldo e' un residuo distribuibile. Per FIXED_EXPENSE il valore rappresenta previsto/reale e la sola differenza viene riconciliata.

### 2.8 house_month_closing_transfers
Trasferimenti provenienti da categorie.

Campi:
- houseMonthId
- sourceCategoryId
- destinationType (`CATEGORY`, `AVAILABLE`)
- destinationCategoryId?
- amountCents
- createdAt

Per una categoria BUDGET attiva la quota mantenuta in sorgente e' calcolata automaticamente come:
```text
confirmedBalance - somma destinazioni esplicite
```

### 2.9 house_month_available_closing_transfers
Trasferimenti espliciti dal Disponibile finale verso categorie.

Regola:
```text
keptAvailable = confirmedAvailable - somma transfer dal Disponibile
```

La quota non trasferita resta Disponibile senza una riga ridondante AVAILABLE->AVAILABLE.

### 2.10 house_fixed_expense_pendings
Obblighi fissi gia' finanziati ma ancora da pagare al termine del mese di origine.

Campi:
- id
- sourceHouseMonthId FK CASCADE
- categoryId FK NO_ACTION
- amountCents
- note?
- status (`PENDING`, `PAID`)
- resolvedAt?
- createdAt
- updatedAt

Un pendente NON e':
- opening della categoria;
- nuova allocazione;
- Disponibile.

Finche' e' PENDING concorre pero' ai fondi fisicamente presenti. Quando viene segnato pagato smette di concorrere ai Fondi Casa complessivi senza una seconda sottrazione dal budget corrente.

## 3. Risorse abituali in DataStore
`AppPreferencesRepository` conserva anche:
- `usualHouseMonthlyResourcesCents`.

Serve solo come default per `house_months.totalResourcesCents` quando si crea un nuovo mese. Il valore mensile resta modificabile e separato dal Disponibile ereditato.

## 4. Disponibile Casa
Prima dei movimenti:
```text
availableCents = openingAvailableCents + totalResourcesCents - somma allocatedCents
```

Il prefunding delle spese fisse non aumenta il Disponibile: e' denaro gia' vincolato.

In chiusura:
- il Disponibile resta interamente Disponibile per default;
- puo' essere trasferito a categorie;
- se una FIXED_EXPENSE reale supera il previsto, l'extra consuma automaticamente il Disponibile;
- se il Disponibile non basta, viene salvato `unreconciledFixedExpenseDeficitCents`, mostrato un warning e la chiusura richiede conferma ma non viene bloccata definitivamente.

## 5. Fondi Casa complessivi
Formula corrente prima dei movimenti:
```text
Fondi Casa complessivi
=
nuove risorse
+ Disponibile ereditato
+ opening categorie BUDGET
+ prefunding spese fisse
+ spese fisse pendenti ancora finanziate
```

Questa e' la base per validare le posizioni fisiche.

## 6. Carryover al mese successivo
Per una destinazione `BUDGET`:
```text
opening(X) = somma transfer di categorie verso X
           + somma transfer dal Disponibile verso X
```

Per una destinazione `FIXED_EXPENSE` gli stessi transfer NON diventano opening:
```text
prefunded(X) = somma transfer di categorie verso X
             + somma transfer dal Disponibile verso X
```

Il limite globale di prefunding e':
```text
incoming totale alla FIXED_EXPENSE <= fixedExpenseDefaultCents
```

La verifica somma tutte le sorgenti del wizard, non il singolo campo.

Disponibile ereditato:
```text
confirmedAvailable
- transfer dal Disponibile verso categorie
+ transfer di categorie verso AVAILABLE
```

## 7. Modifica dell'importo abituale di una FIXED_EXPENSE
Il valore globale puo' valere:
- dal prossimo mese;
- anche per il mese OPEN corrente.

Se applicato anche al mese corrente, il riallineamento e' esplicito e transazionale.

Aumento:
- sorgente Disponibile con fondi sufficienti; oppure
- una categoria BUDGET con fondi sufficienti.

Diminuzione:
- destinazione Disponibile; oppure
- categoria BUDGET.

La provenienza viene preservata:
- quota presa da allocated BUDGET -> allocated FIXED;
- quota presa da opening BUDGET -> prefunding FIXED;
- quota fixed liberata da allocated -> torna a nuove risorse/Disponibile o allocated BUDGET;
- quota fixed liberata da prefunding -> openingAvailable o opening BUDGET.

Conversione BUDGET -> FIXED_EXPENSE nel mese OPEN:
```text
old opening   -> fixedExpensePrefundedCents
old allocated -> allocatedCents
old total     -> fixedExpensePlannedCents
opening       -> 0
```

I mesi CLOSED non vengono reinterpretati.

## 8. Chiusura atomica
Ordine logico:
1. verificare mese OPEN e assenza di closing precedente;
2. ricalcolare saldi dal repository;
3. validare saldi confermati;
4. validare trasferimenti dal Disponibile;
5. validare residui BUDGET;
6. validare surplus/deficit FIXED_EXPENSE;
7. validare globalmente i limiti di prefunding delle FIXED_EXPENSE destinazione;
8. persistere closing, category closings, transfer, available transfer e pendenti;
9. valorizzare `status=CLOSED` e `closedAt` nella stessa transazione.

Se una validazione fallisce, il mese resta OPEN.

## 9. Invarianti principali
- importi monetari in Long cents;
- saldi confermati non negativi;
- nuove allocazioni complessive <= nuove risorse;
- `prefunded <= planned` per FIXED_EXPENSE;
- `openingBalanceCents = 0` per FIXED_EXPENSE;
- le spese fisse non usano FLEXIBLE/TARGET come comportamento operativo;
- BUDGET e FIXED_EXPENSE hanno carryover semanticamente distinti;
- i pendenti non diventano Disponibile;
- le posizioni fisiche <= Fondi Casa complessivi;
- un CLOSED non viene modificato dai flussi ordinari;
- adjustment = confermato - calcolato;
- nessun trasferimento puo' inventare denaro.

## 10. Preservazione dati per Analisi & Suggerimenti
Conservare come dati grezzi:
- comportamento categoria per mese;
- importo abituale globale e importo previsto mensile;
- prefunding;
- allocazioni e opening;
- stato pagato/non pagato;
- saldo calcolato e reale;
- rettifiche e note;
- routing della chiusura;
- pendenti e tempi di risoluzione;
- deficit non riconciliati;
- futuri movimenti logici e fisici.

Principio:
```text
dato grezzo -> indicatore derivato -> suggerimento
```

Dopo il completamento di Casa resta obbligatorio un audit dello schema prima di considerarlo stabile per Analisi & Suggerimenti.

## 11. Migrazioni
Database Room corrente: **versione 9**.

- v6: opening Available + closings categoria/Disponibile + routing categoria;
- v7: trasferimenti dal Disponibile verso categorie;
- v8: behavior BUDGET/FIXED_EXPENSE, stato pagamento, riconciliazione fixed e pendenti;
- v9: importo abituale fixed, planned mensile e prefunding.

Migrazioni registrate:
- `MIGRATION_5_6`;
- `MIGRATION_6_7`;
- `MIGRATION_7_8`;
- `MIGRATION_8_9`.

`MIGRATION_8_9` preserva l'eventuale vecchio opening di una FIXED_EXPENSE trasformandolo in prefunding e prova a ricavare l'importo abituale dall'ultimo piano fixed disponibile.

Il fallback distruttivo e `exportSchema=false` restano temporanei per sviluppo. Prima dell'uso reale vanno rimossi/corretti e le migrazioni vanno testate esplicitamente.

## 12. Nota su ID e cloud futuro
Lo schema usa attualmente PK Long autoGenerate. Prima della sincronizzazione multi-device va scelta esplicitamente una strategia UUID globale oppure Long locale + syncId globale.
