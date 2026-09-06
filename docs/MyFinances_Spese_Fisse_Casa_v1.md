# MyFinances - Spese fisse Casa v1

Data: 6 settembre 2026
Stato: requisito funzionale corrente per Pianificazione e Chiusura Casa

## 1. Obiettivo
Una spesa fissa e' una categoria Casa che partecipa alla pianificazione ma non rappresenta un budget da consumare tramite movimenti ordinari.

Esempi: Affitto, Mamma, Zia e altre uscite pianificate come importo unico del mese.

La spesa fissa e' distinta da Bollette: Bollette gestisce scadenze, ricorrenze e promemoria; FIXED_EXPENSE descrive il comportamento del denaro nella Pianificazione Casa.

## 2. Comportamento categoria
Il dominio usa:
- `HouseCategoryBehavior.BUDGET`;
- `HouseCategoryBehavior.FIXED_EXPENSE`.

Nella UI l'utente vede una semplice checkbox `Spesa fissa`.

Default:
- categorie esistenti migrate -> `BUDGET`;
- nuove categorie -> `BUDGET`;
- l'utente abilita esplicitamente la checkbox.

Una FIXED_EXPENSE non usa `FLEXIBLE/TARGET`: possiede invece un `fixedExpenseDefaultCents`, cioe' l'importo mensile abituale.

## 3. Importo abituale e importo del singolo mese
In Personalizzazione una spesa fissa richiede un importo abituale maggiore di zero.

Esempio:
```text
Affitto
Spesa fissa
Importo abituale: 700 EUR
```

Quando si crea un nuovo mese, 700 EUR viene proposto automaticamente ma resta modificabile per quel mese.

Se durante la pianificazione l'utente cambia l'importo puo' attivare:
`Usa questo importo come nuovo valore abituale`.

Il flag e' sempre opt-in. Al salvataggio del piano, se e' attivo, l'app richiede una conferma prima di aggiornare il valore globale usato nei mesi futuri.

## 4. Prefinanziamento
Una spesa fissa non riceve un normale opening di categoria.

Il modello mensile distingue:
```text
fixedExpensePlannedCents   = importo previsto complessivo del mese
fixedExpensePrefundedCents = denaro gia' coperto da mesi precedenti
allocatedCents             = nuove risorse necessarie nel mese
```

Invariante:
```text
0 <= prefunded <= planned
allocated = planned - prefunded
openingBalanceCents = 0 per FIXED_EXPENSE
```

Esempio:
```text
Affitto previsto        700 EUR
Gia' prefinanziato      200 EUR
Da nuove risorse        500 EUR
```

I 200 EUR sono denaro gia' vincolato, non un residuo spendibile della categoria.

## 5. Destinazioni della chiusura
Durante la chiusura, una categoria o il Disponibile possono destinare denaro a una FIXED_EXPENSE del mese successivo. Tale denaro diventa prefunding, non opening.

Una spesa fissa non puo' essere prefinanziata oltre il proprio importo abituale previsto.

Il limite e' globale al wizard: si sommano gli importi provenienti da tutte le categorie e dal Disponibile. Se il totale supera il limite:
- viene mostrato un errore;
- `Fatto` del foglio interessato resta disabilitato;
- `Chiudi mese` resta disabilitato.

## 6. Risorse Casa abituali
Personalizzazione possiede anche `Nuove risorse Casa abituali`.

Se valorizzato, il campo `Nuove risorse Casa del mese` viene precompilato automaticamente nei nuovi piani e resta modificabile per il singolo mese.

Il Disponibile ereditato resta separato:
```text
nuove risorse abituali = default di nuove risorse del mese
Disponibile ereditato  = deriva dalla chiusura precedente
```

Nel primo mese senza precedente, il Disponibile ereditato e' automaticamente 0.

## 7. Modifica del default da Personalizzazione
Quando si modifica l'importo abituale di una FIXED_EXPENSE esistente, l'utente sceglie:
- `Dal prossimo mese`;
- `Anche al mese corrente`.

Cambiare solo il default globale non modifica silenziosamente un mese gia' OPEN.

Se viene scelto `Anche al mese corrente`, la differenza deve essere riconciliata esplicitamente.

### 7.1 Aumento
Se il nuovo importo e' maggiore, l'utente sceglie la sorgente:
- Disponibile, mostrando quanto e' disponibile;
- una categoria `BUDGET` con fondi sufficienti.

Non si usa un'altra FIXED_EXPENSE come sorgente ordinaria.

### 7.2 Diminuzione
Se il nuovo importo e' minore, l'importo liberato viene destinato a:
- Disponibile, default;
- una categoria `BUDGET`.

Se il nuovo importo scende sotto il prefunding gia' presente, l'eccedenza prefinanziata viene liberata esplicitamente e non puo' restare nella FIXED_EXPENSE.

