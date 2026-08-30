# MyFinances - Specifiche di Progetto v4

Data: 30 agosto 2026
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

## 5. Pianificazione mensile Casa
Esiste un solo piano Casa per coppia anno/mese.

Ogni mese contiene:
- risorse totali del mese;
- stato del mese OPEN/CLOSED;
- nota opzionale;
- allocazioni mensili per categoria;
- posizione fisica corrente del denaro Casa sui money account.

### 5.1 Stato OPEN/CLOSED
Lo stato di chiusura appartiene al singolo mese e deve essere salvato in house_months, non in AppPreferences/DataStore.

Stati iniziali:
- OPEN: mese operativo e modificabile;
- CLOSED: mese chiuso e storico.

Campi previsti:
- status: HouseMonthStatus;
- closedAt: Long?;

Regola di sequenza: non si pianifica normalmente un mese se il mese precedente esiste ed e' ancora OPEN. Il primo mese mai creato costituisce l'eccezione naturale.

La procedura di chiusura mese sara' responsabile di consolidare i residui finali prima della pianificazione del mese successivo.

## 6. Allocazioni per categoria
Per ogni categoria del mese:
- openingBalanceCents: denaro gia' presente nella categoria all'inizio del mese;
- allocatedCents: nuove risorse assegnate in quel mese.

Derivati:
- categoryTotalCents = openingBalanceCents + allocatedCents;
- allocatedThisMonthCents = somma allocatedCents;
- unallocatedCents = totalResourcesCents - allocatedThisMonthCents.

openingBalanceCents non consuma nuovamente le risorse del mese corrente.

### 6.1 Precompilazione opening balance
Quando viene creato un nuovo mese, openingBalanceCents deve essere precompilato dal residuo finale consolidato della stessa categoria nel mese precedente chiuso.

Se il dato non esiste, valore suggerito = 0.

Il valore resta sempre modificabile durante la creazione del nuovo piano per correggere discrepanze rispetto alla situazione reale.

Finche' la chiusura mese e il tracciamento spese non forniscono residui affidabili, il sistema non deve inventare valori: fallback a 0.

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

Esempio non valido:
- risorse 2000 EUR;
- nuove allocazioni 2100 EUR;
- da allocare -100 EUR -> salvataggio bloccato.

## 8. Posizione fisica corrente del denaro Casa
Le posizioni fisiche sono saldi correnti mutuamente esclusivi, non una cronologia dei passaggi effettuati.

Esempio:
- totale Casa: 2000 EUR;
- Libretto: 600 EUR;
- Quaderno: 400 EUR;
- Online: 1000 EUR;
- totale posizionato: 2000 EUR.

Se 1000 EUR vengono spostati dal Quaderno alla carta Online, non si aggiungono 1000 EUR al totale: Quaderno diminuisce di 1000 EUR e Online aumenta di 1000 EUR.

La cronologia del movimento e' un concetto separato e futuro.

Durante una modifica e' consentita una situazione incompleta con somma posizioni inferiore alle risorse; la UI mostra "Da posizionare". Non e' consentito superare le risorse.

Dopo la creazione del piano le posizioni devono rimanere modificabili, perche' il denaro puo' essere prelevato o spostato durante il mese.

## 9. Pianificazione come schermata operativa
Quando esiste un mese OPEN, Pianificazione non mostra soltanto un riepilogo generale. Deve diventare la schermata operativa del mese e mostrare almeno:
- mese e stato OPEN/CLOSED;
- risorse totali;
- allocato nel mese;
- da allocare;
- categorie del mese come card coerenti;
- posizione attuale dei soldi;
- azione Modifica pianificazione;
- azione Modifica posizioni;
- azione Chiudi mese.

### 9.1 Card categoria
Ogni categoria del mese sara' una card standardizzata.

Nella prima versione mostra almeno:
- nome;
- opening balance;
- nuova allocazione;
- totale disponibile.

Quando verranno introdotti i movimenti/spese mostrera' anche:
- entrate/uscite rilevanti;
- speso;
- residuo corrente.

Click futuro sulla card -> dettaglio categoria del mese con movimenti e azioni come:
- aggiungi uscita;
- aggiungi entrata;
- rettifica/correzione situazione.

