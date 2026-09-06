# MyFinances - Flussi UX, Stati ed Edge Case v1

Data: 6 settembre 2026
Scopo: descrivere come si muove l'utente nelle sezioni, cosa puo' modificare e quali casi limite devono essere gestiti.

## 1. Principio di responsabilita' delle sezioni

### Dashboard
Risponde a: "Cosa devo sapere adesso?"

Mostra sintesi cross-app. Non e' il luogo principale per modificare la pianificazione Casa.

### Casa / Pianificazione
Risponde a: "Come gestisco il mese Casa?"

E' la schermata operativa del mese: risorse, categorie, Disponibile, posizioni, stato mese, modifiche e chiusura.

### Casa / Personalizzazione
Risponde a: "Quali categorie e posizioni esistono e come sono configurate?"

Modifica definizioni globali, non saldi mensili.

## 2. Ingresso in Casa
Regola UX:
- ogni nuovo ingresso in Casa dalla bottom navigation atterra su Pianificazione;
- una semplice rotazione/configuration change non resetta la tab corrente.

Caso:
1. Casa -> Personalizzazione;
2. bottom bar -> Bollette;
3. bottom bar -> Casa;
4. risultato atteso: Pianificazione.

## 3. Setup Casa
Se isHouseSetupCompleted = false:
- Pianificazione mostra onboarding;
- "Configura Casa" porta a Personalizzazione;
- setup completabile con almeno una categoria attiva e un money account attivo;
- completamento persistito in DataStore.

Dopo il setup:
- per creare un nuovo piano servono ancora almeno una categoria attiva e un money account attivo;
- un piano gia' esistente resta consultabile anche se successivamente tutte le entita' vengono archiviate.

## 4. Creazione primo mese
Se setup completato e non esiste il mese corrente:
- mostra "Pianifica mese";
- apre CreateHousePlan;
- carica categorie e money account attivi;
- l'utente inserisce risorse totali, allocazioni e posizioni;
- salva tutto atomicamente.

Se non esiste alcun mese precedente, opening balance suggerito = 0.

## 5. Creazione mese successivo
Regola:
- se il mese precedente esiste ed e' OPEN, il mese successivo non e' pianificabile;
- l'utente deve prima chiudere il mese precedente;
- dopo la chiusura, gli opening balance vengono proposti dalla distribuzione dei residui finali;
- dato assente -> 0;
- opening balance resta modificabile.

Gli importi inviati al Disponibile in chiusura non diventano opening di categorie.

## 6. Schermata Pianificazione con mese OPEN
Header:
- mese/anno;
- badge/label Aperto;
- risorse totali.

Riepilogo:
- Allocato nel mese;
- Disponibile Casa.

Sezione categorie:
- card uniforme per ogni categoria inclusa nel mese;
- nome;
- gia' presente;
- nuova allocazione;
- totale disponibile teorico;
- in futuro speso/residuo corrente.

Azioni:
- Modifica pianificazione;
- nascondi categoria dal mese, quando implementato.

Sezione posizione attuale:
- card/lista uniforme con account e saldo Casa corrente;
- totale posizionato;
- da posizionare;
- Modifica posizioni.

Footer:
- Chiudi mese.

## 7. Modifica pianificazione
Disponibile solo su mese OPEN.

Consente di modificare:
- risorse del mese;
- opening balance mensile;
- nuova allocazione per categoria;
- nota;
- inclusione/nascondimento mensile delle categorie quando disponibile.

Non modifica le proprieta' globali della categoria.

## 8. Card categoria e dettaglio movimenti
La card non modifica la categoria globale.

Click futuro apre dettaglio della categoria nel mese:
- elenco movimenti;
- saldo corrente;
- aggiungi uscita;
- aggiungi entrata;
- rettifica.

Esempio:
- Gatto disponibile 150 EUR;
- uscita 20 EUR;
- residuo 130 EUR.

## 9. Disponibile Casa
Il Disponibile Casa e' liquidita' non vincolata ad alcuna categoria. Non deve necessariamente essere allocato.

Non e' una categoria fittizia.

Con i movimenti potra' avere operazioni proprie, ad esempio:
- Disponibile 300 EUR;
- pizza non categorizzata -15 EUR;
- Disponibile 285 EUR.

Una spesa con nessuna categoria logica appartiene al Disponibile e deve rimanere tracciabile.

## 10. Nascondere una categoria dal mese
Caso d'uso:
- categoria esiste globalmente ma non serve in un determinato mese.

Azione corretta:
- "Nascondi dal mese" / "Escludi dal mese".

Non equivale a eliminare o archiviare globalmente.

