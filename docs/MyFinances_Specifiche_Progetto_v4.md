# MyFinances - Specifiche di Progetto v4

Data: 6 settembre 2026
Stato: specifica corrente per Step 1 locale

## 1. Obiettivo
MyFinances e' un'app Android personale per pianificare e controllare finanze Casa, finanze personali e bollette. L'app deve separare sempre:
- il luogo fisico in cui il denaro si trova;
- lo scopo logico a cui il denaro e' destinato.

Questa separazione e' fondamentale perche' uno stesso conto o carta puo' contenere denaro appartenente a domini logici diversi.

## 2. Navigazione principale
La bottom navigation contiene esattamente:
- Dashboard
- Casa
- Personale
- Bollette

Impostazioni e configurazioni non occupano una voce della bottom navigation.

Quando l'utente entra in Casa dalla bottom navigation, Casa deve atterrare sempre sulla tab interna Pianificazione, anche se l'ultima visita era su Personalizzazione.

## 3. Casa: struttura interna
Casa contiene due tab interne:
- Pianificazione: default all'ingresso nella sezione;
- Personalizzazione: configurazione stabile delle finanze Casa.

### 3.1 Personalizzazione
Contiene almeno:
- gestione categorie Casa;
- gestione posizioni fisiche / money account.

Le categorie possono essere:
- FLEXIBLE: categoria senza obiettivo monetario definito;
- TARGET: categoria con targetCents obbligatorio e positivo.

Le categorie devono supportare:
- creazione;
- modifica nome/tipo/target;
- archiviazione e riattivazione;
- eliminazione reale futura con protezione di denaro e storico;
- ordinamento personalizzato, da implementare;
- eventuale flag futuro per evidenziazione in Dashboard.

I nomi categoria sono unici senza distinzione tra maiuscole/minuscole e ignorando spazi esterni. Una categoria archiviata occupa comunque il nome e va riattivata invece di duplicarla.

I money account descrivono contenitori fisici generali e modificabili dall'utente, ad esempio:
- Contanti / Quaderno;
- Carta / Online;
- Conto o libretto;
- Revolut o altri futuri contenitori.

Tipi strutturali:
- CASH
- BANK_ACCOUNT
- CARD
- OTHER

## 4. Setup Casa
Lo stato iniziale di configurazione Casa e' persistito in DataStore con isHouseSetupCompleted.

Questo flag significa che l'utente ha completato intenzionalmente il setup, non che esistono semplicemente righe nel database.

Il setup puo' essere completato solo se esistono almeno:
- una categoria attiva;
- un money account attivo.

Dopo il completamento, la disponibilita' di una nuova pianificazione dipende comunque dalla presenza corrente di almeno una categoria attiva e un money account attivo. Un piano gia' esistente resta consultabile anche se in seguito tutte le entita' vengono archiviate.

## 5. Pianificazione mensile Casa
Esiste un solo piano Casa per coppia anno/mese.

Ogni mese contiene:
- risorse totali del mese;
- stato del mese OPEN/CLOSED;
- nota opzionale;
- allocazioni mensili per categoria;
- Disponibile Casa;
- posizione fisica corrente del denaro Casa sui money account.

### 5.1 Stato OPEN/CLOSED
Lo stato di chiusura appartiene al singolo mese e deve essere salvato in house_months, non in AppPreferences/DataStore.

Stati:
- OPEN: mese operativo e modificabile;
- CLOSED: mese chiuso e storico.

Campi:
- status: HouseMonthStatus;
- closedAt: Long?.

Regola di sequenza: non si pianifica normalmente un mese se il mese precedente esiste ed e' ancora OPEN. Il primo mese mai creato costituisce l'eccezione naturale.

La procedura di chiusura mese consolida i residui finali e le loro destinazioni prima della pianificazione del mese successivo.

## 6. Allocazioni per categoria e Disponibile Casa
Per ogni categoria del mese:
- openingBalanceCents: denaro gia' presente nella categoria all'inizio del mese;
- allocatedCents: nuove risorse assegnate in quel mese.

Derivati iniziali, prima dei movimenti:
- categoryTotalCents = openingBalanceCents + allocatedCents;
- allocatedThisMonthCents = somma allocatedCents;
- availableCents = totalResourcesCents - allocatedThisMonthCents.

openingBalanceCents non consuma nuovamente le risorse del mese corrente.

