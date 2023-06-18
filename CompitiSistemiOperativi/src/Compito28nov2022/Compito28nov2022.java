package Compito28nov2022;

import java.util.ArrayList;
import java.util.Arrays;

public class Compito28nov2022 {
    public static void main(String[] args) throws InterruptedException {
        int n = 5, m = 7, k = 4, x = 100;

        Sensor[] sensor = new Sensor[n];
        for(int i=0; i< sensor.length; i++){
            sensor[i] = new Sensor(x, i, k);
            sensor[i].start();
        }
        ValueManager vm = new ValueManager(m);
        AcquisitionThread at = new AcquisitionThread(sensor, vm);
        at.start();

        Processor[] processor = new Processor[m];
        for(int i=0; i< processor.length; i++){
            processor[i] = new Processor(vm, i);
            processor[i].start();
        }

        Thread.sleep(10000);

        for(Sensor s: sensor){
            s.interrupt();
            s.join();
            System.out.println("sensore"+s.id + " ha letto "+s.nRead+" volte"+", valori in coda:"+ s.maq.maq.size());
        }

        at.interrupt();
        at.join();
        System.out.println("Acquisition ha operato: "+ at.nP+" volte");

        for(Processor p: processor){
            p.interrupt();
            p.join();
            System.out.println("processor"+p.id + " ha letto:"+p.nP+" volte");
        }
    }
}

class MovingAvgQueue{
    ArrayList<Double> maq = new ArrayList<>();
    int k;

    public MovingAvgQueue(int k) {
        this.k = k;
    }

    public synchronized void putValue(double value) {
        maq.add(value);
        notifyAll();
    }

    public synchronized double getAvg() throws InterruptedException{
        while(maq.size() < k){
            wait();
        }
        double sum = 0;
        for (int i = 0; i < k ; i++){
            sum += maq.get(i);
        }
        maq.remove(0);
        return sum / k;
    }

}

class ValueManager{
    Double[] values = null;
    int m;
    int Pid = 0;

    public ValueManager(int m){
        this.m = m;
    }

    public synchronized void putArray(Double[] vv) throws InterruptedException{
        while ( values != null){
            wait();
        }
        values = vv;
        notifyAll();
    }

    public synchronized Double[] getArray(int Pid) throws InterruptedException{
        while ( values == null || this.Pid != Pid){
            wait();
        }
        Double[] w = values;
        values = null;
        this.Pid = (Pid + 1) % m;
        notifyAll();
        return w;
    }
}

class Sensor extends Thread{
    MovingAvgQueue maq;
    int value;
    int x;
    int id;
    int nRead = 0;

    public Sensor(int x, int id, int k) {
        this.maq = new MovingAvgQueue(k);  //* così è più pulito!
        this.x = x;
        this.id = id;
    }

    public void run(){
        try{
            while(true){
                value = (id * 1000)+nRead++;
                sleep(x);
                maq.putValue(value);
            }
        }catch (InterruptedException e){}
    }
}

class AcquisitionThread extends Thread{
    Sensor[] sensor;
    ValueManager vm;
    int nP = 0;

    public AcquisitionThread(Sensor[] sensor, ValueManager vm){
        this.sensor = sensor;
        this.vm = vm;
    }

    public void run(){
        try{
            while (true){
                Double[] values = new Double[sensor.length];
                for(Sensor s: sensor)
                    values[s.id] = s.maq.getAvg();
                vm.putArray(values);
                nP++;
            }
        }catch (InterruptedException e){}
    }
}

class Processor extends Thread{
    ValueManager vm;
    int id;
    int nP = 0;

    public Processor(ValueManager vm, int id) {
        this.vm = vm;
        this.id = id;
    }

    public void run() {
        try{
            while(true){
                Double[] values = vm.getArray(id);
                nP++;
                System.out.println("Processor"+id+" "+Arrays.toString(values));
                sleep((int) (Math.random() * 3 + 1) *1000);
            }
        }catch (InterruptedException e){}
    }
}


