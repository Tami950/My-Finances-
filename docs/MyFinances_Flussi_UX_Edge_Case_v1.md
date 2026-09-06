# MyFinances - Flussi UX, Stati ed Edge Case v1

Data: 6 settembre 2026
Scopo: descrivere i flussi operativi correnti di Casa, con particolare attenzione a pianificazione, Personalizzazione, chiusura e casi limite.

## 1. Responsabilita' delle sezioni

### Dashboard
Risponde a: "Cosa devo sapere adesso?". Mostra sintesi cross-app e non e' il luogo principale per modificare la Pianificazione Casa.

### Casa / Pianificazione
Risponde a: "Come gestisco il mese Casa?". Contiene risorse del mese, categorie, Disponibile, posizioni fisiche, spese fisse, pendenti, modifica e chiusura.

### Casa / Personalizzazione
Risponde a: "Come e' configurata Casa?". Gestisce categorie globali, posizioni e default ricorrenti. Alcune modifiche possono essere applicate esplicitamente anche al mese OPEN corrente; non devono modificarlo in silenzio.

## 2. Ingresso e setup Casa
- Ogni nuovo ingresso in Casa dalla bottom navigation parte da Pianificazione.
- Una rotation/configuration change mantiene la tab corrente.
- Setup completabile con almeno una categoria attiva e una posizione del denaro attiva.
- Lo stato di setup persistito e' distinto dalla readiness operativa.

## 3. Creazione del mese
Se il mese corrente non esiste:
- se il mese precedente immediato esiste ed e' OPEN, prima viene proposta la sua chiusura;
- altrimenti il planner viene aperto se esistono categoria e posizione attive.

Il planner precompila:
- `Nuove risorse Casa del mese` dal valore abituale salvato in Personalizzazione, se presente;
- `Disponibile ereditato` dalla chiusura precedente; nel primo mese senza precedente vale 0;
- opening delle categorie `BUDGET` dal routing della chiusura precedente;
- prefunding delle `FIXED_EXPENSE` dal routing della chiusura precedente;
- importo previsto delle `FIXED_EXPENSE` dal relativo valore abituale.

Tutti i valori mensili restano modificabili.

## 4. Categorie BUDGET e FIXED_EXPENSE
In UI l'utente vede una checkbox `Spesa fissa`.

`BUDGET`:
- puo' essere Libera o Con obiettivo;
- possiede opening e nuova allocazione;
- in futuro possiede movimenti;
- genera residuo di chiusura.

`FIXED_EXPENSE`:
- non usa Libera/Obiettivo;
- richiede un importo abituale mensile;
- possiede importo previsto del mese, prefunding e quota da nuove risorse;
- puo' essere Pagata o Da pagare;
- non richiede movimenti ordinari;
- non genera un normale residuo di categoria.

Formula:
```text
planned = importo previsto
prefunded = gia' coperto da mesi precedenti
newAllocation = planned - prefunded
```

## 5. Modifica importo fixed nel planner
Nel foglio della spesa fissa si puo' cambiare l'importo previsto del solo mese.

Flag opzionale:
`Usa questo importo come nuovo valore abituale`.

Il flag e' disattivato per default. Se attivo e il valore differisce dal default globale, al salvataggio del piano viene richiesta conferma prima di aggiornare Personalizzazione.

La X del bottom sheet annulla le modifiche effettuate nel foglio; `Fatto` le applica al draft del piano.

## 6. Modifica da Personalizzazione
Quando si cambia il valore abituale di una FIXED_EXPENSE gia' presente in un mese OPEN, l'utente sceglie:
- `Dal prossimo mese`;
- `Anche al mese corrente`.

### Aumento nel mese corrente
Serve una sorgente esplicita:
- Disponibile, solo se sufficiente, mostrando il saldo disponibile;
- una categoria BUDGET con denaro sufficiente.

Le sorgenti insufficienti non vengono proposte. Un'altra FIXED_EXPENSE non e' sorgente ordinaria.

### Diminuzione nel mese corrente
L'importo liberato va:
- al Disponibile, default;
- oppure a una categoria BUDGET.

Se il nuovo planned scende sotto il prefunding, l'eccedenza prefinanziata deve essere liberata e non puo' restare nella spesa fissa.

### Conversione BUDGET -> FIXED_EXPENSE nel mese OPEN
Se applicata anche al mese corrente:
```text
vecchio opening -> prefunding
vecchia allocazione -> quota da nuove risorse
somma -> planned iniziale
opening -> 0
```

Poi l'eventuale differenza rispetto al nuovo importo viene riconciliata.

I mesi CLOSED non vengono reinterpretati.

## 7. Stato pagata / da pagare
Segnare una spesa fissa Pagata o Da pagare non cambia la pianificazione: i soldi erano gia' vincolati.

Lo stato serve a distinguere:
- obbligo previsto ma non ancora eseguito;
- obbligo materialmente pagato.

## 8. Chiusura - categorie BUDGET
Il wizard mostra saldo calcolato e `Saldo reale da chiudere`, sempre correggibile.

Per una categoria BUDGET attiva:
```text
Mantieni nella categoria
=
max(saldo reale - somma destinazioni esplicite, 0)
```