### 7.3 Provenienza del denaro
La riconciliazione preserva, per quanto possibile, la provenienza:
- quota proveniente da nuova allocazione resta nuova allocazione;
- quota proveniente da opening diventa prefunding;
- quota liberata da prefunding verso una categoria BUDGET diventa opening;
- quota liberata da nuova allocazione verso una categoria BUDGET resta allocazione.

## 8. Conversione BUDGET -> FIXED_EXPENSE nel mese OPEN
Se una categoria gia' presente nel mese viene convertita in spesa fissa e si applica la modifica anche al mese corrente:
```text
vecchio opening categoria -> fixedExpensePrefundedCents
vecchia allocazione       -> allocatedCents
somma                      -> fixedExpensePlannedCents iniziale
openingBalanceCents        -> 0
```

Solo dopo questa conversione l'eventuale differenza con il nuovo importo abituale viene riconciliata.

I mesi CLOSED non vengono reinterpretati.

## 9. Stato di pagamento
Per la prima versione:
- `PLANNED`: prevista / ancora da pagare;
- `PAID`: pagata.

Cambiare stato non modifica la pianificazione: il denaro e' gia' considerato vincolato.

## 10. Chiusura della spesa fissa
Il wizard mostra:
- importo previsto;
- importo reale;
- eventuale rettifica;
- risoluzione finale.

Se non risulta pagata occorre scegliere:
- `MARK_PAID`;
- `KEEP_PENDING`;
- `CANCELLED`.

`KEEP_PENDING` consente una nota opzionale per il mese successivo.

### 10.1 Reale uguale al previsto
Nessuna differenza monetaria.

### 10.2 Reale minore del previsto
```text
surplus = planned - actual
```

Il surplus non e' residuo della FIXED_EXPENSE. Va al Disponibile per default e puo' essere riallocato verso categorie valide.

### 10.3 Reale maggiore del previsto
```text
deficit = actual - planned
```

Il deficit viene assorbito automaticamente dal Disponibile.

Se il Disponibile non basta:
- Disponibile calcolato -> 0;
- la parte non coperta viene salvata come `unreconciledFixedExpenseDeficitCents`;
- viene mostrato un warning;
- la chiusura resta possibile con conferma;
- l'app non inventa una provenienza del denaro mancante.

## 11. Spesa pendente
`KEEP_PENDING` crea una riga separata con categoria, mese origine, importo reale, nota, stato e timestamp.

Il pendente NON diventa opening, nuova allocazione o Disponibile. E' un obbligo gia' finanziato.

Finche' e' PENDING il denaro esiste ancora fisicamente e concorre ai Fondi Casa complessivi, ma non al Disponibile.

```text
Fondi Casa complessivi
=
nuove risorse
+ Disponibile ereditato
+ opening categorie BUDGET
+ prefunding spese fisse
+ spese fisse pendenti ancora finanziate
```

Quando il pendente viene segnato pagato non viene sottratto di nuovo dal budget corrente; smette invece di concorrere ai fondi fisicamente presenti.

## 12. Regola UX dei bottom sheet
I bottom sheet dell'app:
- non si chiudono toccando lo sfondo;
- non si chiudono trascinandoli verso il basso;
- mostrano una X in alto a destra;
- si concludono con X oppure con un'azione esplicita.

Semantica:
- X = annulla le modifiche effettuate nel foglio;
- `Fatto/Salva/Conferma` = valida e applica;
- se il contenuto e' invalido, l'azione positiva rimane disabilitata quando applicabile.

Nel foglio di distribuzione del Disponibile l'ordine e':
1. saldo reale;
2. `Mantieni Disponibile`, aggiornato live;
3. destinazioni;
4. eventuali errori.

## 13. Persistenza Room
Room v8 ha introdotto comportamento, stato di pagamento, chiusura fixed e pendenti.

Room v9 introduce:
- `house_categories.fixedExpenseDefaultCents`;
- `house_monthly_allocations.fixedExpensePlannedCents`;
- `house_monthly_allocations.fixedExpensePrefundedCents`.

`MIGRATION_8_9` mantiene i dati esistenti e converte le allocazioni FIXED_EXPENSE gia' presenti nel nuovo modello.

Le `Nuove risorse Casa abituali` sono una preferenza utente DataStore, non un dato storico mensile: il valore effettivamente usato nel mese continua a essere persistito in `house_months.totalResourcesCents`.

## 14. Analisi futura
Dati da preservare:
- comportamento categoria nel mese;
- valore abituale globale;
- pianificato mensile;
- prefunding;
- quota da nuove risorse;
- reale;
- rettifica;
- stato pagata/non pagata;
- scelta di chiusura;
- pendenti e tempi di risoluzione;
- deficit non riconciliati;
- destinazione degli importi liberati e dei prefinanziamenti.

Questi dati permetteranno indicatori su stabilita' delle spese fisse, sotto/sovrastima, frequenza dei pendenti e uso ricorrente del prefunding.
