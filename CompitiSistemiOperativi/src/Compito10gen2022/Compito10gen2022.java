package Compito10gen2022;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadPoolExecutor;

public class Compito10gen2022 {
    public static void main(String[] args) throws InterruptedException {
        int n = 5;
        double borsellino = 5000;
        Tavolo tavolo = new Tavolo(n);
        Giocatore[] giocatori = new Giocatore[n];
        Banco banco = new Banco(tavolo, n);

        for(int i = 0; i < n; i++){
            giocatori[i] = new Giocatore(tavolo, i, borsellino);
            giocatori[i].setName("G["+i+"]");
            giocatori[i].start();
        }

        banco.setName("B");
        banco.start();
        for (Giocatore giocatore : giocatori) giocatore.join();
        banco.join();

        double totale = banco.borsellino;
        System.out.println("borsellino banco: "+banco.borsellino);
        for (int i = 0; i < n; i++){
            System.out.println("borsellino giocatore: "+giocatori[i].borsellino);
            totale += giocatori[i].borsellino;
            System.out.print(giocatori[i].getName()+" ha vinto "+ giocatori[i].nVincite+ " volte");
        }
        System.out.println();
        System.out.println("totale dei borsellini: "+totale);



    }
}

class Bet{
    int target;     //* numero giocato
    double puntata;    //* soldi puntati
    int idGiocatore;

    public Bet(int target, double puntata, int idGiocatore){
        this.target = target;
        this.puntata = puntata;
        this.idGiocatore = idGiocatore;
    }
}
class Tavolo{
    ArrayList<Bet> bets = new ArrayList<>();
    double[] vincite;
    int nGiocatori;

    public Tavolo(int nGiocatori) {
        this.nGiocatori = nGiocatori;
        vincite = new double[nGiocatori];
        for (double v: vincite) {
            v = -1;
        }
    }

    public synchronized void putBet(Bet bet){
        bets.add(bet);
        System.out.println("scommessa fatta "+bet.idGiocatore);
        notifyAll();
    }
    public synchronized int getSize(){
        return bets.size();
    }
    public synchronized ArrayList<Bet> getBets() throws InterruptedException {
        while(getSize()<nGiocatori){
            System.out.println("banco in attesa");
            wait();
        }
        ArrayList<Bet> b = new ArrayList<>(bets);
        bets = new ArrayList<Bet>();
        return b;
    }

    public synchronized void putVincite(double[] vincite){
        for(int i = 0; i<vincite.length; i++) {
            this.vincite[i] = vincite[i];
            notifyAll();
        }
        System.out.println("vincite pubblicate");
    }

    public synchronized void leaveTable(){
        nGiocatori--;
        notifyAll();
    }

    public synchronized double waitResult(int idGiocatore) throws InterruptedException {
        while ( vincite[idGiocatore]==-1 ){
            System.out.println("il giocatore aspetta il vincitore");
            wait();
        }
        System.out.println("ho trovato il vincitore");
        double v = vincite[idGiocatore];
        vincite[idGiocatore] = -1;
        return v;
    }
}
class Giocatore extends Thread{
    Tavolo tavolo;
    int id;
    double borsellino;

    int nVincite = 0;

    public Giocatore(Tavolo tavolo, int id, double borsellino) {
        this.tavolo = tavolo;
        this.id = id;
        this.borsellino = borsellino;
    }

    @Override
    public void run() {
        try{
            while (borsellino>=1){
                int target = (int)(Math.random()*100+1);            //* genero un numero su cui puntare
                double puntata = (borsellino*0.2);                  //* punto il 20% del borsellino
                borsellino -= puntata;
                Bet b = new Bet( target, puntata, id );
                tavolo.putBet(b);                                   //* faccio la scommessa
                double vincita = tavolo.waitResult(id);             //* aspetto che escano i risultati

                System.out.println(getName()+" target: "+target+" vincita "+vincita);

                if(vincita>0)  {
                    borsellino += vincita;
                    nVincite++;
                }
            }
            tavolo.leaveTable();
        }catch (InterruptedException e){

        }
    }
}

class Banco extends Thread{
    Tavolo tavolo;

    double borsellino = 0;
    int numeroVincente;
    double[] vincite;

    boolean fine = false;

    public Banco(Tavolo tavolo, int nGiocatori) {
        this.tavolo = tavolo;
        vincite = new double[nGiocatori];
        for(double v: vincite)
            v = 0;
    }

    @Override
    public void run() {
        try {
            while (!fine){
                numeroVincente = (int)(Math.random()*100+1);            //* genera il numero vincente
                ArrayList<Bet> bets = tavolo.getBets();                 //* prende le puntate dei giocatori
                System.out.println(bets.size());
                if(bets.size() == 0) {
                    fine = true;
                }
                ArrayList<Integer> vincitori = new ArrayList<>();
                double tot = getTotGiocate(bets);                        //* somma totale puntata dai giocatori
                System.out.println("borsellino banco: "+(borsellino));
                int minDist = 99;
                for(Bet b : bets){
                    if(b.target <= numeroVincente){
                        int dist = findDist(b.target);                  //* trovo la distanza tra il target della puntata e il numero vincente
                        if (dist<minDist){                              //* distanza minore della distanza minore, allora
                            minDist = dist;                             //* aggiorno la distanza minore
                            vincitori.clear();                          //* i vincitori precedenti non sono più vincitori
                            vincitori.add(b.idGiocatore);               //* aggiungo il nuovo vincitore
                        }
                        if(dist == minDist)                             //* distanza uguale, allora
                            vincitori.add(b.idGiocatore);               //* aggiungo un altro vincitore
                    }
                }
                if(vincitori.size() == 0) {                               //* nessuno ha vinto, allora
                    borsellino += tot;                                  //* vince il banco (prende tutte le puntate)
                } else {
                        int nVincitori = vincitori.size();
                        tot /= nVincitori;                                  //* divido il totale per il numero di vincitori
                        for(int x: vincitori){                              //* assegno a ogni vincitore la propria vincita
                            vincite[x] = tot;
                        }
                }
                tavolo.putVincite(vincite);
                Arrays.fill(vincite, 0);
            }
        }catch (InterruptedException e){

        }
    }

    private int findDist(int value){
        return numeroVincente-value;
    }
    private double getTotGiocate(ArrayList<Bet> bets){
        double tot = 0;
        for(Bet b: bets){
            tot += b.puntata;
        }
        return tot;
    }
}


