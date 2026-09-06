# MyFinances - Schema Room Casa v2

Data: 6 settembre 2026
Stato: schema logico corrente della Pianificazione Casa

## 1. Principi
- Il denaro usa Long in centesimi.
- Le categorie sono concetti logici persistenti.
- I money account sono contenitori fisici generali.
- Il piano mensile collega nuove risorse, residui iniziali, Disponibile e posizioni fisiche.
- Lo stato di chiusura appartiene al mese.
- La posizione fisica e' uno snapshot corrente, non uno storico movimenti.
- I dati grezzi utili alla futura Analisi & Suggerimenti non devono essere persi o sovrascritti.

## 2. Tabelle correnti

### 2.1 money_accounts
Campi:
- id: Long PK autoGenerate
- name: String
- type: MoneyAccountType
- sortOrder: Int
- isArchived: Boolean
- createdAt: Long
- updatedAt: Long

MoneyAccountType:
- CASH
- BANK_ACCOUNT
- CARD
- OTHER

### 2.2 house_categories
Campi correnti:
- id: Long PK autoGenerate
- name: String
- type: HouseCategoryType
- targetCents: Long?
- sortOrder: Int
- isArchived: Boolean
- createdAt: Long
- updatedAt: Long

HouseCategoryType:
- FLEXIBLE
- TARGET

Vincoli:
- name non vuoto;
- name unico case-insensitive;
- TARGET richiede targetCents > 0;
- FLEXIBLE usa targetCents = null.

### 2.3 house_months
Campi correnti:
- id: Long PK autoGenerate
- year: Int
- month: Int
- totalResourcesCents: Long
- openingAvailableCents: Long NOT NULL DEFAULT 0
- note: String?
- status: HouseMonthStatus
- closedAt: Long?
- createdAt: Long
- updatedAt: Long

HouseMonthStatus:
- OPEN
- CLOSED

Vincolo unico:
- (year, month)

Regole:
- un solo mese per coppia anno/mese;
- un nuovo mese nasce OPEN;
- CLOSED implica mese storico e non modificabile dai flussi ordinari;
- non creare il mese successivo se il precedente esiste ed e' OPEN;
- openingAvailableCents rappresenta Disponibile ereditato dal mese precedente, separato dalle nuove risorse.

### 2.4 house_monthly_allocations
Campi:
- id: Long PK autoGenerate
- houseMonthId: Long FK -> house_months.id CASCADE
- categoryId: Long FK -> house_categories.id NO_ACTION
- openingBalanceCents: Long
- allocatedCents: Long
- createdAt: Long
- updatedAt: Long

Vincolo unico:
- (houseMonthId, categoryId)

Derivato iniziale:
- categoryTotalCents = openingBalanceCents + allocatedCents.

### 2.5 house_month_account_balances
Campi:
- id: Long PK autoGenerate
- houseMonthId: Long FK -> house_months.id CASCADE
- moneyAccountId: Long FK -> money_accounts.id NO_ACTION
- amountCents: Long
- createdAt: Long
- updatedAt: Long

Vincolo unico:
- (houseMonthId, moneyAccountId).

Regole:
- amountCents >= 0;
- una posizione a zero puo' non avere riga persistita;
- somma posizioni <= fondi Casa complessivi.

Fondi Casa complessivi prima dei movimenti:
- totalHouseFundsCents = totalResourcesCents + openingAvailableCents + somma openingBalanceCents delle categorie.

Le posizioni descrivono dove si trova fisicamente tutto il denaro Casa, inclusi i residui ereditati.

### 2.6 house_month_closings
Tabella corrente, una riga per mese chiuso.

Campi:
- id: Long PK autoGenerate
- houseMonthId: Long FK -> house_months CASCADE
- calculatedAvailableCents: Long
- confirmedAvailableCents: Long
- availableAdjustmentCents: Long
- availableAdjustmentNote: String?
- createdAt: Long
- updatedAt: Long

Vincolo unico:
- houseMonthId.

Regole:
- conserva sia il Disponibile calcolato sia quello reale confermato;
- availableAdjustmentCents = confirmedAvailableCents - calculatedAvailableCents;
- la rettifica non modifica retroattivamente allocazioni o movimenti.

### 2.7 house_month_category_closings
Tabella corrente, una riga per categoria presente nel mese chiuso.

