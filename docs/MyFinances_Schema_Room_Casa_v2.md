# MyFinances - Schema Room Casa v2

Data: 30 agosto 2026
Stato: schema logico corrente e prossime estensioni necessarie alla Pianificazione

## 1. Principi
- Il denaro usa Long in centesimi.
- Le categorie sono concetti logici persistenti.
- I money account sono contenitori fisici generali.
- Il piano mensile collega risorse, categorie e posizioni fisiche.
- Lo stato di chiusura appartiene al mese.
- La posizione fisica e' uno snapshot corrente, non uno storico movimenti.

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

Note:
- descrive il contenitore fisico;
- non contiene direttamente il saldo totale dell'account;
- puo' essere riutilizzato anche da Personale.

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

Estensioni future candidate:
- flag per evidenziazione Dashboard;
- eventuali preferenze di visualizzazione.

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
- totalResourcesCents >= 0;
- un nuovo mese nasce OPEN;
- CLOSED implica mese storico e non modificabile dai flussi ordinari;
- non creare il mese successivo se il precedente esiste ed e' OPEN;
- closedAt resta null per OPEN e verra' valorizzato dal futuro flusso di chiusura mese.

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

Derivati:
- categoryTotalCents = openingBalanceCents + allocatedCents

Regole:
- openingBalanceCents >= 0;
- allocatedCents >= 0;
- somma allocatedCents del mese <= house_months.totalResourcesCents.

### 2.5 house_month_account_balances
Campi:
- id: Long PK autoGenerate
- houseMonthId: Long FK -> house_months.id CASCADE
- moneyAccountId: Long FK -> money_accounts.id NO_ACTION
- amountCents: Long
- createdAt: Long
- updatedAt: Long

Vincolo unico:
- (houseMonthId, moneyAccountId)

Regole:
- amountCents >= 0;
- somma amountCents <= totalResourcesCents;
- differenza positiva = denaro Casa ancora non posizionato/riconciliato;
- nessun euro deve essere contato contemporaneamente su due posizioni;
- una posizione a zero puo' non avere una riga persistita.

## 3. Opening balance e mese precedente
Obiettivo definitivo:
- il residuo finale consolidato di agosto alimenta openingBalanceCents di settembre per la stessa categoria;
- se il dato non esiste -> 0;
- il valore suggerito resta modificabile.

Per calcolare il residuo finale servira' il modello movimenti/spese e/o la chiusura mese.

La chiusura mese deve avvenire prima della pianificazione del mese successivo quando il mese precedente esiste.

## 4. Stato mese
HouseMonthStatus e' ora persistito direttamente in house_months.

Regole implementate:
- createPlan crea sempre il mese in stato OPEN;
- updatePlan e updatePositions rifiutano mesi CLOSED;
- createPlan controlla il mese precedente: se esiste ed e' OPEN, la nuova pianificazione viene rifiutata.

Non usare DataStore/AppPreferences per isHouseMonthClosed, perche' la chiusura e' una proprieta' storica di ogni singolo mese.

La transizione OPEN -> CLOSED e la valorizzazione di closedAt verranno implementate con il flusso di chiusura mese.

## 5. Categoria nascosta dal mese
Requisito nuovo: una categoria globale puo' essere nascosta/esclusa da uno specifico mese senza archiviarla globalmente.

Soluzione dati da scegliere prima dell'implementazione. Opzioni:
1. aggiungere isHiddenInMonth a house_monthly_allocations;
2. introdurre tabella di membership/configurazione del mese;
3. considerare assenza di allocation come esclusione.

Preferenza preliminare: mantenere una riga mensile anche per categorie incluse a zero e usare un attributo mensile esplicito se serve distinguere "zero" da "nascosta".

## 6. Movimenti categoria - estensione futura
Serve una struttura append-only per tracciare operazioni su una categoria del mese, ad esempio:
- spesa/uscita;
- entrata;
- rettifica.

