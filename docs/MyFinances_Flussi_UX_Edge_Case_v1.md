# MyFinances - Flussi UX, Stati ed Edge Case v1

Data: 30 agosto 2026
Scopo: descrivere come si muove l'utente nelle sezioni, cosa puo' modificare e quali casi limite devono essere gestiti.

## 1. Principio di responsabilita' delle sezioni

### Dashboard
Risponde a: "Cosa devo sapere adesso?"

Mostra sintesi e KPI cross-app. Non e' il luogo principale per modificare la pianificazione Casa.

### Casa / Pianificazione
Risponde a: "Come gestisco il mese Casa?"

E' la schermata operativa del mese: risorse, categorie, posizioni, stato mese, modifiche e chiusura.

### Casa / Personalizzazione
Risponde a: "Quali categorie e posizioni esistono e come sono configurate?"

Modifica definizioni globali, non saldi mensili.

## 2. Ingresso in Casa
Regola UX:
- ogni ingresso in Casa dalla bottom navigation atterra su Pianificazione;
- Personalizzazione non rimane selezionata come destinazione di ingresso tra una visita e l'altra.

Caso:
1. Casa -> Personalizzazione;
2. bottom bar -> Bollette;
3. bottom bar -> Casa;
4. risultato atteso: Pianificazione.

La navigazione interna eventualmente piu' profonda di Casa deve essere riportata alla home appropriata quando si rientra dalla bottom navigation, secondo la policy che verra' implementata.

## 3. Setup Casa
Se isHouseSetupCompleted = false:
- Pianificazione mostra onboarding;
- "Configura Casa" porta a Personalizzazione;
- setup completabile con almeno una categoria attiva e un money account attivo;
- completamento persistito in DataStore.

## 4. Creazione primo mese
Se setup completato e non esiste il mese corrente:
- mostra "Pianifica mese";
- apre schermata CreateHousePlan;
- carica categorie e money account attivi;
- l'utente inserisce risorse totali, allocazioni e posizioni;
- salva tutto atomicamente.

Se non esiste alcun mese precedente, opening balance suggerito = 0.

## 5. Creazione mese successivo
Requisito:
- se il mese precedente esiste ed e' OPEN, il mese successivo non e' pianificabile;
- l'utente deve prima chiudere il mese precedente;
- dopo la chiusura, gli opening balance vengono proposti dai residui finali consolidati;
- dato assente -> 0;
- opening balance resta modificabile.

La UX completa per navigazione a mesi futuri verra' definita dopo il flusso di chiusura.

## 6. Schermata Pianificazione con mese OPEN
Struttura prevista:

Header:
- mese/anno;
- badge o label Aperto;
- risorse totali.

Riepilogo:
- Allocato nel mese;
- Da allocare.

Sezione categorie:
- card uniforme per ogni categoria inclusa nel mese;
- nome;
- gia' presente;
- nuova allocazione;
- totale disponibile;
- in futuro speso/residuo.

Azioni:
- Modifica pianificazione;
- nascondi categoria dal mese, quando implementato.

Sezione posizione attuale:
- card/lista uniforme con account e saldo Casa corrente;
- totale posizionato;
- da posizionare;
- Modifica posizioni.

Footer/azione principale:
- Chiudi mese.

## 7. Modifica pianificazione
Disponibile solo su mese OPEN.

Consente di modificare:
- risorse del mese;
- opening balance mensile;
- nuova allocazione per categoria;
- nota;
- inclusione/nascondimento mensile delle categorie quando disponibile.

Non consente di modificare:
- nome globale categoria;
- tipo FLEXIBLE/TARGET;
- target globale;
- archivio globale;
- ordine globale.

Queste operazioni appartengono a Personalizzazione.

## 8. Card categoria e dettaglio futuro
La card non e' un pulsante di modifica globale.

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

Il movimento influenza la situazione mensile, non la definizione globale "Gatto".

## 9. Nascondere una categoria dal mese
Caso d'uso:
- categoria esiste globalmente ma non serve in un determinato mese.

Azione corretta:
- "Nascondi dal mese" / "Escludi dal mese".

