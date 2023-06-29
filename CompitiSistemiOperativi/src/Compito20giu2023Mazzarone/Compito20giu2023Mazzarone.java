package Compito20giu2023Mazzarone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Compito20giu2023Mazzarone {
    public static void main(String[] args) throws InterruptedException {
        int n = 10, m = 5, l = 20, t = 100, d = 400, x = 500;
        Worker[] workers = new Worker[m];
        Generator[] generators = new Generator[n];
        OutputThread[] outputThreads = new OutputThread[2];
        CodaLimitata codaLim = new CodaLimitata(l);
        OutputMng outputMng = new OutputMng(m);
        for ( int i = 0; i < generators.length; i++ ){
            generators[i] = new Generator( i, codaLim, x );
            generators[i].setName( "G"+i );
            generators[i].start();
        }
        for ( int i = 0; i < workers.length; i++ ){
            workers[i] = new Worker( i, t, d, n, codaLim, outputMng );
            workers[i].setName( "W"+i );
            workers[i].start();
        }

        //* nel compito scritto mi sono dimenticato di far partire gli output thread
        for ( int i = 0; i < outputThreads.length; i++ ){
            outputThreads[i] = new OutputThread( workers, outputMng );
            outputThreads[i].setName( "O"+i );
            outputThreads[i].start();
        }
        Thread.sleep(10000);
        int totMess = 0;
        for( Generator g:generators ){
            g.interrupt();
            g.join();
            System.out.print(g.getName()+" ne ha generati "+g.nMess+"; ");
            totMess += g.nMess;
        }
        System.out.println();
        System.out.println("per un totale di: "+totMess);

        for ( Worker w:workers ){
            w.interrupt();
            w.join();
            System.out.print(w.getName()+" ne ha elaborati "+w.nEl+"; ");
        }
        System.out.println();

        System.out.println("numero messaggi pronti per la stampa: "+outputMng.piene.availablePermits());    //* ho aggiunto questa stampa
                                                                                                            //* per verificare che il numero di messaggi rimasti in cosa all fine
                                                                                                            //* fosse coerente col numero di risultati elaborati dai worker
        for ( OutputThread o:outputThreads ){
            o.interrupt();
            o.join();
            System.out.print(o.getName()+" ha fatto "+o.nStampe+" stampe ");
        }
    }
}

class Messaggio{
    int idG, value;

    public Messaggio(int idG, int value) {
        this.idG = idG;
        this.value = value;
    }
}
class CodaLimitata{
    Semaphore mutex = new Semaphore(1);
    Semaphore libere;                           //* numero di posizioni libere nella coda
    Semaphore piene = new Semaphore(0); //* numero di messaggi disponibili nella coda

    List<Messaggio> messaggi = new ArrayList<>();

    public CodaLimitata(int l){
        libere = new Semaphore(l);
    }

    public void caricaMessaggio(Messaggio msg) throws InterruptedException {
        libere.acquire();
        mutex.acquire();

        messaggi.add(msg);

        mutex.release();
        piene.release();
    }

    public Messaggio[] prendiMessaggi(int n) throws InterruptedException {
        Messaggio[] msg = new Messaggio[n];         //* nello scritto ho fatto un errore di sintassi,
                                                    //* scrivendo Messaggio msg[] = ...
        piene.acquire(n);                           //* un worker aspetta che ci siano almeno n messaggi
        mutex.acquire();

        for (int i = 0; i<n; i++)
            msg[i] = messaggi.remove(0);      //* rimuove i primi n messaggi dalla coda

        mutex.release();
        libere.release(n);                          //* libera n permessi: incrementa il numero di posizioni libere di n
        return msg;
    }
}

class OutputMng{
    //* nel compito ho dfinito un mutex superfluo. Non è stato usato né nel compito scritto né in questo codice
    int[] results;
    Semaphore piene = new Semaphore(0); //* indica il numero di messaggi caricati

    public OutputMng(int m){
        results = new int[m];
    }
    public void caricaValore(int idW, int value){
        results[idW] = value;       //* il worker carica il valore nella sua posizione
        piene.release();
    }
    public int[] prendiValori() throws InterruptedException {
        piene.acquire(results.length);
        return results;
    }
}

class OutputThread extends Thread{
    Worker[] workers;
    OutputMng output;
    int nStampe = 0;

    public OutputThread(Worker[] workers, OutputMng output) {
        this.workers = workers;
        this.output = output;
    }

    @Override
    public void run() {
        try{
            while (true){
                int[] results = output.prendiValori();
                System.out.print(getName()+": [ ");     //* ho aggiunto delle stampe in più per rendere più leggibile l'output
                for (int i = 0; i<results.length; i++){
                    workers[i].viaLibera.release();
                    System.out.print(results[i]+" ");
                }
                System.out.println("]");
                nStampe++;
            }
        }catch (InterruptedException e){}
    }
}

class Worker extends Thread{
    Semaphore viaLibera = new Semaphore(1);
    int id, nEl = 0, t, d, n;
    CodaLimitata codaLim;
    OutputMng output;

    public Worker(int id, int t, int d, int n,
                  CodaLimitata codaLim, OutputMng output) {
        this.id = id;
        this.t = t;
        this.d = d;
        this.n = n;
        this.codaLim = codaLim;
        this.output = output;
    }

    @Override
    public void run() {
        try {
            while (true){
                Messaggio[] msg = codaLim.prendiMessaggi(n);
                int sum = 0;
                for (Messaggio m : msg)
                    sum += m.value;
                sleep( ( int ) ( Math.random() * ( t + d ) + t ) );
                nEl ++;
                viaLibera.acquire();        //* aspetta che uno degli OutPutThread avverta che
                                            //* il risultato precedente è stato prelevato
                output.caricaValore(id, sum);
            }
        } catch (InterruptedException e) {}
    }
}

class Generator extends Thread{
    CodaLimitata codaLim;
    int id, serie = 1, nMess = 0, x;

    public Generator(int id, CodaLimitata codaLim, int x) {
        this.codaLim = codaLim;
        this.id = id;
        this.x = x;
    }

    @Override
    public void run() {
        try{
            while (true){
                codaLim.caricaMessaggio( new Messaggio( id, serie++ ) );
                nMess++;
                sleep(x);
            }
        }catch (InterruptedException e){}
    }
}