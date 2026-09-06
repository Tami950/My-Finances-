# MyFinances - Schema Room Casa v2

Data: 6 settembre 2026
Stato: schema logico corrente e prossime estensioni necessarie alla Pianificazione

## 1. Principi
- Il denaro usa Long in centesimi.
- Le categorie sono concetti logici persistenti.
- I money account sono contenitori fisici generali.
- Il piano mensile collega risorse, categorie, Disponibile e posizioni fisiche.
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
- non creare il mese successivo se il precedente esiste ed e' OPEN.

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
- somma amountCents <= totalResourcesCents;
- una posizione a zero puo' non avere riga persistita.

## 3. Disponibile Casa
`Disponibile` sostituisce semanticamente `Da allocare`.

Prima dei movimenti:
- availableCents = totalResourcesCents - somma allocatedCents.

Con i movimenti:
- availableCurrentCents = risorse - allocazioni - uscite dal Disponibile + entrate/rettifiche sul Disponibile.

Il Disponibile non e' una categoria fittizia.

Le somme destinate al Disponibile durante una chiusura devono essere preservate separatamente dagli opening delle categorie.

## 4. Stato mese
HouseMonthStatus e' persistito direttamente in house_months.

Regole implementate:
- createPlan crea OPEN;
- updatePlan e updatePositions rifiutano CLOSED;
- createPlan controlla il mese precedente e rifiuta se OPEN.

La chiusura deve valorizzare closedAt e portare OPEN -> CLOSED nella stessa transazione che salva i dati di chiusura.

## 5. Nuova struttura di chiusura mese
La chiusura deve conservare sia il risultato reale sia il confronto con quanto l'app aveva calcolato.

### 5.1 house_month_category_closings
Tabella prevista/da implementare nel prossimo schema.

Campi proposti:
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
- calculatedBalanceCents >= 0 nella prima versione senza overspending esplicito;
- confirmedBalanceCents >= 0;
- adjustmentCents = confirmedBalanceCents - calculatedBalanceCents;
- non riscrivere allocazioni o movimenti per nascondere la discrepanza.

Quando arriveranno i movimenti, il saldo calcolato sara' derivato da opening + allocazioni + entrate - uscite +/- rettifiche. Fino ad allora il calcolato iniziale coincide con opening + allocated e il confermato resta liberamente correggibile.

### 5.2 house_month_closing_transfers
Tabella prevista/da implementare nel prossimo schema.

Scopo: conservare come ogni residuo confermato viene destinato al mese successivo.

Campi proposti:
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
- amountCents > 0;
- CATEGORY richiede destinationCategoryId != null;
- AVAILABLE richiede destinationCategoryId = null;
- per ogni sourceCategoryId, somma transfer.amountCents = confirmedBalanceCents della relativa chiusura;
- la stessa categoria come sorgente/destinazione rappresenta "Mantieni";
- split tra piu' destinazioni e' consentito;
- nessun Fondo Casa separato nella prima versione.

Questa tabella conserva la provenienza storica del denaro e consente di derivare gli opening del mese successivo senza perdere informazione.

## 6. Autocompletamento del mese successivo
Per una categoria destinazione X:
- suggestedOpening(X) = somma amountCents dei closing transfers del precedente mese CLOSED con destinationType=CATEGORY e destinationCategoryId=X.

Se nessun transfer -> 0.

Il suggerimento resta modificabile.

Per il Disponibile:
- transferredAvailableCents = somma transfer del precedente CLOSED con destinationType=AVAILABLE.

Il modo in cui questa quota entra formalmente nel totale del nuovo mese deve rimanere coerente con la distinzione tra nuove risorse del mese e denaro ereditato dal precedente. Durante l'implementazione va evitato qualsiasi doppio conteggio.

## 7. Categoria nascosta dal mese
Requisito futuro: una categoria globale puo' essere nascosta/esclusa da uno specifico mese senza archiviarla globalmente.

Preferenza preliminare: mantenere una riga mensile esplicita e distinguere zero da nascosta tramite attributo/configurazione mensile.

## 8. Movimenti categoria e Disponibile - estensione successiva
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

Questa struttura deve permettere di derivare saldi correnti e saldo calcolato di chiusura senza salvare aggregati ridondanti come fonte primaria.

## 9. Movimenti tra posizioni - estensione futura
Lo snapshot house_month_account_balances descrive dove si trova il denaro ora.

Per lo storico servira' una tabella di movimenti con:
- fromMoneyAccountId;
- toMoneyAccountId;
- amountCents;
- movementDate;
- note;
- createdAt.

Trasferimento atomico: decremento sorgente + incremento destinazione, totale Casa invariato.

## 10. Delete e storico
Archivio e delete sono separati.

Hard delete futura:
- vietata se esiste denaro non riallocato;
- non deve invalidare record storici;
- prima del codice va scelta una strategia tra soft-delete definitivo, snapshot storico o altra soluzione coerente.

Le nuove tabelle di chiusura aumentano l'importanza di questa decisione: categoryId puo' essere referenziato da allocazioni, closings e transfers.

## 11. Transazioni
Devono essere atomiche:
- creazione piano;
- modifica pianificazione;
- modifica posizioni;
- chiusura mese;
- futuri trasferimenti tra posizioni.

Chiusura mese atomica:
1. validare mese OPEN;
2. validare confirmed balances;
3. validare per ogni categoria somma transfer = confirmed balance;
4. insert/update house_month_category_closings;
5. insert house_month_closing_transfers;
6. update house_months status=CLOSED, closedAt=now, updatedAt=now.

Se qualunque passaggio fallisce, il mese resta OPEN e nessuna chiusura parziale deve essere persistita.

## 12. Invarianti di dominio
- denaro mai negativo nei saldi confermati;
- allocato nuovo mai oltre le risorse;
- posizionato mai oltre le risorse;
- opening separato dalle nuove risorse;
- un mese CLOSED non viene modificato dai flussi ordinari;
- mese successivo richiede precedente CLOSED quando esiste;
- per ogni categoria chiusa: distribuito = confermato;
- adjustment = confermato - calcolato;
- i transfer verso AVAILABLE non creano opening categoria;
- i transfer verso CATEGORY concorrono all'opening suggerito del mese successivo;
- i movimenti tra posizioni conservano il totale.

## 13. Preservazione dati per Analisi & Suggerimenti
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

Gli indicatori e suggerimenti saranno derivati. Evitare di usare come fonte di verita' aggregati o insight salvati quando possono essere ricalcolati dai dati primari.

L'audit post-Casa dovra' verificare se servono:
- snapshot storici aggiuntivi;
- tabelle evento;
- metadati sulle correzioni;
- indici/query per analisi longitudinali;
- eventuali versioni/snapshot delle definizioni categoria.

## 14. Migrazioni
Database Room corrente prima della chiusura: versione 5.

La chiusura richiede un nuovo version bump per introdurre almeno:
- house_month_category_closings;
- house_month_closing_transfers;
- relativi converter/enums.

Durante lo sviluppo iniziale fallbackToDestructiveMigration() e' ancora temporaneamente accettato per dati di test.

Prima dell'uso reale:
- rimuovere fallback distruttivo;
- abilitare export schema;
- scrivere migrazioni Room esplicite.

## 15. Nota su ID e cloud futuro
Lo schema usa attualmente PK Long autoGenerate.

Prima della sincronizzazione multi-device va presa una decisione esplicita:
- UUID/String globale;
- oppure Long locale + syncId globale.