## 11. Archiviazione ed eliminazione globale futura
Archiviazione:
- mantiene l'entita' e lo storico;
- esclude dai nuovi piani;
- consente riattivazione.

Delete reale futura:
- se esiste denaro, bloccare e chiedere riallocazione;
- se esiste storico, preservarne significato e riferimenti;
- progettare la strategia tecnica prima di implementare il pulsante definitivo.

## 12. Posizione attuale dei soldi
Rappresenta esclusivamente dove si trova ORA il denaro Casa.

Esempio:
- Libretto 600;
- Quaderno 400;
- Online 1000;
- totale Casa 2000.

Lo stesso denaro non puo' essere contato in piu' posizioni.

## 13. Modifica posizioni
Disponibile su mese OPEN.

Validazione:
- valori individuali >= 0;
- somma <= risorse totali;
- somma < totale consentita con "Da posizionare";
- somma > totale blocca subito il salvataggio.

Gli input precompilati possono essere azzerati rapidamente tramite trailing action.

## 14. Storico posizioni futuro
Modello desiderato:
- movimento Libretto -> Quaderno 1400;
- movimento Quaderno -> Online 1000.

Lo storico deve derivare da movimenti espliciti, non dal confronto tra snapshot arbitrari.

## 15. Validazioni immediate
La UI segnala subito stati monetari impossibili e disabilita Salva. Il repository ripete le invarianti.

Esempio:
- risorse 2000;
- allocazioni 2100;
- overflow 100;
- Salva disabilitato.

## 16. Chiusura mese - flusso definitivo v1
Disponibile solo su OPEN.

### 16.1 Saldo calcolato e saldo confermato
Per ogni categoria il wizard mostra:
- saldo calcolato dall'app;
- saldo finale confermato;
- eventuale rettifica = confermato - calcolato;
- eventuale nota di rettifica.

Il saldo confermato e' SEMPRE modificabile, anche quando saranno presenti tutti i movimenti.

Motivazione:
- spese dimenticate;
- discrepanze reali;
- correzioni manuali;
- necessita' di non falsificare lo storico.

Il sistema conserva sia il calcolato sia il confermato e non riscrive i movimenti per farli coincidere.

### 16.2 Distribuzione residuo
Per ogni saldo finale confermato > 0 l'utente distribuisce l'intero residuo verso una o piu' destinazioni:
- stessa categoria nel mese successivo;
- altra categoria nel mese successivo;
- piu' categorie con split;
- Disponibile del mese successivo.

"Mantieni" e' solo una scorciatoia UX per categoria sorgente -> stessa categoria.

Esempio valido:
- residuo Gatto 200;
- Gatto 100;
- Farmacia 50;
- Disponibile 50;
- totale distribuito 200.

Esempi non validi:
- totale 190 su residuo 200;
- totale 210 su residuo 200.

Invariante:
- somma destinazioni = saldo finale confermato;
- ogni importo >= 0.

Nessun Fondo Casa separato nella prima versione.

### 16.3 Conferma chiusura
Il pulsante Chiudi mese e' attivo solo quando:
- ogni saldo confermato e' valido;
- ogni residuo positivo e' completamente distribuito;
- nessuna distribuzione supera il proprio residuo;
- il mese e' ancora OPEN.

La conferma salva atomicamente:
- saldo calcolato per categoria;
- saldo confermato;
- rettifica;
- nota rettifica;
- distribuzioni residue;
- status CLOSED;
- closedAt.

Dopo la chiusura:
- il mese diventa storico;
- le modifiche ordinarie sono bloccate;
- il mese successivo diventa pianificabile;
- la pianificazione successiva usa le distribuzioni di chiusura come suggerimenti opening.

## 17. Autocompletamento del mese successivo
Per ogni categoria attiva del nuovo mese:
- sommare tutte le distribuzioni del precedente CLOSED dirette a quella categoria;
- usare il risultato come opening suggerito;
- se nessuna distribuzione -> 0;
- lasciare sempre il campo modificabile.

Esempio chiusura agosto:
- Cibo -> Cibo 80;
- Gatto -> Gatto 100;
- Gatto -> Farmacia 50;
- Gatto -> Disponibile 50;
- Farmacia -> Farmacia 20.

Opening settembre:
- Cibo 80;
- Gatto 100;
- Farmacia 70.

Il Disponibile trasferito viene gestito separatamente e non crea una categoria fittizia.

## 18. Navigazione mesi
Da implementare subito dopo chiusura + autocompletamento.

Requisiti:
- mese corrente default;
- precedente/successivo;
- accesso ai CLOSED;
- OPEN chiaramente distinguibile;
- blocco del futuro quando la sequenza temporale non e' valida.