Non equivale a:
- eliminare categoria;
- archiviare categoria.

Il mese storico deve ricordare la propria configurazione.

## 10. Archiviazione globale categoria
In Personalizzazione:
- categoria archiviata resta visibile attenuata;
- non e' selezionabile nei nuovi piani;
- puo' essere riattivata;
- mantiene validita' nello storico.

## 11. Eliminazione globale futura
Se l'utente chiede delete reale:
- se saldo logico/residuo > 0, delete bloccata;
- aprire modal/bottom sheet che richiede riallocazione del denaro;
- solo dopo saldo zero si puo' procedere;
- lo storico non deve perdere riferimenti o significato.

Edge case da progettare prima del codice: categoria gia' usata in mesi CLOSED.

## 12. Posizione attuale dei soldi
Rappresenta esclusivamente dove si trova ORA il denaro Casa.

Esempio iniziale:
- Libretto 2000;
- Quaderno 0;
- Online 0.

Dopo prelievo 1400:
- Libretto 600;
- Quaderno 1400;
- Online 0.

Dopo spostamento 1000 dal Quaderno a Online:
- Libretto 600;
- Quaderno 400;
- Online 1000.

Errore da evitare:
- Libretto 600;
- Quaderno 1400;
- Online 1000;
- totale 3000 su risorse 2000.

## 13. Modifica posizioni
Disponibile su mese OPEN.

L'utente puo' aggiornare i saldi correnti quando preleva/sposta denaro.

Validazione:
- valori individuali >= 0;
- somma <= risorse totali;
- se somma < totale, mostra "Da posizionare";
- se somma > totale, errore immediato e salvataggio disabilitato.

## 14. Storico posizioni futuro
La card delle posizioni dovra' poter aprire un dettaglio con storico del mese.

Modello desiderato finale:
- movimento Libretto -> Quaderno 1400;
- movimento Quaderno -> Online 1000.

Lo storico non deve essere ricostruito confrontando snapshot arbitrari se possiamo registrare movimenti espliciti.

## 15. Validazioni immediate
La UI non deve accettare stati monetari impossibili e poi mostrare solo un errore al salvataggio.

Esempio allocazioni:
- risorse 2000;
- nuove allocazioni 2100;
- errore immediato;
- Salva disabilitato.

Esempio posizioni:
- risorse 2000;
- posizioni 2300;
- errore immediato;
- Salva disabilitato.

Il repository ripete comunque le invarianti.

## 16. Chiusura mese
Disponibile solo su OPEN.

Obiettivi:
- consolidare spese/movimenti;
- determinare residui finali;
- decidere destinazione residui;
- salvare closedAt;
- impostare status CLOSED;
- preparare dati per il mese successivo.

Un mese CLOSED e' storico e non viene modificato dai flussi ordinari.

Eventuale "Riapri mese" e correzioni a posteriori sono funzionalita' future da progettare esplicitamente.

## 17. Navigazione mesi
Requisito registrato:
- controllo mese precedente;
- controllo mese successivo;
- accesso allo storico;
- mese corrente come default.

Da definire dopo chiusura mese:
- quanti mesi futuri mostrare;
- se permettere creazione di mesi futuri oltre il prossimo;
- comportamento se ci sono buchi temporali;
- policy per mesi CLOSED/OPEN durante la navigazione.

## 18. Dashboard
La Dashboard non duplica il manager mensile.

Mostra almeno in futuro:
- Disponibile da spendere PERSONALE realmente libero;
- stato Casa del mese;
- risorse Casa;
- da allocare Casa;
- categorie principali/evidenziate;
- posizione sintetica denaro Casa;
- bollette rilevanti;
- sintesi Personale.

Importante:
- "Disponibile da spendere" = personale;
- "Da allocare" = Casa, nuove risorse non ancora assegnate;
- non usare un generico "Residuo Casa" per entrambi i concetti.

## 19. Categorie principali Dashboard
Requisito futuro:
- l'utente potra' probabilmente scegliere quali categorie mostrare in Dashboard;
- candidato: flag esplicito nella personalizzazione categoria;
- da coordinare con ordinamento personalizzato.

