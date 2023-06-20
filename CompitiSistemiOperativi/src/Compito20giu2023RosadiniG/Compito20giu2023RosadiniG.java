package Compito20giu2023RosadiniG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class Compito20giu2023RosadiniG{
    public static void main(String[] args) throws InterruptedException {
        int n = 10;
        int m = 5;
        int l = 4;
        int t = 100;
        int d = 900;
        int x = 100;

        //* risorse condivise
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
            ot[i].start();
        }

        Thread.sleep(10000); //* si attendono 10 secondi

        int totGen = 0; //* variabile per totale di messaggi generati dai Threads

        for( int i = 0; i < generators.length; i++ ){
            generators[i].interrupt();
            generators[i].join();
            System.out.print("G"+i+" ha generato "+generators[i].nGen+" messaggi. ");
            totGen += generators[i].nGen;
        }
        System.out.println();
        System.out.println("Tot messaggi generati:"+totGen);
        for( int i = 0; i < workers.length; i++ ){
            workers[i].interrupt();
            workers[i].join();
            System.out.print("W"+i+" ha elaborato "+workers[i].nWork+" risultati. ");
        }
        System.out.println();
        for( int i = 0; i < ot.length; i++){
            ot[i].interrupt();
            ot[i].join();
            System.out.print("OT"+i+" ha stampato "+ot[i].nPrints+" volte. ");
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
        vuote.acquire();    //* decremento una posizione vuota nella coda
        mutex.acquire();    //* attendo il mutex
        queue.add(m);       //* aggiungo il messaggio m alla coda
        mutex.release();    //* rilascio mutex
        piene.release();    //* incremento una posizione piena
    }

    public Message[] getNMessages() throws InterruptedException{
        piene.acquire(n);
        mutex.acquire();
        //* Errore: nel compito non ho scorso tutto l'array e copiato valore per valore
        //* ma ho fatto semplicemente:
        //* Message[] temp = queue.remove(n);
        Message[] temp = new Message[n];
        for(int i = 0; i < n; i++ )
            temp[i] = queue.remove(0);
        mutex.release();
        vuote.release(n);
        return temp;
    }
}

class OutputMng {
    int[] results;  //! provare con un array di Integer e confronto con null dentro putResult
    int m;
    int idW; //* per indirizzare l'array di results
    Semaphore mutex = new Semaphore(1);
    Semaphore piene = new Semaphore(0);
    Semaphore vuote; //* serve??
    public OutputMng(int m){
        results = new int[m];
        vuote= new Semaphore(m);
        this.m = m;
    }
    public void putResult(int r, int idW) throws InterruptedException{
        mutex.acquire();    //* attendo il mutex
        //! FIXME
        if( results[idW] != 0 ){    //* errore: nel compito ho comparato con null
            mutex.release();        //! funziona? se il result potrebbe essere = 0 no
        }else{
            results[idW] = r;
            mutex.release();
            piene.release();
        }
    }
    public int[] getResults() throws InterruptedException{
        piene.acquire(m);
        mutex.acquire();
        int[] temp = results;
        results = new int[m]; //! se li inizializza tutti a zero poi non funziona il putResult
        mutex.release();
        return temp;
    }
}

class GeneratorThread extends Thread{
    LimQueue lq;
    int idG;
    int value = 1;
    int nGen = 0;   //* numero messaggi generati
    int x;

    public GeneratorThread(LimQueue lq, int idG, int x) {
        this.lq = lq;
        this.idG = idG;
        this.x = x;
    }

    public void run(){
        try{
            while (true){
                Message m = new Message(idG, value);
                value++;
                lq.putMessage(m);
                sleep(x);
            }
        }catch (InterruptedException e) {}
    }
}

class WorkerThread extends Thread{
    LimQueue lq;
    OutputMng om;
    int idW;
    int n;
    int nWork = 0;
    int t;
    int d;

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
                Message[] mm = lq.getNMessages();
                int result = 0;
                for(Message m : mm)
                    result += m.value;              //* sommo tutti i value
                sleep((int)(Math.random()*d+t));    //* nel compito mi sono dimenticato una parentesi
                om.putResult(result, idW);
                nWork++;
            }
        }catch (InterruptedException e){}
    }
}

class OutputThread extends Thread{
    OutputMng om;
    int[] results;
    int nPrints = 0;
    public OutputThread(OutputMng om, int m){
        this.om = om;
        results = new int[m];
    }
    public void run() {
        try{
            while(true){
                results = om.getResults();
                System.out.println("Array: "+Arrays.toString(results)); //* nel compito ho scritto male il metodo per mancanza di spazio nel foglio
            }
        }catch (InterruptedException e ){}
    }
}
