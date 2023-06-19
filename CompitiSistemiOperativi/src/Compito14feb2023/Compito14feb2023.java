package Compito14feb2023;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Compito14feb2023 {
    public static void main(String[] args) throws InterruptedException {
        int n = 4, m = 3, t=1000;
        Collector collector = new Collector(n);
        FinalQueue finalQueue = new FinalQueue();
        Generator[] generators = new Generator[n];
        Processor[] processors = new Processor[m];

        for (int i = 0; i<generators.length; i++){
            generators[i] = new Generator(i+1,collector, t);
            generators[i].start();
        }
        for (int i = 0; i<processors.length; i++){
            processors[i] = new Processor(collector, finalQueue, i);
            processors[i].start();
        }

        Printer printer = new Printer(finalQueue);
        printer.start();
        printer.join();

        for (Processor p: processors){
            p.interrupt();
            p.join();
        }
        for (Generator g: generators){
            g.interrupt();
            g.join();
        }

    }
}
class Collector{
    Integer[] values;
    List<Integer[]> arrays = new ArrayList<>();

    public Collector(int n){
        values = new Integer[n];
    }

    public synchronized void putValue(int idG, int value){
        values[idG-1] = value;
        if(isFull()){
            arrays.add(values);
            values = new Integer[values.length];
            notifyAll();
        }

    }
    public synchronized ArrayList<Integer[]> getArrays() throws InterruptedException {
        while(arrays.size() < 2) {
            wait();
        }
        ArrayList<Integer[]> twoArr = new ArrayList<>();
        twoArr.add( arrays.remove(0) );
        twoArr.add( arrays.remove(0) );
        return twoArr;
    }

    public synchronized void waitComputes(int idG) throws InterruptedException {
        while (values[idG-1] != null)
            wait();
    }
    private boolean isFull() {
        boolean full = true;
        for(Integer v : values) {
            if (v == null) {
                full = false;
                break;
            }
        }

        return full;
    }
}

class FinalQueue{
    List<Integer[]> results = new ArrayList<>();
    public synchronized void putResults(Integer[] array){
        results.add(array);
        notifyAll();
    }

    public synchronized Integer[] getResults() throws InterruptedException {
        while ( results.size() == 0 )
            wait();
//        Integer[] result = results.remove(0);
//        return result;
        return results.remove(0);           //? funziona??
    }
}

class Generator extends Thread{
    int id;
    Collector collector;
    int value;
    int t;      //* tempo max di attesa

    public Generator(int id, Collector collector, int t) {
        this.id = id;
        this.collector = collector;
        this.t = t;
    }

    @Override
    public void run() {
        try{
            int i = id;
            while (true){
                value = i++;
                collector.waitComputes(id);         //* attende se non è ancora possibile insrire un nuovo valore
                collector.putValue(id, value);      //* inserisce un nuovo valore
                sleep( (int)(Math.random()*t) );
            }
        }catch (InterruptedException e){}
    }
}

class Processor extends Thread{
    Collector collector;
    FinalQueue finalQueue;
    int id;
    Integer[] sums;

    public Processor(Collector collector, FinalQueue finalQueue, int id) {
        this.collector = collector;
        this.finalQueue = finalQueue;
        this.id = id;
    }

    @Override
    public void run() {
        try{
            while (true){

                ArrayList<Integer[]> arrays = collector.getArrays();
                Integer[] sums0 = arrays.remove(0);
                sums = arrays.remove(0);
                for (int i = 0; i< sums.length; i++){
                    sums[i] += sums0[0];
                }
                finalQueue.putResults(sums);
            }
        }catch (InterruptedException e){}
    }
}

class Printer  extends Thread{
    FinalQueue finalQueue;
    int nArray = 0;
    public Printer(FinalQueue finalQueue) {
        this.finalQueue = finalQueue;
    }

    @Override
    public void run() {
        try{
            while (nArray<10){
                Integer[] results = finalQueue.getResults();
                System.out.println("somme: "+ Arrays.toString(results));
                nArray++;
            }
        }catch (InterruptedException e) {}
    }
}