`Disponibile` sostituisce l'etichetta `Da allocare`: e' liquidita' Casa non vincolata a categorie e non deve necessariamente essere allocata. Quando verranno introdotti i movimenti, potra' avere entrate/uscite proprie, ad esempio una spesa non attribuita a nessuna categoria.

Formula futura concettuale:
- Disponibile corrente = risorse - allocazioni - uscite dal Disponibile + entrate/rettifiche del Disponibile.

Il Disponibile non deve essere modellato come una categoria fittizia.

### 6.1 Precompilazione opening balance
Quando viene creato un nuovo mese, openingBalanceCents deve essere precompilato dalla distribuzione dei residui registrata nella chiusura del mese precedente.

Per ogni categoria di destinazione, l'opening del nuovo mese e' la somma di tutti i trasferimenti di chiusura diretti a quella categoria, indipendentemente dalla categoria sorgente.

Se il dato non esiste, valore suggerito = 0.

Il valore resta sempre modificabile durante la creazione del nuovo piano per correggere discrepanze rispetto alla situazione reale.

Un importo destinato al Disponibile in chiusura non diventa opening di alcuna categoria: confluisce invece nel Disponibile del mese successivo secondo il modello dati della chiusura.

## 7. Validazioni monetarie
Tutti gli importi monetari sono Long in centesimi.

Invarianti principali:
- totalResourcesCents >= 0;
- openingBalanceCents >= 0;
- allocatedCents >= 0;
- amountCents delle posizioni >= 0;
- somma allocatedCents <= totalResourcesCents;
- somma posizioni fisiche <= totalResourcesCents.

Se una modifica porta un derivato sotto zero, la UI deve mostrare subito l'errore e disabilitare il salvataggio. Il repository deve ripetere la stessa validazione prima di scrivere su Room.

## 8. Posizione fisica corrente del denaro Casa
Le posizioni fisiche sono saldi correnti mutuamente esclusivi, non una cronologia dei passaggi effettuati.

Esempio:
- totale Casa: 2000 EUR;
- Libretto: 600 EUR;
- Quaderno: 400 EUR;
- Online: 1000 EUR;
- totale posizionato: 2000 EUR.

Se 1000 EUR vengono spostati dal Quaderno alla carta Online, non si aggiungono 1000 EUR al totale: Quaderno diminuisce di 1000 EUR e Online aumenta di 1000 EUR.

La cronologia del movimento e' un concetto separato.

Durante una modifica e' consentita una situazione incompleta con somma posizioni inferiore alle risorse; la UI mostra "Da posizionare". Non e' consentito superare le risorse.

Dopo la creazione del piano le posizioni devono rimanere modificabili.

## 9. Pianificazione come schermata operativa
Quando esiste un mese, Pianificazione mostra almeno:
- mese e stato OPEN/CLOSED;
- risorse totali;
- allocato nel mese;
- Disponibile;
- categorie del mese come card coerenti;
- posizione attuale dei soldi;
- azione Modifica pianificazione su OPEN;
- azione Modifica posizioni su OPEN;
- azione Chiudi mese su OPEN.

### 9.1 Card categoria
Ogni categoria del mese e' una card standardizzata.

Prima dei movimenti mostra almeno:
- nome;
- opening balance;
- nuova allocazione;
- totale disponibile teorico.

Con i movimenti mostrera' anche:
- entrate/uscite;
- speso;
- residuo corrente.

Click futuro sulla card -> dettaglio categoria del mese con movimenti e azioni:
- aggiungi uscita;
- aggiungi entrata;
- rettifica/correzione situazione.

Le proprieta' globali della categoria restano in Personalizzazione.

### 9.2 Categoria nascosta dal mese
Una categoria globale puo' essere esclusa/nascosta da uno specifico mese senza eliminarla o archiviarla globalmente.

Questa e' un'operazione mensile distinta da delete/archive.

## 10. Chiusura mese e residui
La chiusura e' disponibile solo per un mese OPEN.

Per ogni categoria vengono distinti:
- saldo calcolato dall'app;
- saldo finale confermato manualmente dall'utente;
- adjustment/rettifica = saldo confermato - saldo calcolato;
- eventuale nota di rettifica.

Il saldo finale confermato resta SEMPRE modificabile anche quando i movimenti saranno completi, per gestire uscite dimenticate, errori e discrepanze reali.