Campi:
- id: Long PK autoGenerate
- houseMonthId: Long FK -> house_months CASCADE
- categoryId: Long FK -> house_categories NO_ACTION
- calculatedBalanceCents: Long
- confirmedBalanceCents: Long
- adjustmentCents: Long
- adjustmentNote: String?
- createdAt: Long
- updatedAt: Long

Vincolo unico:
- (houseMonthId, categoryId).

Regole:
- confirmedBalanceCents >= 0;
- adjustmentCents = confirmedBalanceCents - calculatedBalanceCents;
- non riscrivere la storia per nascondere discrepanze;
- nella versione corrente senza movimenti, calculatedBalanceCents = openingBalanceCents + allocatedCents.

Quando arriveranno i movimenti, il saldo calcolato sara' derivato da opening + allocazioni + entrate - uscite +/- rettifiche.

### 2.8 house_month_closing_transfers
Tabella corrente che conserva la destinazione del residuo confermato di ogni categoria.

Campi:
- id: Long PK autoGenerate
- houseMonthId: Long FK -> house_months CASCADE
- sourceCategoryId: Long FK -> house_categories NO_ACTION
- destinationType: HouseClosingDestinationType
- destinationCategoryId: Long? FK -> house_categories NO_ACTION
- amountCents: Long
- createdAt: Long

HouseClosingDestinationType:
- CATEGORY
- AVAILABLE

Regole:
- gli importi persistiti sono positivi;
- CATEGORY richiede destinationCategoryId != null;
- AVAILABLE richiede destinationCategoryId = null;
- per ogni sourceCategoryId, somma transfer.amountCents = confirmedBalanceCents della relativa chiusura;
- stessa sorgente e destinazione = "Mantieni";
- split tra piu' destinazioni consentito;
- nessun Fondo Casa separato nella versione corrente.

La provenienza viene conservata per storico e futura Analisi & Suggerimenti.

## 3. Disponibile Casa
`Disponibile` e' liquidita' Casa non vincolata ad alcuna categoria. Non e' una categoria fittizia e non deve obbligatoriamente essere allocato.

Prima dei movimenti:
- availableCents = openingAvailableCents + totalResourcesCents - somma allocatedCents.

Con i movimenti:
- availableCurrentCents = openingAvailableCents + nuove risorse - allocazioni - uscite dal Disponibile + entrate/rettifiche sul Disponibile.

Il Disponibile finale confermato in chiusura viene riportato al mese successivo insieme agli eventuali transfer con destinationType=AVAILABLE.

## 4. Stato mese e chiusura
HouseMonthStatus e' persistito direttamente in house_months.

Regole implementate:
- createPlan crea OPEN;
- updatePlan e updatePositions rifiutano CLOSED;
- createPlan controlla il mese precedente e rifiuta se OPEN;
- closeMonth salva tutti i dati di chiusura e porta OPEN -> CLOSED nella stessa transazione;
- closedAt viene valorizzato solo al completamento della transazione.

Se una validazione fallisce, la transazione viene annullata e il mese resta OPEN.

## 5. Autocompletamento del mese successivo
Per una categoria destinazione X:
- suggestedOpening(X) = somma amountCents dei transfer del precedente CLOSED con destinationType=CATEGORY e destinationCategoryId=X.

Se nessun transfer -> 0.

Il valore suggerito resta modificabile.

Per il Disponibile:
- inheritedAvailable = confirmedAvailableCents della chiusura precedente + somma transfer con destinationType=AVAILABLE.

Questo valore inizializza openingAvailableCents del nuovo mese e resta modificabile per riconciliare eventuali discrepanze reali.

Non esiste doppio conteggio:
- opening categoria appartiene al saldo iniziale della relativa categoria;
- openingAvailableCents appartiene al Disponibile;
- totalResourcesCents contiene soltanto le nuove risorse del mese.

## 6. Categoria nascosta dal mese
Requisito futuro: una categoria globale puo' essere nascosta/esclusa da uno specifico mese senza archiviarla globalmente.

Preferenza preliminare: mantenere una riga mensile esplicita e distinguere zero da nascosta tramite attributo/configurazione mensile.

## 7. Movimenti categoria e Disponibile - prossima estensione
Serve una struttura append-only per tracciare:
- uscita;
- entrata;
- rettifica.

La categoria deve poter essere opzionale:
- categoryId valorizzato -> movimento della categoria;
- categoryId null -> movimento del Disponibile.

