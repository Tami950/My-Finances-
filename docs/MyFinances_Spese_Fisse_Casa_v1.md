# MyFinances - Spese fisse Casa v1

Data: 6 settembre 2026
Stato: requisito funzionale e modello implementato per il primo test sulla chiusura mese

## 1. Obiettivo
Una spesa fissa e' una categoria Casa che partecipa alla pianificazione ma non rappresenta un budget da consumare tramite movimenti ordinari.

Esempi:
- Affitto;
- Mamma;
- Zia;
- altre uscite pianificate come importo unico del mese.

La spesa fissa e' distinta dalla sezione Bollette. Bollette gestisce scadenze, ricorrenze e promemoria; una spesa fissa descrive invece il comportamento dell'importo nella Pianificazione Casa.

## 2. Comportamento categoria
Il dominio usa:
- `HouseCategoryBehavior.BUDGET`;
- `HouseCategoryBehavior.FIXED_EXPENSE`.

Nella UI l'utente vede una semplice checkbox `Spesa fissa`.

Default:
- categorie esistenti migrate -> `BUDGET`;
- nuove categorie -> `BUDGET`;
- l'utente abilita esplicitamente la checkbox per trasformarle in spese fisse.

Il comportamento viene copiato nello stato mensile delle allocazioni OPEN. Questo evita che la sola definizione globale debba reinterpretare uno storico gia' chiuso.

## 3. Pianificazione mensile
Una spesa fissa continua a usare l'importo allocato nella pianificazione e riduce il Disponibile esattamente come le altre allocazioni.

Esempio:
- Risorse: 2.000 EUR;
- Affitto: 700 EUR FIXED_EXPENSE;
- Gatto: 50 EUR BUDGET;
- Disponibile: 1.250 EUR.

I 700 EUR di Affitto non diventano disponibili solo perche' la spesa non e' ancora stata materialmente pagata: sono denaro vincolato.

## 4. Stato di pagamento
Per la prima versione lo stato mensile e':
- `PLANNED`: pianificata / ancora da pagare;
- `PAID`: pagata.

La card della spesa fissa permette di segnare Pagata / Da pagare.

Cambiare lo stato non modifica il budget: indica soltanto se l'obbligo e' stato materialmente eseguito.

## 5. Chiusura mese
Per una categoria BUDGET continua a valere il flusso residuo normale.

Per una FIXED_EXPENSE il wizard mostra:
- importo pianificato;
- importo reale;
- eventuale rettifica;
- stato/risoluzione finale.

Se la spesa risulta non pagata, prima di chiudere occorre scegliere esplicitamente:
- `MARK_PAID`: e' stata pagata ma lo stato non era stato aggiornato;
- `KEEP_PENDING`: resta da pagare;
- `CANCELLED`: non e' piu' dovuta.

`KEEP_PENDING` consente una nota opzionale da mostrare insieme al pendente nel mese successivo.

## 6. Differenza pianificato / reale
Definizioni:
- planned = importo pianificato della spesa fissa nel mese;
- actual = importo reale confermato in chiusura.

### 6.1 actual = planned
Nessuna differenza monetaria da riconciliare.

### 6.2 actual < planned
Si genera un importo liberato:
- surplus = planned - actual.

Il surplus non e' un residuo della categoria fissa.
Per default va al Disponibile, ma l'utente puo' riallocarne una parte o tutto verso categorie attive.

Il valore che resta al Disponibile si aggiorna automaticamente mentre l'utente inserisce altre destinazioni.

### 6.3 actual > planned
Si genera un extra:
- deficit = actual - planned.

Il deficit viene assorbito automaticamente dal Disponibile.

Se il Disponibile non basta:
- Disponibile calcolato viene portato a zero;
- la parte non coperta viene conservata come `unreconciledFixedExpenseDeficitCents`;
- viene mostrato un warning;
- la chiusura resta consentita dopo conferma esplicita;
- il sistema non inventa una provenienza del denaro mancante.

Questo dato deve restare disponibile per controlli storici e futura Analisi & Suggerimenti.

## 7. Spesa pendente
Se in chiusura viene scelto `KEEP_PENDING`, si crea una riga in `house_fixed_expense_pendings` con:
- categoria;
- mese di origine;
- importo reale ancora dovuto;
- nota opzionale;
- stato PENDING;
- timestamp.

Il pendente NON diventa:
- opening della categoria;
- nuova allocazione;
- nuovo Disponibile.

E' un obbligo gia' finanziato nel mese di origine.

Nel mese successivo viene mostrato in una sezione separata:

```text
Spese fisse pendenti

Affitto      700 EUR
Ereditato da Agosto 2026
Nota: pagamento previsto il 2 settembre
```

Quando viene segnato pagato cambia soltanto lo stato del pendente: non viene effettuata una nuova sottrazione dal budget corrente.

## 8. Distinzione da opening e categorie normali
`BUDGET`:
- possiede saldo;
- in futuro possiede movimenti;
- genera residuo;
- residuo puo' essere mantenuto o spostato.

`FIXED_EXPENSE`:
- rappresenta un importo impegnato;
- non richiede movimenti ordinari;
- possiede stato di pagamento;
- non genera un residuo trasferibile come concetto normale;
- solo la differenza tra pianificato e reale viene riconciliata;
- se resta non pagata genera un pendente separato.

## 9. Persistenza
Room v8 introduce:
- `house_categories.behavior` con default `BUDGET`;
- `house_monthly_allocations.categoryBehavior`;
- `house_monthly_allocations.fixedExpensePaymentStatus`;
- metadati fixed expense in `house_month_category_closings`;
- `house_month_closings.unreconciledFixedExpenseDeficitCents`;
- `house_fixed_expense_pendings`.

Migrazione:
- `MIGRATION_7_8` conserva i dati esistenti;
- tutte le categorie e allocazioni precedenti vengono interpretate come `BUDGET` fino a modifica esplicita.

Quando una categoria viene trasformata in spesa fissa mentre esiste un mese OPEN, il comportamento mensile del mese OPEN viene riallineato per permettere l'uso immediato della feature. I mesi CLOSED non vengono reinterpretati.

## 10. Analisi futura
Dati da preservare:
- comportamento categoria nel mese;
- pianificato;
- reale;
- differenza;
- stato pagata/non pagata;
- scelta di chiusura;
- pendenti e tempi di risoluzione;
- deficit non riconciliati;
- destinazione degli importi liberati.

Questi dati possono supportare in futuro indicatori come:
- spese fisse sistematicamente sottostimate;
- spese fisse sovrastimate;
- pagamenti spesso pendenti alla chiusura;
- tempi medi di pagamento dopo il mese di pianificazione;
- frequenza delle discrepanze.