L'app non deve sovrascrivere lo storico per far tornare i conti: deve conservare saldo calcolato, saldo confermato e differenza.

Ogni residuo finale confermato deve essere distribuito completamente. Destinazioni supportate nella prima versione:
- mantenere tutto o parte nella stessa categoria per il mese successivo;
- spostare tutto o parte in un'altra categoria;
- dividere il residuo tra piu' categorie;
- spostare tutto o parte nel Disponibile del mese successivo.

Non vengono introdotti per ora Fondi Casa separati: non esiste ancora una semantica abbastanza diversa da una normale categoria da giustificarli.

Invariante della distribuzione per ogni categoria sorgente:
- somma destinazioni = saldo finale confermato;
- mai inferiore;
- mai superiore;
- nessun importo negativo.

Una distribuzione verso la stessa categoria e' tecnicamente un normale trasferimento categoria -> stessa categoria; "Mantieni" e' solo una scorciatoia UX.

La chiusura salva atomicamente:
- dati di conferma/rettifica delle categorie;
- distribuzioni dei residui;
- eventuale quota destinata al Disponibile successivo;
- status = CLOSED;
- closedAt.

Un mese CLOSED non viene modificato dai flussi ordinari. Eventuale riapertura futura richiedera' un flusso esplicito.

## 11. Eliminazione futura di categorie e posizioni
Archiviazione e cancellazione sono concetti distinti.

Per una cancellazione definitiva futura:
- se esiste denaro logicamente associato alla categoria, l'eliminazione deve essere bloccata;
- deve aprirsi un flusso che richiede di riallocare/spostare prima tutto il denaro residuo;
- i riferimenti storici non devono diventare incoerenti o perdere significato.

La strategia tecnica per preservare lo storico in caso di delete reale resta da definire prima dell'implementazione.

## 12. Posizioni: dettaglio e storico futuro
La sezione Posizione attuale dei soldi sara' rappresentata con card coerenti.

Click futuro -> dettaglio/modifica delle posizioni e storico del mese.

L'evoluzione definitiva preferita e' basata su movimenti espliciti:
- Da: Quaderno
- A: Online
- Importo: 1000 EUR.

Il sistema aggiorna atomicamente i due saldi. Le correzioni manuali restano un'azione esplicita separata.

## 13. Navigazione tra mesi
Pianificazione deve permettere la navigazione verso mesi precedenti e successivi.

Ordine di implementazione deciso:
1. chiusura mese completa;
2. autocompletamento del mese successivo;
3. navigazione mesi;
4. movimenti categorie/Disponibile;
5. movimenti posizioni;
6. rifiniture Personalizzazione.

Vincolo: un mese successivo non deve essere pianificabile se il precedente esiste ed e' ancora OPEN.

## 14. Dashboard vs Pianificazione
Principio:
- Pianificazione = luogo operativo per gestire Casa;
- Dashboard = sintesi cross-app per capire rapidamente la situazione.

La Dashboard non deve duplicare tutti gli strumenti di modifica di Casa.

### 14.1 Disponibile da spendere
"Disponibile da spendere" nella Dashboard indica il denaro personale realmente libero dell'utente, non il Disponibile Casa.

Formula concettuale personale:
physical personal card balance - household money on card - personal savings - bill reserves - other commitments.

### 14.2 Casa in Dashboard
Per Casa usare etichette non ambigue:
- Risorse Casa;
- Disponibile Casa = liquidita' Casa non vincolata a categorie;
- residui delle categorie = denaro ancora presente nelle singole categorie dopo i movimenti/spese;
- stato mese OPEN/CLOSED;
- sintesi categorie principali/evidenziate;
- sintesi posizione fisica del denaro Casa.

## 15. Analisi & Suggerimenti - requisito mandatorio futuro
Dopo il completamento funzionale di Casa e prima di considerare stabile il dominio, verra' eseguito un audit dedicato di schema, repository e flussi per garantire che siano conservati i dati necessari all'analisi storica.

La futura sezione Analisi & Suggerimenti non e' considerata opzionale. L'implementazione UI viene dopo Casa, ma il modello dati deve essere progettato fin da ora per renderla possibile senza perdere informazione.

Principio:
- dato grezzo = fatti registrati;
- indicatore = misura derivata dai fatti;
- suggerimento = interpretazione dell'indicatore.

