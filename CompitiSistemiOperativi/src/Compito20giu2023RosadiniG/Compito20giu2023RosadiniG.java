package Compito20giu2023RosadiniG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class Compito20giu2023RosadiniG{
    public static void main(String[] args) throws InterruptedException {
        int n = 10;     //* n° di Generators
        int m = 5;      //* n° di Workers
        int l = 20;     //* dimensione coda limitata (nel compito ho messo 4, ma è troppo piccola, il Worker si aspetta n messaggi)
        int x = 100;    //* tempo di attesa tra la generazione di un messaggio e l'altro
        int t = 100;    //* tempo minimo di elaborazione
        int d = 900;    //* incremento massimo del tempo di elaborazione

        //* creazione risorse condivise
        LimQueue lq = new LimQueue(l, n);
        OutputMng om = new OutputMng(m);

        //* generazione e start() dei vari Threads
        GeneratorThread[] generators = new GeneratorThread[n];
        for( int i = 0; i < generators.length; i++ ){
            generators[i] = new GeneratorThread(lq, i, x);
            generators[i].start();
        }
        WorkerThread[] workers = new WorkerThread[m];
        for( int i = 0; i < workers.length; i++ ){
            workers[i] = new WorkerThread(lq, om, i, n, t, d);
            workers[i].start();
        }
        OutputThread[] ot = new OutputThread[2];
        for( int i = 0; i < ot.length; i++){
            ot[i] = new OutputThread(om, m);
            ot[i].setName("OutputT"+i);
            ot[i].start();
        }

        Thread.sleep(10000); //* si attendono 10 secondi

        int totGen = 0; //* variabile per totale di messaggi generati dai Threads

        //*interruzione di tutti i Threads e stampa delle statistiche
        System.out.println();
        for( int i = 0; i < generators.length; i++ ){
            generators[i].interrupt();
            generators[i].join();
            System.out.println("G"+i+" ha generato "+generators[i].nGen+" messaggi. ");
            totGen += generators[i].nGen;
        }
        System.out.println("Tot messaggi generati:"+totGen);

        System.out.println();
        for( int i = 0; i < workers.length; i++ ){
            workers[i].interrupt();
            workers[i].join();
            System.out.println("W"+i+" ha elaborato "+workers[i].nWork+" risultati. ");
        }
        System.out.println();
        System.out.println("numero messaggi rimasti in coda: "+om.piene.availablePermits());
        for( int i = 0; i < ot.length; i++){
            ot[i].interrupt();
            ot[i].join();
            System.out.print("OutputThread"+i+" ha stampato "+ot[i].nPrints+" volte. ");
        }
    }
}

class Message{
    int idG;
    int value;
    public Message(int idG, int value){
        this.idG = idG;
        this.value = value;
    }
}

class LimQueue{
    int n;
    Semaphore mutex = new Semaphore(1);
    Semaphore piene = new Semaphore(0);
    Semaphore vuote;
    ArrayList<Message> queue = new ArrayList<>();
    public LimQueue(int l, int n){
        vuote = new Semaphore(l);
        this.n = n;
    }
    public void putMessage(Message m) throws InterruptedException{
        vuote.acquire();        //* decremento una posizione vuota nella coda
        mutex.acquire();        //* attendo il mutex
        queue.add(m);           //* aggiungo il messaggio m alla coda
        mutex.release();        //* rilascio mutex
        piene.release();        //* incremento una posizione piena
    }

    public Message[] getNMessages() throws InterruptedException{
        piene.acquire(n);                       //* attendo che n posizioni siano piene
        mutex.acquire();
        //* ERRORE: nel compito mi sono dimenticato di scorrere tutto l'array e rimuovere valore per valore
        Message[] temp = new Message[n];        //* creo un array di n Message
        for(int i = 0; i < n; i++ )             //* scorro tutto l'array
            temp[i] = queue.remove(0);    //* copio valore per valore
        mutex.release();                        //* rilascio mutex
        vuote.release(n);                       //* incremento n posizioni vuote
        return temp;
    }
}