## 20. Componenti UI riusabili
Regola:
- comportamento strutturale ripetuto -> componente condiviso;
- evitare mega-componenti che contengono logica di dominio.

Gia' presenti:
- AppScreen;
- AppModalBottomSheet.

Prossimi candidati quando la UI operativa si stabilizza:
- card categoria mensile;
- card posizione denaro;
- badge stato mese;
- righe monetarie;
- empty/error state.

## 21. Edge case checklist
- duplicato categoria con differenze solo di case/spazi -> rifiutato;
- categoria archiviata con stesso nome -> riattivare, non duplicare;
- categoria archiviata resta valida nello storico;
- delete con soldi -> bloccare e riallocare;
- delete con storico -> preservare significato storico;
- opening balance mancante -> 0;
- opening balance suggerito discrepante -> modificabile;
- allocazioni oltre risorse -> blocco;
- posizioni oltre risorse -> blocco;
- posizioni sotto risorse -> consentito con "Da posizionare";
- stesso denaro su due posizioni -> modello non valido;
- rientro in Casa dalla bottom bar -> Pianificazione;
- mese precedente OPEN -> blocco nuovo mese;
- mese CLOSED -> niente modifiche ordinarie;
- categoria non usata nel mese -> nascondi/escludi, non delete globale;
- rotazione con form/sheet aperto -> stato conservato dal ViewModel;
- tastiera landscape -> contenuto sheet raggiungibile e scrollabile.

## 22. Piano di lavoro per chiudere Pianificazione v1
Ordine raccomandato:

### P1 - Stato mese e regole temporali
1. aggiungere HouseMonthStatus OPEN/CLOSED e closedAt a house_months;
2. migrare/versionare Room;
3. esporre status nel dominio/repository;
4. bloccare creazione del mese successivo se il precedente e' OPEN.

### P2 - Correggere ingresso Casa
1. quando si seleziona Casa dalla bottom navigation, atterrare sempre su Pianificazione;
2. definire reset della navigazione interna Casa senza perdere stato persistente.

### P3 - Validazioni planner
1. errori live per allocazioni > risorse;
2. errori live per posizioni > risorse;
3. Salva disabilitato in stato invalido;
4. stessi check nel repository.

### P4 - Opening balance suggerito
1. repository per recuperare il mese precedente CLOSED;
2. sorgente del residuo finale;
3. fallback 0;
4. valore modificabile.

Nota: finche' non esiste un residuo finale affidabile, il sistema usa 0 e non inventa dati.

### P5 - Pianificazione mese esistente: schermata operativa
1. mostra stato mese e risorse;
2. mostra riepilogo Allocato/Da allocare;
3. mostra tutte le categorie del mese come card;
4. mostra opening/allocated/totale;
5. mostra posizione attuale dei soldi;
6. aggiungi Modifica pianificazione;
7. aggiungi Modifica posizioni;
8. aggiungi Chiudi mese come azione futura/inizialmente preparata.

### P6 - Modifica pianificazione esistente
1. precompila dati salvati;
2. modifica risorse/allocazioni/opening/note;
3. salvataggio atomico;
4. protezione mese CLOSED.

### P7 - Modifica posizioni esistenti
1. precompila saldi correnti;
2. permette aggiornamenti durante il mese;
3. validazione totale;
4. salvataggio atomico;
5. prepara futura integrazione con movimenti espliciti.

### P8 - Nascondi categoria dal mese
1. decidere rappresentazione dati;
2. UI per includi/nascondi;
3. non alterare Personalizzazione;
4. mantenere storico mensile.

### P9 - Chiusura mese minima
1. definire dati necessari al residuo finale;
2. stato CLOSED;
3. closedAt;
4. blocco modifiche ordinarie;
5. preparazione opening del mese successivo.

La gestione completa di spese, movimenti e split dei residui puo' essere una milestone successiva, ma la chiusura minima deve avere semantica coerente prima di abilitare il mese successivo.

### P10 - Navigazione mesi
Solo dopo P1-P9:
1. selettore precedente/successivo;
2. storico;
3. regole mesi futuri;
4. gestione buchi/mesi non creati.