Da definire nel dettaglio durante l'implementazione:
- quanti mesi futuri mostrare;
- eventuali buchi;
- comportamento del successivo non ancora creato.

## 19. Movimenti categorie e Disponibile
Dopo la navigazione mesi:
- uscite;
- entrate;
- rettifiche;
- data operazione;
- nota;
- categoria opzionale: null significa Disponibile;
- saldo corrente derivato.

Il saldo calcolato in chiusura diventera' progressivamente affidabile grazie a questi movimenti.

## 20. Dashboard
La Dashboard non duplica Casa.

Distinzioni:
- Disponibile da spendere = personale;
- Disponibile Casa = liquidita' Casa non vincolata;
- residuo categoria = saldo corrente della specifica categoria.

## 21. Analisi & Suggerimenti - audit mandatorio post-Casa
Dopo aver completato l'intero flusso Casa, prima di considerare il dominio stabile, eseguire un audit dedicato orientato alla futura sezione Analisi & Suggerimenti.

L'audit deve verificare che il sistema conservi dati sufficienti a ricostruire:
- allocazioni pianificate;
- saldi iniziali;
- singoli movimenti;
- sforamenti;
- residui;
- rettifiche manuali;
- differenza tra saldo calcolato e reale;
- distribuzioni di chiusura;
- spostamenti frequenti tra categorie;
- uso del Disponibile;
- cambiamenti delle posizioni fisiche.

Principio:
- dato grezzo -> indicatore -> suggerimento.

Non salvare come fonte di verita' un suggerimento o un aggregato se puo' essere ricalcolato dai dati primari.

Indicatori candidati:
- categoria sottostimata/sovrastimata;
- sforamenti frequenti;
- residuo medio elevato;
- rettifiche frequenti;
- categoria quasi inutilizzata;
- allocazioni instabili;
- categorie candidate ad accorpamento/scorporo;
- consumo medio del Disponibile.

Suggerimenti futuri devono essere proposte motivate dai dati, non prescrizioni.

## 22. Componenti UI riusabili
Gia' presenti:
- AppScreen;
- AppModalBottomSheet;
- AppContentCard.

Candidati:
- card categoria mensile;
- card posizione;
- badge stato mese;
- righe monetarie;
- empty/error state;
- componenti del wizard di chiusura.

## 23. Edge case checklist
- categoria archiviata resta valida nello storico;
- delete con soldi -> bloccare e riallocare;
- delete con storico -> preservare significato;
- opening mancante -> 0;
- opening suggerito -> sempre modificabile;
- allocazioni oltre risorse -> blocco;
- posizioni oltre risorse -> blocco;
- posizioni sotto risorse -> consentito;
- mese precedente OPEN -> blocco nuovo mese;
- mese CLOSED -> niente modifiche ordinarie;
- piano esistente resta visibile anche se setup corrente non e' piu' sufficiente;
- residuo distribuito meno del confermato -> blocco chiusura;
- residuo distribuito oltre il confermato -> blocco chiusura;
- saldo confermato diverso dal calcolato -> conservare rettifica;
- saldo confermato 0 -> nessuna distribuzione necessaria;
- categoria destinazione archiviata prima del mese successivo -> non deve produrre perdita di denaro; gestire esplicitamente nel flusso di creazione/chiusura;
- rotazione con form/sheet aperto -> stato conservato;
- tastiera landscape -> contenuto raggiungibile.

## 24. Piano corrente per chiudere Casa
Ordine concordato:

### C1 - Chiusura mese
- persistenza del riepilogo di chiusura per categoria;
- saldo calcolato/confermato;
- rettifica e nota;
- distribuzioni residue;
- transazione OPEN -> CLOSED.

### C2 - Autocompletamento mese successivo
- opening suggeriti dalle distribuzioni;
- Disponibile trasferito gestito separatamente;
- fallback 0;
- valori modificabili.

### C3 - Navigazione mesi
- storico e precedente/successivo;
- regole temporali.

### C4 - Movimenti categorie + Disponibile
- entrate/uscite/rettifiche;
- saldi correnti;
- storico.

### C5 - Movimenti posizioni
- trasferimenti atomici;
- storico;
- correzione saldo separata.

### C6 - Rifiniture Personalizzazione
- delete sicuro;
- ordinamento;
- nascondi dal mese;
- altre mancanze registrate.

### C7 - Audit dati per Analisi & Suggerimenti
- riesame completo tabelle e flussi Casa;
- aggiungere/modificare eventi e dati mancanti;
- creare eventuali tabelle necessarie alla preservazione storica;
- solo dopo questo punto considerare Casa definitivamente chiusa a livello di modello dati.
