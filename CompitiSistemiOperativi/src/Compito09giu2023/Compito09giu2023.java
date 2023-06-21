package Compito09giu2023;

import java.util.ArrayList;

public class Compito09giu2023 {
    public static void main(String[] args) throws InterruptedException{
        int l = 4;
        int m = 5;
        int n = 10;
        int t = 10;
        LimQueue limQueue = new LimQueue(l, m);
        UnlimQueue arrQueue = new UnlimQueue(m);

        Generator[] gen = new Generator[n];
        Worker[] work = new Worker[m];
        Printer printer = new Printer(m, arrQueue);
        for (int i = 0; i<gen.length;i++){
            gen[i] = new Generator(i, t, limQueue);
            gen[i].setName("G"+i);
            gen[i].start();
        }

        for (int i = 0; i<work.length;i++){
            work[i] = new Worker(i+1, limQueue, arrQueue, t);
            work[i].setName("W"+i);
            work[i].start();
        }

        printer.start();
        Thread.sleep(10000);

        for(Worker w:work){
            w.interrupt();
            w.join();
            System.out.println(w.getName()+" ha prodotto "+ w.nReq + " risultati");
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


        System.out.println("rimangono "+ limQueue.queue.size()+" messaggi nella coda limitata e "+ arrQueue.unlQueue.size()+ " nella coda illimitata");
    }
}

class Message{
    int idG;
    int idW;
    int value;

    public Message(int idG, int value) {
        this.idG = idG;
        this.value = value;
    }
}

class LimQueue {
    ArrayList<Message> queue = new ArrayList<>();
    int l;
    boolean[] checkTaken;       //* Worker[i] aspetterà se checkTaken[i] == true
    int nMess = 0;              //* num tot di messaggi comunicati

    public LimQueue(int l, int m) {
        this.l = l;
        checkTaken = new boolean[m];  //* java lo inizializza già a false
    }
    public synchronized void putMessage(Message m) throws InterruptedException {
        while ( queue.size() == l )
            wait();
        queue.add(m);
        nMess++;
        notifyAll();
    }
    public synchronized Message getMessage(int idW) throws InterruptedException {
        while (queue.size() == 0)     //* aspetta se la coda è vuota oppure se quel worker ha già preso il mess
            wait();
        checkTaken[idW-1] = true;
        Message m = queue.get(0);
        if (isTrue()){
            checkTaken = new boolean[checkTaken.length];
            queue.remove(0);
        }
        notifyAll();
        return m;
    }
    private boolean isTrue() {
        boolean check = true;
        for(boolean c : checkTaken){
            if(!c) {
                check = false;
                break;
            }
        }
        return check;
    }
    public synchronized void wait(int idW) throws InterruptedException {
        while (checkTaken[idW-1])
            wait();
    }
}

class UnlimQueue{
    ArrayList<Message[]> unlQueue = new ArrayList<>();
    Message[] results;
    public UnlimQueue(int m){
        results = new Message[m];
    }

    public synchronized void putResults(Message m) throws InterruptedException {
        results[m.idW-1] = m;
        if(isComplete()){
            unlQueue.add(results);
            results = new Message[results.length];
            notifyAll();
        }
    }

    public synchronized void wait(int idW) throws InterruptedException {
        while (results[idW-1] != null)
            wait();
    }

    private boolean isComplete() {
        boolean check = true;
        for (Message x : results) {
            if (x == null) {
                check = false;
                break;
            }
        }
        return check;
    }

    public synchronized Message[] getResults() throws InterruptedException {
        while(unlQueue.size() == 0)
            wait();
        return unlQueue.remove(0);
    }
}

class Generator extends Thread{
    int id;
    int time;       //* tempo di attesa
    LimQueue queue;
    int nMess = 0;

    public Generator(int id, int time, LimQueue queue) {
        this.time = time;
        this.queue = queue;
        this.id = id;
    }
    @Override
    public void run() {
        try{
            while (true){
                Message msg = new Message(id, id*100+1);
                queue.putMessage(msg);
                sleep(time);
                nMess++;
            }
        }catch (InterruptedException e){
        }
    }
}

class Printer extends Thread{
    int[] results;
    UnlimQueue queue;

    int nPrint;

    public Printer(int m, UnlimQueue queue) {
        this.results = new int[m];
        this.queue = queue;
    }

    @Override
    public void run() {
        try{
            while (true) {
                Message[] messages = queue.getResults();

                System.out.print("array di risultati: [ ");
                for(Message m: messages)
                    System.out.print(m.value+" ");
                System.out.println("]");
                nPrint++;
            }
        }catch (InterruptedException e){
        }
    }
}

class Worker extends Thread{
    int id, t;
    LimQueue limQueue;
    UnlimQueue unlimQueue;

    int nReq = 0;

    public Worker(int id, LimQueue limQueue, UnlimQueue unlimQueue, int t) {
        this.limQueue = limQueue;
        this.unlimQueue = unlimQueue;
        this.id = id;
        this.t = t;
    }

    @Override
    public void run() {
        try{
            while (true){
                limQueue.wait(id);

                Message m = limQueue.getMessage(id);

                int time = (int)( Math.random()*t*t+t);
                sleep( time );

                m.value *= id;
                m.idW = id;

                nReq++;
                unlimQueue.wait(id);
                unlimQueue.putResults(m);
            }
        }catch (InterruptedException e){}
    }
}