Le proprieta' globali della categoria (nome, tipo, target, archivio, ordine) restano in Personalizzazione.

### 9.2 Categoria nascosta dal mese
Una categoria globale puo' essere esclusa/nascosta da uno specifico mese senza eliminarla o archiviarla globalmente.

Questa e' un'operazione mensile distinta da delete/archive.

## 10. Eliminazione futura di categorie e posizioni
Archiviazione e cancellazione sono concetti distinti.

Per una cancellazione definitiva futura:
- se esiste denaro logicamente associato alla categoria, l'eliminazione deve essere bloccata;
- deve aprirsi un flusso che richiede di riallocare/spostare prima tutto il denaro residuo;
- i riferimenti storici non devono diventare incoerenti o perdere significato.

La strategia tecnica per preservare lo storico in caso di delete reale resta da definire prima dell'implementazione.

## 11. Posizioni: dettaglio e storico futuro
La sezione Posizione attuale dei soldi sara' rappresentata con card coerenti.

Click futuro -> dettaglio/modifica delle posizioni e storico del mese.

L'evoluzione definitiva preferita e' basata su movimenti espliciti:
- Da: Quaderno
- A: Online
- Importo: 1000 EUR

Il sistema aggiorna atomicamente i due saldi. Le correzioni manuali restano un'azione esplicita separata.

## 12. Navigazione tra mesi
Pianificazione dovra' permettere la navigazione verso mesi precedenti e successivi.

La funzionalita' e' pianificata ma le condizioni complete per il mese futuro devono essere definite dopo il completamento del flusso OPEN/CLOSED. Vincolo gia' deciso: un mese successivo non deve essere pianificabile se il precedente esiste ed e' ancora OPEN.

## 13. Dashboard vs Pianificazione
Principio:
- Pianificazione = luogo operativo per gestire Casa;
- Dashboard = sintesi cross-app per capire rapidamente la situazione.

La Dashboard non deve duplicare tutti gli strumenti di modifica di Casa.

### 13.1 Disponibile da spendere
"Disponibile da spendere" nella Dashboard indica il denaro personale realmente libero dell'utente, non le risorse Casa non allocate.

Formula concettuale personale:
physical personal card balance - household money on card - personal savings - bill reserves - other commitments.

### 13.2 Casa in Dashboard
Per Casa usare etichette non ambigue:
- Risorse Casa;
- Da allocare = risorse nuove del mese non assegnate ad alcuna categoria;
- residui delle categorie = denaro ancora presente nelle singole categorie dopo i movimenti/spese;
- stato mese OPEN/CLOSED;
- sintesi categorie principali/evidenziate;
- sintesi posizione fisica del denaro Casa.

La definizione di "categoria principale" verra' implementata in futuro, probabilmente come preferenza esplicita della categoria insieme all'ordinamento personalizzato.

## 14. Personale
I salvadanai personali sono riserve logiche, anche quando il denaro si trova fisicamente sulla stessa carta.

KPI centrale: disponibile personale realmente spendibile dopo tutte le riserve e gli impegni.

## 15. Bollette
Le bollette ricorrenti/pianificate sono distinte dai fondi accantonati per pagarle.

Ogni occorrenza puo' avere stato pianificato, riservato, pagato, posticipato o saltato.

## 16. Architettura Android
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

I salvataggi che coinvolgono mese, allocazioni e posizioni devono essere atomici tramite transazione Room.

## 17. Componenti UI condivisi
Senza anticipare il design system definitivo, i comportamenti strutturali comuni vengono centralizzati subito.

Componenti gia' introdotti:
- AppScreen: safe insets e struttura base delle schermate;
- AppModalBottomSheet: comportamento comune, portrait/landscape, IME, scroll e azioni.

Le card operative verranno costruite con componenti condivisi quando il modello delle schermate sara' sufficientemente stabile.

## 18. Roadmap cloud
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

## 19. Decisioni rimandate
Da definire quando la feature relativa viene affrontata:
- UX completa di chiusura mese e riallocazione residui;
- navigazione mese precedente/successivo e vincoli sui mesi futuri;
- movimenti/spese per categoria;
- storico dettagliato delle posizioni;
- hard delete con preservazione storico;
- categoria principale/evidenziata Dashboard;
- riordino categorie;
- riordino money account;
- design system visivo definitivo.