Campi candidati:
- id
- houseMonthId
- categoryId?
- type
- amountCents
- movementDate
- note
- createdAt
- updatedAt.

Questa struttura deve permettere di derivare saldi correnti e saldo calcolato di chiusura senza usare aggregati ridondanti come fonte primaria.

## 8. Movimenti tra posizioni - estensione futura
Lo snapshot house_month_account_balances descrive dove si trova il denaro ora.

Per lo storico servira' una tabella di movimenti con:
- fromMoneyAccountId;
- toMoneyAccountId;
- amountCents;
- movementDate;
- note;
- createdAt.

Trasferimento atomico: decremento sorgente + incremento destinazione, totale Casa invariato.

## 9. Delete e storico
Archivio e delete sono separati.

Hard delete futura:
- vietata se esiste denaro non riallocato;
- non deve invalidare record storici;
- prima del codice va scelta una strategia tra soft-delete definitivo, snapshot storico o altra soluzione coerente.

Le tabelle di chiusura aumentano l'importanza della decisione: categoryId puo' essere referenziato da allocazioni, closings e transfers.

## 10. Transazioni
Devono essere atomiche:
- creazione piano;
- modifica pianificazione;
- modifica posizioni;
- chiusura mese;
- futuri trasferimenti tra posizioni.

Chiusura mese atomica:
1. validare mese OPEN;
2. ricalcolare i saldi calcolati dal repository;
3. validare i saldi confermati;
4. validare per ogni categoria somma transfer = confirmed balance;
5. insert house_month_closings;
6. insert house_month_category_closings;
7. insert house_month_closing_transfers;
8. update house_months status=CLOSED, closedAt=now, updatedAt=now.

## 11. Invarianti di dominio
- denaro mai negativo nei saldi confermati;
- allocato nuovo mai oltre totalResourcesCents;
- posizionato mai oltre totalHouseFundsCents;
- opening separato dalle nuove risorse;
- Disponibile ereditato separato dalle nuove risorse;
- un mese CLOSED non viene modificato dai flussi ordinari;
- mese successivo richiede precedente CLOSED quando esiste;
- per ogni categoria chiusa: distribuito = confermato;
- adjustment = confermato - calcolato;
- transfer AVAILABLE non crea opening categoria;
- transfer CATEGORY concorre all'opening suggerito del mese successivo;
- i movimenti tra posizioni conservano il totale.

## 12. Preservazione dati per Analisi & Suggerimenti
Requisito mandatorio di progetto: dopo il completamento di Casa va eseguito un audit dello schema prima di congelare il dominio.

Dati che non devono essere persi:
- allocazioni mensili;
- opening;
- movimenti individuali;
- saldo calcolato in chiusura;
- saldo confermato;
- adjustment e nota;
- trasferimenti di chiusura sorgente -> destinazione;
- timestamp di chiusura;
- movimenti fisici tra posizioni.

Principio:
- dato grezzo -> indicatore -> suggerimento.

Gli indicatori e suggerimenti saranno derivati. Evitare di usare come fonte di verita' aggregati o insight salvati quando possono essere ricalcolati dai dati primari.

L'audit post-Casa dovra' verificare se servono:
- snapshot storici aggiuntivi;
- tabelle evento;
- metadati sulle correzioni;
- indici/query per analisi longitudinali;
- eventuali versioni/snapshot delle definizioni categoria.

## 13. Migrazioni
Database Room corrente: versione 6.

La versione 6 introduce:
- house_months.openingAvailableCents NOT NULL DEFAULT 0;
- house_month_closings;
- house_month_category_closings;
- house_month_closing_transfers;
- converter per HouseClosingDestinationType.

E' presente una migrazione esplicita MIGRATION_5_6 che preserva i dati esistenti della versione 5.

DatabaseModule registra MIGRATION_5_6 prima del fallback distruttivo. Il fallback resta temporaneamente disponibile soltanto per vecchi schemi di sviluppo non coperti da migrazioni.

Prima dell'uso reale:
- rimuovere fallback distruttivo;
- abilitare exportSchema;
- mantenere migrazioni Room esplicite e testate.

## 14. Nota su ID e cloud futuro
Lo schema usa attualmente PK Long autoGenerate.

Prima della sincronizzazione multi-device va presa una decisione esplicita:
- UUID/String globale;
- oppure Long locale + syncId globale.
