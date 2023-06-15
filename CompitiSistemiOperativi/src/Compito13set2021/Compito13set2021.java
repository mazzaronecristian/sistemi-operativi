package Compito13set2021;

import java.util.ArrayList;

public class Compito13set2021 {
    public static void main(String[] args) throws InterruptedException {
        int n = 5;
        int m = 10;
        MidQueue midQueue = new MidQueue( n );
        OutQueue outQueue = new OutQueue();

        Generator[ ] generators = new Generator[ n ];
        Processor[ ] processors = new Processor[ m ];
        Extractor extractor = new Extractor( outQueue );

        for( int i = 0; i < generators.length; i++ ){
            generators[i] = new Generator( i, midQueue );
            generators[i].start();
        }

        for( int i = 0; i < processors.length; i++ ){
            processors[i] = new Processor( midQueue, outQueue );
            processors[i].start();
        }

        extractor.start();

        Thread.sleep(30000);

        for( int i = 0; i < generators.length; i++ ){
            generators[i].interrupt();
            generators[i].join();
        }
        for( int i = 0; i < processors.length; i++ ){
            processors[i].interrupt();
            processors[i].join();
        }
        extractor.interrupt();
        extractor.join();
    }
}

class Message{
    int p;
    int value;

    public Message(int p, int value){
        this.p = p;
        this.value = value;
    }
}

class MidQueue{
    Message[] messages;
    int full = 0; //* numero di caselle piene

    public MidQueue(int n){
        messages = new Message[n];
    }

    public synchronized void putMessage(Message m, int id) throws InterruptedException {
        while( messages[ id ] != null){
            wait();
        }
        messages[ id ] = m;
        full++;
        notifyAll();
    }

    public synchronized Message[] getAllMessages() throws InterruptedException {
        while ( full != messages.length){
            wait();
        }
        Message[] o = messages;
        for( int i= 0; i < messages.length; i++ ) //* azzero l'array dopo averlo copiato
            messages[i] = null;{
        }
        System.out.println("MidQueue svuotata");
        full = 0;
        notifyAll();
        return o;
    }
}

class OutQueue{
    ArrayList<Message> results = new ArrayList<>();

    public synchronized void putResult(Message r){
        results.add(r);
        notifyAll();
    }

    public synchronized Message getResult(int p) throws InterruptedException {
        Message r = null;
        while( (r = findResult(p)) == null ){  //* cerca il risultato con p
            wait();
        }
        return r;
    }
    
    private Message findResult(int p){
        for( Message r : results){
            if( r.p == p){
                return r;
            }
        }
        return null;
    }
}

class Generator extends Thread{
    int id;
    int p = 0;
    MidQueue midQueue;

    public Generator(int id, MidQueue midQueue) {
        this.id = id;
        this.midQueue = midQueue;
    }

    @Override
    public void run() {
        try{
            while(true){
                int value = ( id * 100 + p );
                Message m = new Message(p, value);
                midQueue.putMessage(m, id);
                System.out.println("Gen"+id+" ha generato p:"+p+" value:"+value);
                sleep((int)(Math.random()*900)+100);  //* aspetta tra 100 e 1000 ms
                p++;
            }
        } catch (InterruptedException e) {
            System.out.println("Generator interrotto");
        }
    }
}

class Processor extends Thread{
    MidQueue midQueue;
    OutQueue outQueue;

    public Processor(MidQueue midQueue, OutQueue outQueue) {
        this.midQueue = midQueue;
        this.outQueue = outQueue;
    }

    public void run(){
        try{
            while(true){
                Message[] messages;
                messages = midQueue.getAllMessages();
                int p = messages[0].p;
                Message result;
                int sum = 0;
                for( int i = 0; i < messages.length; i++){
                    sum += messages[i].value;
                }
                result = new Message(p, sum);
                outQueue.putResult(result);
                System.out.println("caricato p:"+p+" sum:"+sum);
                sleep((int)(Math.random()*900)+100);
            }
        } catch (InterruptedException e) {
            System.out.println("Processor interrotto");
        }
    }
}

class Extractor extends Thread{
    int pPrev = 0;
    OutQueue outQueue;

    public Extractor(OutQueue outQueue) {
        this.outQueue = outQueue;
    }

    public void run(){
        try{
            while(true){  //* estrae i risultati in ordine secondo p
                Message result = outQueue.getResult(pPrev);
                System.out.println("Stampo valore p:"+result.p+" value: "+result.value);
                pPrev++;
            }
        }catch (InterruptedException e){
            System.out.println("Extractor interrotto");
        }
    }
}