package Compito21072020;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Compito21072020 {
    public static void main (String[] args) throws InterruptedException{
        int M = 4;      //* Numero di stanze
        int K = 3;      //* Numero di stanze da visitare a volta
        int N = 5;     //* Numero di persone
        GestoreStanze gs = new GestoreStanze(M);
        Persona[] p = new Persona[N];
        for (int i = 0; i < N; i++){
            p[i] = new Persona(gs, K);
            p[i].start();
        }

        Thread.sleep(30000);
        for (int i = 0; i < N; i++){
            p[i].interrupt();
        }
    }
}
class GestoreStanze{
    Semaphore[] stanza;
    int[] used;
    public GestoreStanze(int n){
        stanza = new Semaphore[n];
        used = new int[n];
        for(int i = 0; i < n; i++){
            stanza[i] = new Semaphore(1);   //* 1 = stanza libera, 0 = stanza occupata
        }
    }

    public int getStanzaMin(ArrayList<Integer> visited){
        int pmin = -1;
        for (int i = 0; i<used.length; i++){
            if ( !visited.contains(i) && ( pmin == -1 || used[i] < used[pmin] ) )
                pmin = i;
        }

        return pmin;
    }

    public void acquireStanza(int st) throws InterruptedException {
        stanza[st].acquire();
        used[st]++;             //* Incremento il numero di persone che hanno usato la stanza
    }

    public void releaseStanza(int st){
        stanza[st].release();
    }
}
class Persona extends Thread{
    GestoreStanze gs;
    int K;

    public Persona(GestoreStanze gs, int K) {
        this.gs = gs;
        this.K = K;
    }

    public void run(){
        try {
            while (true){
                ArrayList<Integer> visited = new ArrayList<>();
                for (int i = 0; i < K; i++){
                    int st = gs.getStanzaMin(visited);
                    System.out.println(getName()+ " visita la stanza " + st+" "+visited);
                    gs.acquireStanza(st);
                    System.out.println(getName()+ " sta usando la stanza " + st+" "+visited);
                    visited.add(st);
                    sleep(100);
                    gs.releaseStanza(st);
                    System.out.println(getName()+ " lascia la stanza " + st+" "+visited);
                }
            }
        } catch (InterruptedException e) {
            System.out.println(getName()+ " interrotta");
        }
    }
}

