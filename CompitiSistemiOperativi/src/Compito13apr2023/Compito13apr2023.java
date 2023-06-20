package Compito13apr2023;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Semaphore;

public class Compito13apr2023 {
    public static void main(String[] args) throws InterruptedException {
        int n = 4, m = 3, k = 10, l = 4, t= 1000;

        LimQueue lq = new LimQueue(l);
        ResourceMng rm = new ResourceMng(k);

        Requester[] requesters = new Requester[n];
        Worker[] workers = new Worker[m];
        for(int i = 0; i < requesters.length ; i++){
            requesters[i] = new Requester(lq, i);
            requesters[i].setName("R["+i+"]");
            requesters[i].start();
        }
        for(int i = 0; i < workers.length ; i++){
            workers[i] = new Worker(requesters, lq, rm, i, t);
            workers[i].setName("W["+i+"]");
            workers[i].start();
        }
        Thread.sleep(10000);

        for(Requester r: requesters){
            r.interrupt();
            r.join();
            System.out.print(r.getName()+" nReq:"+r.nReq+" avgTime:"+r.totTime/r.nReq);
        }
        System.out.println();

        for(Worker w: workers){
            w.interrupt();
            w.join();
            System.out.print(w.getName()+" nReq:"+w.nReq);
        }
        System.out.println();
        int nRes = 0;
        for(int i = 0 ; i < k; i++){
            if(rm.resources[i].available)
                nRes++;
            System.out.print("Risorsa"+i+" usata"+rm.resources[i].nUse+" volte");
        }
        System.out.println(" Numero risorse disponibili:"+nRes);

    }
}

class Request{
    int value;
    int idReq;
    int nRes; //* n di risorse necessario per elaborare la richiesta

    public Request(int idReq, int nRes, int value) {
        this.idReq = idReq;
        this.nRes = nRes;
        this.value = value;
    }
}

class Resource {
    int nUse = 0;
    boolean available = true;
    Integer idOwner = null;
}

class LimQueue {
    ArrayList<Request> list = new ArrayList<>();
    Semaphore mutex = new Semaphore(1);
    Semaphore piene = new Semaphore(0);
    Semaphore vuote;

    public LimQueue(int l){
        vuote = new Semaphore(l);
    }

    public void putRequest(Request r) throws InterruptedException{
        vuote.acquire();
        mutex.acquire();
        list.add(r);
        mutex.release();
        piene.release();
    }

    public Request getRequest() throws InterruptedException{
        piene.acquire();
        mutex.acquire();
        Request r = list.remove(0);
        mutex.release();
        vuote.release();
        return r;
    }
}

class ResourceMng {
    Semaphore available;
    Semaphore mutex = new Semaphore(1);
    Resource[] resources;
    public ResourceMng(int k) {
        available = new Semaphore(k);
        resources = new Resource[k];
        for(int i = 0; i < resources.length; i++)
            resources[i] = new Resource();
    }
    public void getRes(int n, int idW) throws InterruptedException {
        available.acquire(n);
        mutex.acquire();
        for (int i = 0; i < n; i++){
            int pos = getMinUsed();
            resources[pos].nUse++;
            resources[pos].available = false;
            resources[pos].idOwner = idW;
        }

        mutex.release();

    }
    public void relRes(int idW) throws InterruptedException {
        int n = 0;
        for(int i = 0; i < resources.length; i++){  //todo: fixme!!!
            if(Objects.equals(resources[i].idOwner, idW)) {
                resources[i].available = true;
                n++;
            }
        }
        available.release(n);
    }

    private int getMinUsed(){
        int pos = 0;

        for(int i= 0; i < resources.length; i++){
            if (resources[i].available){
                if (resources[i].nUse < resources[pos].nUse) {
                    pos = i;
                }
            }
        }
        return pos;
    }
}

class Requester extends Thread{
    LimQueue lq;
    int id;
    int nReq = 0;
    int answer;
    long totTime = 0;
    Semaphore result = new Semaphore(0); //* indica al requester se il risultato è disponibile o meno
                                                //* 0 se non disponibile,
                                                //* 1 se disponibile
    public Requester(LimQueue lq, int id) {
        this.lq = lq;
        this.id = id;
    }
    public void run(){
        try{
            while(true){
                Request r = new Request(id, 1 + (nReq % 3), nReq++);
                lq.putRequest(r);
                long time = System.currentTimeMillis();

                result.acquire();       //* in attesa della risposta

                time = System.currentTimeMillis() - time;
                System.out.println("Requester"+id+" req:"+r.value+" answer"+answer+ " time:"+time);
                totTime += time;
            }
        }catch (InterruptedException e){}
    }
}

class Worker extends Thread{
    Requester[] requesters;
    LimQueue lq;
    ResourceMng rm;
    int nReq = 0;
    int id;
    int t;

    public Worker(Requester[] requesters, LimQueue lq, ResourceMng rm, int id, int t) {
        this.requesters = requesters;
        this.lq = lq;
        this.rm = rm;
        this.id = id;
        this.t = t;
    }

    public void run() {
        try{
            while(true){

                Request r = lq.getRequest();

                rm.getRes(r.nRes, id);
                sleep(t);

                rm.relRes(id);

                int idReq = r.idReq;
                requesters[idReq].answer = r.value * 2;

                requesters[idReq].result.release();
            }
        }catch (InterruptedException e){}
        finally {
            try {
                rm.relRes(id);
            } catch (InterruptedException e) {}
        }
    }
}