Esempi di dati grezzi da conservare quando disponibili:
- allocazioni mensili;
- opening balance;
- singoli movimenti/entrate/uscite;
- movimenti tra posizioni;
- saldo calcolato in chiusura;
- saldo finale confermato;
- rettifica di chiusura e nota;
- distribuzioni dei residui;
- stato e data di chiusura.

Esempi di indicatori futuri:
- frequenza e valore medio degli sforamenti;
- categorie sistematicamente sottostimate;
- categorie sistematicamente sovrastimate;
- residuo medio per categoria;
- frequenza delle rettifiche manuali;
- categorie quasi mai utilizzate;
- categorie che ricevono spesso trasferimenti da altre;
- categorie che potrebbero essere accorpate o scorporate;
- stabilita' dell'allocazione nel tempo;
- uso e consumo del Disponibile Casa.

Esempi di suggerimenti futuri:
- aumentare il budget di una categoria spesso insufficiente;
- ridurlo se produce residui sistematici;
- rivedere una categoria raramente utilizzata;
- separare una categoria troppo generica;
- accorpare categorie con uso scarso o sovrapposto;
- migliorare la registrazione delle spese se le rettifiche sono frequenti.

I suggerimenti devono essere presentati come indicazioni basate sui dati, non come verita' o obblighi.

Architettura candidata:
Room -> Repository -> Analytics/Insight domain layer -> Indicatori -> Suggerimenti -> UI Analisi.

Gli insight devono essere derivati e ricalcolabili dai dati primari; evitare di salvare come fonte di verita' valori aggregati che possono essere ricostruiti.

## 16. Personale
I salvadanai personali sono riserve logiche, anche quando il denaro si trova fisicamente sulla stessa carta.

KPI centrale: disponibile personale realmente spendibile dopo tutte le riserve e gli impegni.

## 17. Bollette
Le bollette ricorrenti/pianificate sono distinte dai fondi accantonati per pagarle.

Ogni occorrenza puo' avere stato pianificato, riservato, pagato, posticipato o saltato.

## 18. Architettura Android
Stack Step 1:
- Kotlin;
- Jetpack Compose;
- Single Activity;
- Navigation Compose;
- ViewModel + StateFlow;
- Hilt;
- Room;
- DataStore Preferences;
- Coroutines;
- WorkManager disponibile per futuro.

Regola: Compose non accede direttamente ai DAO.

Architettura:
Compose -> ViewModel -> Repository -> Room/DataStore.

I salvataggi compositi devono essere atomici tramite transazione Room.

## 19. Componenti UI condivisi
Senza anticipare il design system definitivo, i comportamenti strutturali comuni vengono centralizzati subito.

Componenti gia' introdotti:
- AppScreen;
- AppModalBottomSheet;
- AppContentCard.

## 20. Roadmap Casa
Ordine corrente per completare Casa:
1. chiusura mese con saldo calcolato/confermato, rettifiche e distribuzione residui;
2. autocompletamento coerente della pianificazione del mese successivo;
3. navigazione tra mesi;
4. movimenti/spese delle categorie e del Disponibile;
5. movimenti e storico delle posizioni fisiche;
6. rifiniture Personalizzazione: delete sicuro, ordine, nascondi dal mese e altre mancanze gia' registrate;
7. audit finale Casa orientato ad Analisi & Suggerimenti: riesame tabelle, eventi e dati conservati; eventuali modifiche/addizioni allo schema prima di chiudere definitivamente il dominio Casa.

Solo dopo questo audit si passa all'implementazione vera e propria dell'area Analisi/Statistiche/Suggerimenti.

## 21. Roadmap cloud
Step 1: app locale completa con Room.

Step 2:
- Firebase Authentication;
- Google Sign-In;
- Cloud Firestore;
- sync offline-first;
- cifratura dei dati prima dell'uscita dal dispositivo.

Step 3:
- Household condiviso;
- account personali separati;
- dati Casa condivisi;
- privacy strutturale;
- inviti tramite codice/QR;
- chiavi crittografiche separate per dati personali e Household.

## 22. Decisioni rimandate
Da definire quando la feature relativa viene affrontata:
- UX di eventuale riapertura/correzione di un mese CLOSED;
- regole complete dei mesi futuri durante la navigazione;
- schema definitivo dei movimenti/spese;
- hard delete con preservazione storico;
- categoria principale/evidenziata Dashboard;
- riordino categorie e money account;
- design system visivo definitivo;
- algoritmo e soglie degli insight finanziari.
