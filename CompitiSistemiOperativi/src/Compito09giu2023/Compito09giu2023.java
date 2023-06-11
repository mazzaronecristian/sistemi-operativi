package Compito09giu2023;

import java.util.ArrayList;
import java.util.Arrays;

public class Compito09giu2023 {
    public static void main(String[] args) throws InterruptedException{
        int l = 4;
        int m = 1;
        int n = 10;
        int t = 2000;
        LimitedQueue limQueue = new LimitedQueue(l, m);
        ArrayQueue arrQueue = new ArrayQueue(m);

        Generator[] gen = new Generator[n];
        Worker[] work = new Worker[m];
        Printer printer = new Printer(m, arrQueue);
        for (int i = 0; i<gen.length;i++){
            gen[i] = new Generator(i, t, limQueue);
            gen[i].setName("G"+i);
            gen[i].start();
        }

        for (int i = 0; i<work.length;i++){
            work[i] = new Worker(i, limQueue, arrQueue, t);
            work[i].setName("W"+i);
            work[i].start();
        }

        printer.start();
        Thread.sleep(10000);
        for(Worker w:work){
            w.interrupt();
            w.join();
            System.out.println(w.getName()+" ha prodotto "+ w.nRes+ " risultati");
        }
        int messaggi = 0;
        for(Generator g:gen){
            g.interrupt();
            g.join();
            System.out.println(g.getName()+" ha prodotto "+ g.nMess+ " messaggi");
            messaggi += g.nMess;
        }
        System.out.println("sono stati prodotti in totale "+ messaggi+ " messaggi");
        printer.interrupt();
        printer.join();
        System.out.println("sono stati stampati "+ printer.nPrint+" array");

        int rimanenti = 0;
        for (int r: limQueue.queue){
            if(r!=0)
                rimanenti++;
        }

        System.out.println("rimangono "+ rimanenti+" messaggi");


    }
}

class LimitedQueue{
    int[] queue;
    int[] nAcquisizioni;    //* Ogni slot nella coda ha un numero di acquisizioni.
                            //* Se un Worker trova tutti zeri, sceglie quale prendere, altrimenti prende l'unico diverso da 0
    int m, nMess = 0;
    public LimitedQueue(int l, int m){
        queue = new int[l];
        nAcquisizioni = new int[l];
        for(int i = 0; i<l; i++) {
            queue[i] = 0;
            nAcquisizioni[i] = 0;
        }
        this.m = m;
    }
    public synchronized void putMessage(int message) throws InterruptedException {
        while (nMess == queue.length)
            wait();
        int pos = 0;
        while(queue[pos]!=0)
            pos++;
        queue[pos] = message;
        nMess++;
        notifyAll();
    }
    public synchronized int getMessage() throws InterruptedException {
        while(nMess == 0)
            wait();
        int pos;
        for (pos = 0; pos< nAcquisizioni.length; pos++){
            if(nAcquisizioni[pos] != 0)
                break;
        }
        if(pos == nAcquisizioni.length) {
            pos = (int) (Math.random() * nAcquisizioni.length);
            nAcquisizioni[pos]++;
            return queue[pos];
        }else{
            nAcquisizioni[pos]++;
            int value = queue[pos];
            if (nAcquisizioni[pos] == m){
                queue[pos] = 0;
                nMess--;
            }
            return value;
        }
    }

    public boolean isEmpty(){
        int sum = 0;
        for(int x: queue)
            sum += x;
        return sum == 0;
    }
}

class ArrayQueue{
    ArrayList<int[]> queue = new ArrayList<>();
    int[] singleArray;
    boolean[] caricato;     //* ogni worker ha il suo slot in caricato. caricato[i]=false se worker[i] non ha ancora caricato il messaggio
                            //* se caricato = true per ogni i, allora tutti i roekr hanno caricato lo stesso messaggio

    public ArrayQueue(int m){
        singleArray = new int[m];
        caricato = new boolean[m];
        for(int i = 0; i< m; i++){
            caricato[i] = false;
        }
    }
    public synchronized void putResults(int result, int id) throws InterruptedException {
        while(caricato[id])
            wait();
        singleArray[id] = result;
        boolean finito = true;
        for(boolean x: caricato)
            if(!x){
                finito = false;
                break;
            }

        if(finito){
            queue.add(singleArray);
            Arrays.fill(caricato, false);
        }

    }


    public synchronized int[] getResults() throws InterruptedException {
        while(queue.size() == 0)
            wait();
        return queue.remove(0);
    }
}

class Generator extends Thread{
    int id;
    int time;       //* tempo di attesa
    LimitedQueue queue;
    int nMess = 0;

    public Generator(int id, int time, LimitedQueue queue) {
        this.time = time;
        this.queue = queue;
        this.id = id;
    }
    @Override
    public void run() {
        try{
            while (true){
                int message = id * 100 + 1;
                sleep(time);
                queue.putMessage(message);
                nMess++;
                System.out.println(getName()+"carica un messaggio");
            }
        }catch (InterruptedException e){
        }
    }
}

class Printer extends Thread{
    int[] results;
    ArrayQueue queue;

    int nPrint;

    public Printer(int m, ArrayQueue queue) {
        this.results = new int[m];
        this.queue = queue;
    }

    @Override
    public void run() {
        try{
            while (true){
                results = queue.getResults();
                for (int x : results)
                    System.out.print(x+" ");
                System.out.println();
                sleep(100);
            }
        }catch (InterruptedException e){
        }
    }
}

class Worker extends Thread{
    int id, t;
    LimitedQueue messages;
    ArrayQueue arrOfResults;

    int nRes = 0;

    public Worker(int id, LimitedQueue messages, ArrayQueue arrOfResults, int t) {
        this.messages = messages;
        this.arrOfResults = arrOfResults;
        this.id = id;
        this.t = t;
    }

    @Override
    public void run() {
        try{
            while (true){
                int result = id*messages.getMessage(); //* messaggio elaborato
                sleep((int)(Math.random()*t*t + t));
                arrOfResults.putResults(result, id);
                nRes++;
                System.out.println(getName()+"carica un risultato");
            }
        }catch (InterruptedException e){
        }
    }
}