Campi candidati:
- id
- houseMonthId
- categoryId
- type
- amountCents
- movementDate
- note
- createdAt
- updatedAt

Questa tabella permettera' di derivare:
- speso;
- entrate;
- residuo corrente;
- residuo finale da trasferire al mese successivo.

## 7. Movimenti tra posizioni - estensione futura
Lo snapshot house_month_account_balances descrive dove si trova il denaro ora.

Per tracciare come si e' spostato servira' una tabella separata, candidata:
- house_money_movements

Campi candidati:
- id
- houseMonthId
- fromMoneyAccountId
- toMoneyAccountId
- amountCents
- movementDate
- note
- createdAt

Regola:
- movimento tra account non cambia il totale Casa;
- decremento sorgente e incremento destinazione devono essere atomici.

## 8. Chiusura mese - dati futuri
La chiusura deve consolidare almeno:
- residuo finale per categoria;
- destinazione del residuo;
- status CLOSED;
- closedAt.

Possibili destinazioni residue:
- stessa categoria mese successivo;
- altra categoria Casa;
- fondo Casa;
- risparmio Casa;
- disponibile da allocare;
- altre destinazioni deliberate.

Lo schema definitivo della chiusura verra' definito quando si implementera' il relativo wizard.

## 9. Delete e storico
Archivio e delete sono separati.

Hard delete futura:
- vietata se esiste denaro non riallocato;
- deve chiedere dove spostarlo;
- non deve invalidare record storici.

Dato che house_monthly_allocations usa FK NO_ACTION verso house_categories, una categoria referenziata dalla storia non puo' essere cancellata senza una strategia esplicita. Prima di implementare delete reale va deciso se usare:
- soft delete permanente per entita' storiche;
- snapshot del nome/tipo nel record mensile;
- altra strategia di preservazione storico.

## 10. Transazioni
Creazione del piano:
- insert house_months;
- insert house_monthly_allocations;
- insert house_month_account_balances;

devono avvenire in una sola transazione Room.

Modifica pianificazione esistente:
- update house_months;
- update delle allocazioni mensili interessate;
- validazione delle posizioni gia' esistenti rispetto alle nuove risorse;

deve essere atomica.

Modifica posizioni:
- insert/update/delete dei singoli house_month_account_balances;
- validazione della somma rispetto alle risorse del mese;

deve essere atomica e separata dalla modifica della pianificazione logica.

Anche i futuri trasferimenti tra posizioni devono essere atomici.

## 11. Invarianti di dominio
- denaro mai negativo;
- allocato nuovo mai oltre le risorse del mese;
- posizionato mai oltre le risorse del mese;
- opening balance separato dalle risorse nuove;
- un mese CLOSED non viene modificato da flussi ordinari;
- il mese successivo richiede precedente CLOSED quando precedente esiste;
- la posizione fisica non rappresenta la cronologia;
- i movimenti tra posizioni conservano il totale.

Le validazioni vengono applicate sia nello stato UI/ViewModel per feedback immediato sia nel repository prima della scrittura Room.

## 12. Migrazioni
Database Room corrente: versione 5.

La versione 5 introduce in house_months:
- status;
- closedAt.

Durante lo sviluppo iniziale fallbackToDestructiveMigration() e' ancora temporaneamente accettato per dati di test. Il passaggio alla versione 5 puo' quindi cancellare il database locale di sviluppo.

Prima dell'uso reale:
- rimuovere fallback distruttivo;
- abilitare export schema;
- scrivere migrazioni Room esplicite.

## 13. Nota su ID e cloud futuro
Lo schema usa attualmente PK Long autoGenerate.

Prima della sincronizzazione multi-device va presa una decisione esplicita:
- passare a UUID/String come identificatore stabile globale;
- oppure mantenere Long locale e aggiungere un syncId globale.

Non cambiare silenziosamente questo aspetto: la decisione deve essere presa prima che lo schema cloud diventi stabile.
