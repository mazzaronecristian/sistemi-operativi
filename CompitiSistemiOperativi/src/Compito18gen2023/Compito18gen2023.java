package Compito18gen2023;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Compito18gen2023 {
    public static void main(String[] args) throws InterruptedException {
        int n = 10, m = 5, k = 3, ta = 50, tb = 100, tg = 10;
        LimitedQueue limQueue = new LimitedQueue(k);
        UnlimitedQueue unlQueue = new UnlimitedQueue();

        Generator[] gens = new Generator[n];
        Resources a = new Resources(n);
        Resources b = new Resources(n);
        Counter counter = new Counter();
        Worker[] works = new Worker[m];
        Collector collector = new Collector(unlQueue);

        collector.start();

        for(int i = 0; i < gens.length; i++){
            gens[i] = new Generator(i, counter, limQueue, tg);
            gens[i].setName("G["+i+"]");
            gens[i].start();
        }
        for (int i= 0; i< works.length; i++){
            works[i] = new Worker(unlQueue, limQueue, a, b, i, tb, ta);
            works[i].setName("W["+i+"]");
            works[i].start();
        }

        Thread.sleep(10000);
        collector.interrupt();
        collector.join();
        for (Generator g: gens){
            g.interrupt();
            g.join();
            System.out.print(g.getName()+" ha generato "+g.nGenerated+" valori");
        }
        System.out.println();
        for (Worker w: works){
            w.interrupt();
            w.join();
            System.out.print(w.getName()+" ha processato "+w.nWorked+" valori");
        }
        System.out.println();
        int aRes = a.available.availablePermits();
        int bRes = b.available.availablePermits();

        System.out.println("rimangono "+aRes+" risorse A; e "+bRes+" risorse B");


    }
}

class Message {
    int idGenerator, idWorker, p, valueGenerator = 0, valueWorker = 0;

    public Message(int idGenerator, int p) {
        this.idGenerator = idGenerator;
        this.p = p;
    }

}
class Counter{
    int p = 0;
    Semaphore mutex = new Semaphore(1);
    public int getP() throws InterruptedException {
        int x = 0;
        mutex.acquire();
        x = p;
        p++;
        mutex.release();
        return x;
    }
}

class LimitedQueue {
    Semaphore mutex = new Semaphore(1);
    Semaphore empty;
    Semaphore full = new Semaphore(0);
    ArrayList<Message> messages = new ArrayList<>();

    public LimitedQueue(int k) {
        empty = new Semaphore(k);
    }
    public void putMsg(Message m) throws InterruptedException {
        empty.acquire();
        mutex.acquire();

        messages.add(m);

        mutex.release();
        full.release();
    }
    public Message getMsg() throws InterruptedException {
        full.acquire();
        mutex.acquire();
        int pos = findMsgPos();
        Message m = messages.remove(pos);
        mutex.release();
        empty.release();
        return m;
    }
    private int findMsgPos(){
        int pos = 0;
        for (int i = 1; i<messages.size(); i++){
            if(messages.get(i).p < messages.get(pos).p)
                pos = i;
        }
        return pos;
    }
}
class UnlimitedQueue {
    ArrayList<Message> messages = new ArrayList<>();
    Semaphore mutex = new Semaphore(1);
    Semaphore full = new Semaphore(0);

    public void putMsg(Message msg) throws InterruptedException {
        mutex.acquire();
        messages.add(msg);
        mutex.release();
        full.release();
    }

    public Message getMsg() throws InterruptedException {
        full.acquire();
        mutex.acquire();
        int pos = findMsgPos();
        Message m = messages.remove(pos);
        mutex.release();
        return m;
    }

    private int findMsgPos(){
        int pos = 0;
        for (int i = 0; i<messages.size(); i++){
            if(messages.get(i).p < messages.get(pos).p)
                pos = i;
        }
        return pos;
    }
}

class Resources{
    Semaphore available;

    public Resources(int n) {
        available = new Semaphore(n);
    }
    public void acquireResource() throws InterruptedException {
        available.acquire();
    }
    public void releaseResource(){
        available.release();
    }
}
class Generator extends Thread{
    int id;
    Counter counter;
    LimitedQueue limQueue;
    int tg, nGenerated = 0;

    public Generator(int id, Counter counter, LimitedQueue limQueue, int tg) {
        this.id = id;
        this.counter = counter;
        this.limQueue = limQueue;
        this.tg = tg;
    }

    @Override
    public void run() {
        try {
            while (true){
                int p = counter.getP();
                Message msg = new Message(id, p);
                limQueue.putMsg(msg);
                nGenerated++;
                sleep(tg);
            }
        }catch (InterruptedException e){}
    }
}

class Worker extends Thread{
    UnlimitedQueue unlQueue;
    LimitedQueue limQueue;
    Resources rA, rB;
    int id, tb, ta, nWorked = 0;

    public Worker(UnlimitedQueue unlQueue, LimitedQueue limQueue, Resources rA, Resources rB, int id, int tb, int ta) {
        this.unlQueue = unlQueue;
        this.limQueue = limQueue;
        this.rA = rA;
        this.rB = rB;
        this.id = id;
        this.ta = ta;
        this.tb = tb;
    }

    @Override
    public void run() {
        try{
            while (true){
                Message msg = limQueue.getMsg();
                rA.acquireResource();
                sleep(ta);
                rB.acquireResource();
                sleep(tb);
                rA.releaseResource();
                rB.releaseResource();
                msg.valueWorker = msg.p*id;
                msg.idWorker = id;
                unlQueue.putMsg(msg);
                nWorked++;
            }
        }catch (InterruptedException e){}
        finally {
            rA.releaseResource();
            rB.releaseResource();
        }
    }
}

class Collector extends Thread{
    UnlimitedQueue unlQueue;

    public Collector(UnlimitedQueue unlQueue) {
        this.unlQueue = unlQueue;
    }

    @Override
    public void run() {
        try{
            while (true){
                Message msg = unlQueue.getMsg();
                System.out.println("p:"+msg.p+" idGen:"+msg.idGenerator+" idWork:"+msg.idWorker+" value0:"+msg.valueGenerator+" value1:"+msg.valueWorker);
            }
        }catch (InterruptedException e){}
    }
}