Quindi:
- se non si cambia nulla, tutto il residuo resta nella categoria;
- se cambia il saldo reale, `Mantieni` si aggiorna subito;
- se vengono aggiunte destinazioni, `Mantieni` diminuisce automaticamente;
- se le destinazioni superano il saldo reale, `Mantieni` va a 0, compare errore e `Fatto` resta disabilitato.

Per una categoria archiviata il residuo non puo' restare implicitamente nella stessa categoria: deve essere riallocato completamente verso destinazioni valide.

## 9. Chiusura - Disponibile
Il foglio del Disponibile mostra nell'ordine:
1. saldo reale;
2. `Mantieni Disponibile`, aggiornato live;
3. destinazioni;
4. eventuali errori.

Il Disponibile resta libero per default. Puo' essere trasferito a categorie BUDGET oppure usato per prefinanziare FIXED_EXPENSE.

Se la distribuzione supera il saldo reale:
- compare errore;
- `Fatto` resta disabilitato;
- `Chiudi mese` resta disabilitato.

## 10. Chiusura - destinazione FIXED_EXPENSE
Inviare denaro a una spesa fissa significa prefinanziare il suo prossimo mese, non creare opening.

Limite:
```text
incoming totale verso fixed <= importo abituale fixed
```

Il controllo somma tutte le sorgenti del wizard: categorie + Disponibile. Se il limite viene superato, la distribuzione e la chiusura restano invalide finche' l'utente non corregge gli importi.

## 11. Chiusura - riconciliazione FIXED_EXPENSE
Il wizard mostra previsto, reale e scelta finale.

Se la spesa risulta non pagata occorre scegliere:
- l'ho pagata ma non avevo aggiornato lo stato;
- resta da pagare;
- non e' piu' dovuta.

### Reale < previsto
Si libera una quota. Default: Disponibile. Puo' essere riallocata verso destinazioni valide.

### Reale > previsto
L'extra viene tolto automaticamente dal Disponibile.

Se il Disponibile non basta:
- Disponibile calcolato -> 0;
- la parte scoperta viene conservata nello storico;
- viene mostrato un warning;
- la chiusura resta consentita dopo conferma esplicita;
- l'app non inventa da dove provenga il denaro mancante.

## 12. Spese fisse pendenti
Scegliendo `Resta da pagare` si puo' aggiungere una nota opzionale.

Nel mese successivo compare una sezione separata, per esempio:
```text
Spese fisse pendenti
Affitto 700 EUR
Ereditato da Agosto 2026
Nota: pagamento previsto il 2 settembre
```

Il pendente:
- non e' opening;
- non e' nuova allocazione;
- non e' Disponibile;
- e' denaro gia' finanziato e ancora fisicamente presente.

Finche' non viene pagato concorre ai Fondi Casa complessivi. Quando viene segnato pagato non viene sottratto nuovamente dal budget corrente.

## 13. Bottom sheet - regola globale
Per tutta l'app:
- niente dismiss toccando lo sfondo;
- niente dismiss trascinando il foglio verso il basso;
- X esplicita in alto a destra;
- X = annulla il draft locale del foglio;
- `Fatto/Salva/Conferma` = valida e applica;
- se il contenuto locale e' invalido, l'azione positiva resta disabilitata quando applicabile.

Il componente condiviso impedisce backdrop/swipe; i ViewModel mantengono snapshot/draft per rendere reale il rollback della X.

## 14. Fondi Casa e posizioni fisiche
Prima dei movimenti:
```text
Fondi Casa complessivi
=
nuove risorse
+ Disponibile ereditato
+ opening BUDGET
+ prefunding FIXED_EXPENSE
+ pendenti ancora finanziate
```

Le posizioni descrivono dove si trovano fisicamente questi soldi. La stessa cifra non puo' essere contata due volte e il totale posizionato non puo' superare i Fondi Casa complessivi.

## 15. Edge case principali
- Primo mese: Disponibile ereditato = 0.
- Mese precedente OPEN: blocca la creazione del successivo e propone la chiusura.
- Zero nuove risorse ma solo carryover: il piano puo' essere creato se gli altri vincoli sono validi.
- Prefunding > planned: piano invalido finche' l'eccedenza non viene riallocata.
- Fixed destination sovrafinanziata da piu' sorgenti: errore globale del wizard.
- Deficit fixed senza sufficiente Disponibile: warning + conferma, non hard block definitivo.
- Categoria destinazione archiviata tra chiusura e mese successivo: resta un edge case da gestire esplicitamente prima di congelare Casa.
- Un CLOSED non viene modificato dai flussi ordinari.

## 16. Ordine di completamento Casa
1. Rendere affidabile chiusura e carryover, incluso fixed/prefunding.
2. Test reale Agosto -> Settembre.
3. Navigazione e storico mesi.
4. Movimenti categorie/Disponibile.
5. Movimenti/trasferimenti delle posizioni fisiche.
6. Rifiniture Personalizzazione.
7. Audit obbligatorio dei dati grezzi per Analisi & Suggerimenti.

`Analisi & Suggerimenti` deve derivare indicatori dai dati storici e non sostituire i dati grezzi con interpretazioni persistite come fonte di verita'.
