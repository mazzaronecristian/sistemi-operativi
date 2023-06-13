package Compito15feb2022;

import java.util.ArrayList;

public class Compito15feb2022 {
    public static void main(String[] args) throws InterruptedException {
        int n = 60;
        int m = 10;
        Laghetto lago = new Laghetto();
        Pesce[] pesci = new Pesce[n];
        Pescatore[] pescatori = new Pescatore[m];

        Pesce.nVivi = n;
        for(int i = 0; i < pesci.length; i++){
            pesci[i] = new Pesce(lago);
            pesci[i].setName("pesce"+i);
            pesci[i].start();
        }

        for(int i = 0; i < pescatori.length; i++){
            pescatori[i] = new Pescatore(lago);
            pescatori[i].setName("P"+i);
            pescatori[i].start();
        }

        for (int i = 0; i < 30; i++){ //* numero pesci vivi, numero di pesci pe pescatore somma pesci vivi e catturati
            int nPesci = 0;
            for(int j = 0; j<pescatori.length; j++){
                nPesci += pescatori[j].nPesci;
            }
            System.out.println("n pesci vivi: "+Pesce.nVivi+" n pesci catturati: "+ nPesci);
            Thread.sleep(1000);
        }

        for(int i = 0; i < pescatori.length; i++){
            pescatori[i].interrupt();
        }

        for(int i = 0; i < pesci.length; i++){
            pesci[i].interrupt();
        }

    }
}
class Position{
    float x, y;
    public Position(float x, float y){
        this.x = x;
        this.y = y;
    }
    public static float getDist(Position pos1, Position pos2){
        return (float)( Math.sqrt( (pos1.x- pos2.x)*(pos1.x- pos2.x)
                + (pos1.y- pos2.y)*(pos1.y- pos2.y) ) );
    }
    public void move(float dx, float dy){
        x += dx;
        y += dy;
    }
}
class Esca{
    Position pos;
    Pesce pesce = null;
    boolean viva = true;

    public Esca(Position pos) {
        this.pos = pos;
    }

    public synchronized void cattura(Pesce pesce){
        if(viva){
            viva = false;
            this.pesce = pesce;
            notifyAll();
        }
    }

    public synchronized void waitPesce() throws InterruptedException {
        while(pesce == null){
            wait();
        }
    }
}
class Laghetto{
    ArrayList<Esca> esche = new ArrayList<>();

    public synchronized Esca findEsca(Position pos) throws InterruptedException {
        Esca esca = null;
        for(Esca e : esche){
            if (Position.getDist(e.pos, pos) <= 2){
                esca = e;
                break;
            }
        }
        return esca;
    }
    public synchronized void putEsca(Esca e){
        esche.add(e);
    }
    public synchronized void removeEsca(Esca e) {
        esche.remove(e);
    }
}
class Pesce extends Thread{
    Laghetto lago;
    Position pos = new Position( (float)(Math.random()*20-10) , (float)(Math.random()*20-10) );
    static int nVivi = 0;

    public Pesce(Laghetto lago) {
        this.lago = lago;
    }

    @Override
    public void run() {
        try{
            while (true){
                Esca e = lago.findEsca(pos);
                if(e!=null && e.viva){ //* ho trovato un'esca viva
                    e.cattura(this);
                    synchronized (Pesce.class){
                        nVivi--;
                    }
                    break;
                }
                pos.move( (float)(Math.random()*2-1),(float)(Math.random()*2-1) );
                sleep(100);
            }
        }catch (InterruptedException e){}
    }
}
class Pescatore extends Thread{
    int nPesci = 0;
    Laghetto lago;

    public Pescatore(Laghetto lago) {
        this.lago = lago;
    }

    @Override
    public void run() {
        try{
            while (true){
                Esca e = new Esca( new Position( (float)(Math.random()*20-10) , (float)(Math.random()*20-10) ) );
                lago.putEsca(e);
                e.waitPesce();
                if(e.pesce != null){
                    nPesci++;
                }
                lago.removeEsca(e);
                sleep(100);
            }
        }catch (InterruptedException e){

        }
    }
}