package Compito10gen2022;

import java.util.ArrayList;

public class Compito10gen2022 {
    public static void main(String[] args) {

    }
}

class Bet{
    int puntata; //* valore della puntata
    int idGiocatore; //* id del giocatore che ha effettuato la puntata
    int numero; //* numero su cui si è puntato

    //* costruttore
    public Bet( int idGiocatore, int puntata, int numero ){
        this.idGiocatore = idGiocatore;
        this.puntata = puntata;
        this.numero = numero;
    }
}
class Tavolo{
    ArrayList<Bet> bets = new ArrayList<Bet>(); //* lista delle puntate effettuate
    int nGiocatori;  //* numero di giocatori che hanno effettuato una puntata

    //* costruttore
    public Tavolo( int nGiocatori ){
        this.nGiocatori = nGiocatori;
    }

    public synchronized void addBet(Bet b) throws InterruptedException{
        bets.add(b);
        notifyAll();
    }

    public synchronized ArrayList<Bet> waitAllBets() throws InterruptedException{
        while (bets.size() != nGiocatori){  //* se non ci sono abbastanza puntate
            wait();
        }
        ArrayList<Bet> temp = bets;     //* salva la lista delle puntate
        bets = new ArrayList<>();    //* resetta la lista delle puntate
        return temp;
    }

    public void esceGiocatore(){  //* decrementa il numero di giocatori
        nGiocatori--;
        notifyAll();  //* notifica tutti i giocatori che il numero di giocatori è cambiato
    }
}
class Giocatore extends Thread{
    Tavolo t;
    int totalAmount;


    public Giocatore(Tavolo t, int totalAmount){
        this.t = t;
        this.totalAmount = totalAmount;
    }

    public void run(){
        try{
            while(true){
                sleep(100);
            }
        } catch (InterruptedException e){}
    }
}

class Banco extends Thread{

}


