package Compito22nov2021;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Compito22nov2021 {
    public static void main(String[] args) throws InterruptedException {
        int n = 50;
        int m = 5;
        int t = 2000;
        int k = 30;
        int v = 10;

        Ambiente amb = new Ambiente();
        Ospedale osp = new Ospedale(5);

        Persona[] persone = new Persona[n]; //creo le persone

        for(int i = 0 ; i < persone.length ; i++){
            Posizione pos = new Posizione(Math.random()*20, Math.random()*20);
            persone[i] = new Persona(i, pos, t, osp, amb);
            if( i < k ){
                persone[i].caricaV = 50;
            }else if( i < k + v ){
                persone[i].vaccinato = true;
            }
            amb.addPersone(persone[i]);
            persone[i].setName("p:"+i);
            persone[i].start();
        }

        for(int i = 0 ; i < 15 ; i++ ){
            Thread.sleep(1000);
            int cv = 0;
            for(Persona p: persone){
                if(p.caricaV > 10){
                   cv++;
                }
            }
            System.out.println("Pers. con caricaV contagiosa:"+cv+" Pers. in osp:"+(m - osp.vuote.availablePermits())+" Pers. in coda:"+ osp.nCoda);
        }
        for(Persona p : persone){
            p.interrupt();
            p.join();
            System.out.println("Pers. contagiate da "+p.getName()+" "+p.nContagiati);
        }
    }
}

class Posizione{
    double x, y;

    public Posizione(double x, double y){
        this.x = x;
        this.y = y;
    }
}

class Persona extends Thread {
    int id;
    Posizione pos;
    boolean vaccinato;
    int caricaV = 0;
     Ambiente amb;
     Ospedale osp;
    int t, nContagiati = 0;
    Semaphore mutex = new Semaphore(1);

    public Persona(int id, Posizione pos, int t, Ospedale osp, Ambiente amb) {
        this.id = id;
        this.pos = pos;
        this.t = t;
        this.osp = osp;
        this.amb = amb;
    }

    public void run(){
        try{
            while(true){
                if(caricaV > 100){
                    amb.removePersona(this);
                    osp.takeSpot();
                    sleep(t);
                    caricaV = 0;
                    osp.releaseSpot();
                    amb.addPersone(this);
                }else if(caricaV > 10){
                    ArrayList<Persona> vicini = amb.findPersona(this);
                    for(Persona p:vicini){
                        mutex.acquire();
                        if (!p.vaccinato  || Math.random()<0.1){
                            p.caricaV += 5;
                            mutex.release();
                            nContagiati ++;
                        }
                    }
                }
                double dx = Math.random()*2-1; //si sposta
                double dy = Math.random()*2-1;
                mutex.acquire();
                pos.x += dx;
                pos.y += dy;
                if(caricaV > 0 )
                    caricaV--;
                mutex.release();
                sleep(100);
            }
        }catch (InterruptedException e) {
            System.out.println("interrotto");
        }
    }


}

class Ambiente {
    ArrayList<Persona> persone = new ArrayList<Persona>();
    Semaphore mutex = new Semaphore(1);

    public void addPersone(Persona p) throws InterruptedException {
        mutex.acquire();
        persone.add(p);
        mutex.release();
    }

    public void removePersona(Persona p) throws InterruptedException {
        mutex.acquire();
        persone.remove(p);
        mutex.release();
    }

    public ArrayList<Persona> findPersona(Persona infetto) throws InterruptedException{
        mutex.acquire();
        ArrayList<Persona> personeVicine = new ArrayList<Persona>();
        for(Persona ps : persone){
            if(distance(ps.pos, infetto.pos) < 2 && infetto != ps){
                personeVicine.add(ps);
            }
        }
        mutex.release();
        return personeVicine;
    }

    private double distance(Posizione pos1, Posizione pos2){
        double dist = Math.sqrt((pos1.x - pos2.x)*(pos1.x - pos2.x)+(pos1.y - pos2.y)*(pos1.y - pos2.y));
        return dist;
    }
}

class Ospedale{
    int nCoda = 0;
    Semaphore mutex = new Semaphore(1);
    Semaphore vuote;

    public Ospedale(int m){
        vuote = new Semaphore(m);
    }

    public void takeSpot() throws InterruptedException {
        mutex.acquire();
        nCoda++;
        mutex.release(); //si è messo in coda

        vuote.acquire();
        mutex.acquire();
        nCoda--;
        mutex.release(); //esce dalla coda e prende una stanza in ospedale
    }

    public void releaseSpot(){
        vuote.release();
    }
}
