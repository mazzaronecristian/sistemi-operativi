package Compito21lug2021;

import java.util.ArrayList;

public class Compito21lug2021 {
    public static void main(String[] args) throws InterruptedException {
        int n = 10; //* n client
        int m = 5; //* m worker
        int k = 60;

        RequestQueue rq = new RequestQueue(k);
        ClientThread[] clients = new ClientThread[n];
        WorkerThread[] workers = new WorkerThread[m];

        for( int i = 0 ; i < n ; i++){
            clients[i] = new ClientThread(rq);
            clients[i].setName("C["+i+"]");
            clients[i].start();
        }

        for( int i = 0 ; i < m ; i++){
            workers[i] = new WorkerThread(rq);
            workers[i].setName("W["+i+"]");
            workers[i].start();
        }
        Thread.sleep(10000);

        for( int i = 0 ; i < n ; i++){
            clients[i].interrupt();
        }

        for( int i = 0 ; i < m ; i++) {
            workers[i].interrupt();
            workers[i].join();
            System.out.print("n richieste servite: "+workers[i].nReq);
        }
        System.out.println();

    }
}

class Request {
    int value;
    ResultCollector rc;

    public Request(int value, ResultCollector rc) {
        this.value = value;
        this.rc = rc;
    }
}

class ResultCollector {
    ArrayList<Integer> results = new ArrayList<>();
    int n;      //* n di richieste che fa il client

    public synchronized void waitResult() throws InterruptedException {
        while(results.size() < n){
            wait();
        }
    }

    public void reset(int n){
        results.clear();
        this.n = n;
    }

    public synchronized void putResult(int result) throws InterruptedException{
        results.add(result);
        notify();
    }
}

class RequestQueue {
    int k;
    ArrayList<Request> rq = new ArrayList<>();

    public RequestQueue(int k){
        this.k = k;
    }

    public synchronized void putRequest(Request r) throws InterruptedException{
        while(rq.size() == k){
            wait();
        }
        rq.add(r);
        notifyAll();
    }

    public synchronized Request getRequest() throws InterruptedException{
        while(rq.size() == 0){
            wait();
        }
        Request r = rq.remove(0); //* rimuove l'oggetto e lo copia nella destinazione
        notifyAll();
        return r;
    }
}

class ClientThread extends Thread{
    ResultCollector rc = new ResultCollector();
    RequestQueue rq;
    int nReq = 0;

    public ClientThread(RequestQueue rq) {
        this.rq = rq;
    }

    public void run(){
        try{
            while(true){
                int n = (int)(2 + (Math.random()*5));
                rc.reset(n);
                nReq += n;
                for(int i = 0; i < n; i++){
                    Request r = new Request((i+1)*10, rc );
                    rq.putRequest(r);
                }
                rc.waitResult();
                int sum = 0;
                for(int i = 0; i < n; i++)
                    sum += rc.results.get(i);
                System.out.println("Tot richieste: "+n+" Risultato:"+sum);
            }
        }catch(InterruptedException e){}
    }
}

class WorkerThread extends Thread{
    RequestQueue rq;
    int nReq = 0;

    public WorkerThread(RequestQueue rq) {
        this.rq = rq;
    }

    public void run(){
        try{
            while(true){
                Request r = rq.getRequest();
                r.value *= 2;
                r.rc.putResult(r.value);
                nReq++;
                sleep(2000);
            }
        }catch (InterruptedException e){
        }
    }
}