class OutputMng {
    int[] results;
    int m;
    //* nel compito ho incluso l'attributo idW, che non serve, perchè il worker, che userà putResult, conosce già il suo id
    Semaphore mutex = new Semaphore(1);
    //* ogni volta che un worker inserisce un risultato, incrementa una posizione piena
    //* poi, l'OutPutThread, attende m risultati, preleva tutto l'array e decrementa m posizioni piene
    Semaphore piene = new Semaphore(0);
    //* nel compito mi sono scordato di rimuovere il semaforo vuote che non serve
    //* MODIFICA: ho aggiunto un array di semafori per gestire l'attesa del worker per inserire il risultato in OutputMng
    Semaphore[] position;  //* array di semafori per gestire l'attesa dei Worker
    public OutputMng(int m){
        results = new int[m];
        this.m = m;
        position = new Semaphore[m];
        for(int i = 0; i < m; i++){
            position[i] = new Semaphore(1);
        }
    }
    public void putResult(int r, int idW) throws InterruptedException{
        //* MODIFICA: nel compito ho gestito questo metodo con una condizione sull'array di result != null;
        //* in questa revisione ho preferito gestire questa attesa con un array di semaforiin cui ogni worker controlla
        //* se il semaforo relativo alla sua posizione è libero, questo perchè l'OutputThread, una volta che ha prelevato
        //* l'array di risultati, li rilascerà tutti quanti assieme.
        position[idW].acquire();    //* attende se l'array non è stato svuotato da OutputThread e se la sua posizione è ancora occupata
        mutex.acquire();            //* attende il mutex
        results[idW] = r;           //* inserisce il risultato in posizione corrispondente al suo id
        mutex.release();            //* rilascia il mutex
        piene.release();            //* incrementa una posizione piena
    }
    public int[] getResults() throws InterruptedException{
        piene.acquire(m);           //* attendo che m posizioni siano piene
        mutex.acquire();            //* attendo il mutex
        int[] temp = results;       //* copio l'array di risultati
        results = new int[m];       //* inizializza l'array result a null
        mutex.release();            //* rilascio il mutex
        for (Semaphore semaphore : position) {  //* unica aggiunta rispetto al compito
            semaphore.release();    //* rilascio tutti i semafori relativi alle posizioni dell'array corrispondenti agli id del worker
        }
        return temp;
    }
}
class GeneratorThread extends Thread{
    LimQueue lq;
    int idG;
    int value = 1;  //* valore iniziale dei messaggi, continueranno ad incrementarsi in ordine progressivo
    int nGen = 0;   //* numero messaggi generati per stampa finale
    int x;          //* tempo di attesa tra un messaggio e l'altro
    public GeneratorThread(LimQueue lq, int idG, int x) {
        this.lq = lq;
        this.idG = idG;
        this.x = x;
    }
    public void run(){
        try{
            while(true){
                Message m = new Message(idG, value);
                value++;
                nGen++;             //* nel compito mi sono scordato di incrementare nGen
                lq.putMessage(m);   //* incremento nGen e inserisco il messaggio nella coda
                sleep(x);
            }
        }catch (InterruptedException e) {}
    }
}

class WorkerThread extends Thread{
    LimQueue lq;
    OutputMng om;
    int idW;
    int n;              //* numero di messaggi da prelevare dalla coda
    int nWork = 0;      //* numero di messaggi prelevati per stampa finale
    int t;              //* tempo minimo di attesa tra un messaggio e l'altro
    int d;              //* massimo incremento del tempo di elaborazione
    public WorkerThread(LimQueue lq, OutputMng om, int idW, int n, int t, int d) {
        this.lq = lq;
        this.om = om;
        this.idW = idW;
        this.n = n;
        this.t = t;
        this.d = d;
    }
    public void run() {
        try{
            while(true){
                Message[] mm = lq.getNMessages();   //* prelevo N messaggi dalla LimQueue
                int result = 0;                     //* inizializzo il risultato a 0
                for(Message m : mm)
                    result += m.value;              //* sommo tutti i value
                sleep((int)(Math.random()*d+t));    //* nel compito mi sono dimenticato una parentesi dopo il cast
                nWork++;
                //* nel compito ho incrementato nWork successivamente a PutResult, ma penso che sia più giusto farlo prima,
                //* in quanto il messaggio è già stato elaborato prima di essere messo in OutputMng.

                //* Adesso il Worker deve attendere se il risultato messo precedentemente non è stato ancora acquisito dall'OT
                om.putResult(result, idW);
            }
        }catch(InterruptedException e){}
    }
}

class OutputThread extends Thread{
    OutputMng om;
    int[] results;                     //* array di risultati
    int nPrints = 0;                   //* numero di volte che ha stampato l'array
    public OutputThread(OutputMng om, int m){
        this.om = om;
        results = new int[m];
    }
    public void run() {
        try{
            while(true){
                results = om.getResults();
                //* nel compito è stato scritto male il metodo toString() per mancanza di spazio nel foglio
                System.out.println(getName()+" stampa l'array: "+Arrays.toString(results));
                nPrints++;
            }
        }catch (InterruptedException e ){}
    }
}