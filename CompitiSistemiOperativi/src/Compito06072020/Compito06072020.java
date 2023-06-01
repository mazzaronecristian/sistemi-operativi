package Compito06072020;

public class Compito06072020 {
    public static void main(String[] args) throws InterruptedException {
        int N = 20;
        Environment env = new Environment(N);
        Persona[] p = new Persona[N];

        for (int i = 0; i < N; i++) {
            p[i] = new Persona(env, i);
            p[i].start();
        }

        Thread.sleep(30000);
        for (int i = 0; i < N; i++) {
            p[i].interrupt();
            p[i].join();
            System.out.println("Persona " + i + " ha fatto " + p[i].nUpdate + " aggiornamenti, ha aspettato " +
                    p[i].nWait + " volte e ha percorso " + p[i].totDist + " di distanza");

        }
    }
}

class Position{
    private double x, y;

    public Position(double x, double y){
        this.x = x;
        this.y = y;
    }

    //* costruttore di copia
    public Position(Position p){
        this.x = p.x;
        this.y = p.y;
    }

    public void addPosition(Position p){
        this.x += p.x;
        this.y += p.y;
    }

    public double dist(Position p){
        return Math.sqrt( Math.pow(  this.x - p.x, 2 ) + Math.pow( this.y - p.y, 2 ) );
    }
}

class Environment{
    Position[] pos;

    public Environment(int n){
        pos = new Position[n];
        for (int i = 0; i < n; i++)
            pos[i] = new Position(0, 0);
    }

    public synchronized int updatePosition(Position dxy, int id) throws InterruptedException {
        int nWait = 0;
        while(checkPos(id, dxy)){
            nWait++;
            wait();
        }
        pos[id].addPosition(dxy);
        notifyAll();            //* dopo aver aggionrato la propria posizione, notifica tutti del cambiamento

        //* per orgni wait ci deve sempre essere un notify/notifyAll
        return nWait;
    }

    private boolean checkPos(int id, Position dxy){
        Position newPos = new Position(pos[id]);
        newPos.addPosition(dxy);
        for (int i = 0; i < pos.length; i++) {
            if (id != i && pos[i].dist(newPos) < 1) return true;
        }
        return false;
    }


}

class Persona extends Thread{
    private Environment env;
    private int id;

    public int nWait = 0;
    public int nUpdate = 0;
    public double totDist = 0.0;
    public Persona(Environment env, int id){
        this.env = env;
        this.id = id;
    }

    @Override
    public void run() {
        try {
            while (true){
                double dx = (Math.random() * 20) - 10;
                double dy = (Math.random() * 20) - 10;
                Position dxy = new Position(dx, dy);            //* posizione da aggiungere alla corrente

                nWait += env.updatePosition(dxy, id);           //* provo ad aggiornare la posizione
                nUpdate++;                                      //* aggiorno il numero di volte che ho aggiornato la posizione
                totDist += dxy.dist(new Position(0, 0));  //* aggiorno la distanza totale percorsa
                sleep(100);
            }
        }catch(InterruptedException e){
            System.out.println("Persona " + id + " interrotta");
        }
    }
}