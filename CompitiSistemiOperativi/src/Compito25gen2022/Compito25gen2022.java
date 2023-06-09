package Compito25gen2022;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

//semafori

public class Compito25gen2022 {
    public static void main(String[] args) throws InterruptedException {
        int m = 10;
        int n = 4;
        List<Integer> listaValori = new ArrayList<>();
        int v = (int)(Math.random()*100);
        listaValori.add(v);
        for(int i = 0 ; i < m - 1; i++){
            v += (int)((Math.random()*99)+1);
            listaValori.add(v);
        }
        Container codaValori = new Container(listaValori);
        HashValue arrayValue = new HashValue(m);

        Worker[] workers = new Worker[n];
        Collector collector = new Collector(codaValori, arrayValue);

        for(int i = 0 ; i < n ; i++){
            workers[i] = new Worker(codaValori, arrayValue);
            workers[i].setName("w["+i+"]");
            workers[i].start();
        }
        collector.start();

        collector.join();

        for(int i = 0 ; i < n ; i++){
            workers[i].interrupt();
            workers[i].join();
        }

        for(int i = 0 ; i < n ; i++){
            System.out.println(workers[i].getName()+" ha trovato "+workers[i].nOccupati+" posti occupati");
        }

    }
}

class Container{
    List codaValori;
    Semaphore mutex = new Semaphore(1);
    Semaphore nValori;  //questo semaforo identifica il n' di elementi attualmente nel container

    public Container(List codaValori){
        this.codaValori = codaValori;
        nValori = new Semaphore(codaValori.size());
    }

    public Object get() throws InterruptedException{
        nValori.acquire();  //attende se non ci sono valori nella codaValori, altrimenti decrementa nValori
        mutex.acquire();    //decrementa mutex se nessuno sta leggendo dalla coda
        int P = (int)(Math.random()*codaValori.size());
        Object o = codaValori.remove(P);
        mutex.release();
        return o;
    }

    public void putAll(Object[] o) throws InterruptedException {
        mutex.acquire();
        for(int i = 0; i < o.length; i++){
            codaValori.add(o[i]);
        }
        mutex.release();
        nValori.release(o.length);
    }
}

class HashValue{
    Object[] arrayValue;

    Semaphore mutex = new Semaphore(1);
    Semaphore nValori = new Semaphore(0);

    public HashValue(int M){
        arrayValue = new Object[M];
    }

    public int put(int V) throws InterruptedException {
        mutex.acquire();
        int P = V % arrayValue.length; //prima posizione in cui prova a mettere l'oggetto
        int nOccupati = 0;
        while(arrayValue[P]!=null){
            P = ( P + 1 ) % arrayValue.length;
            nOccupati++;
            mutex.release();  //rilascia semaforo, gli altri in attesa provano ad accedere
            mutex.acquire();  //per limitare al max il tempo di attesa
        }
        arrayValue[P] = V;
        mutex.release();
        nValori.release();
        return nOccupati;
    }

    public Object[] getAll() throws InterruptedException {
        nValori.acquire(arrayValue.length); //si mette in attesa se la lista non è piena
        mutex.acquire();
        Object[] C = arrayValue;
        arrayValue = new Object[arrayValue.length]; //sovrascrivo il vettore azzerandolo
        mutex.release();
        return C;
    }
}

class Worker extends Thread{
    Container container;
    HashValue hashValue;
    int nOccupati = 0;

    public Worker(Container container, HashValue hashValue){
        this.container = container;  //SONO GIà STATI CREATIIIIIIIII
        this.hashValue = hashValue;
    }

    public void run(){
        try{
            while(true){
                int V = (Integer)container.get();
                sleep(100);
                nOccupati += hashValue.put(V);
            }
        }catch (InterruptedException e){
            System.out.println("interrotto");
        }
    }
}

class Collector extends Thread{
    Container container;
    HashValue hashValue;

    public Collector(Container container, HashValue hashValue){
        this.container = container;
        this.hashValue = hashValue;
    }

    public void run(){
        try{
            for( int i = 0 ; i < 3 ; i++ ){
                Object[] o = hashValue.getAll();
                sleep(100);
                for(Object x : o){
                    System.out.print("["+x+"] ");
                }
                System.out.println();
                container.putAll(o);
            }
        }catch (InterruptedException e){
            System.out.println("interrotto");
        }
    }
}


