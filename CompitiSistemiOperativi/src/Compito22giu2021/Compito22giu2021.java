package Compito22giu2021;

import java.util.ArrayList;

public class Compito22giu2021 {
    public static void main(String[] args) {

    }
}

class Image{
    int prior; //* 0 = bassa priorità, 1 = media priorità, 2 = alta priorità

    public Image(int prior){
        this.prior = prior;
    }

}

class LocationTracker{
    int nRequest = 0; //* n tot di richieste
    int[] counterPerPrior = {0,0,0};

    public synchronized int getPrior(float x, float y){
        nRequest++;
        if(dist(x, y) < 5){ //* priorità alta
            counterPerPrior[2]++;
            return 2;
        }
        if(dist(x, y) > 5 && dist(x, y) < 10){ //* priorità media
            counterPerPrior[1]++;
            return 1;
        }
        counterPerPrior[0]++; //* priorità bassa
        return 0;
    }

    private float dist(float x, float y){
        return (float) Math.sqrt( x * x + y * y );
    }
}

class ImageQueue{
    ArrayList<Image> images = new ArrayList<Image>();
    int k;
    int[] counterPerPrior = {0,0,0};

    public ImageQueue(int k){
       this.k = k;
    }

    public synchronized void putImage(Image img) throws InterruptedException {
        while(images.size() == k){
            wait();
        }
        counterPerPrior[img.prior]++;
        images.add(img);
        notifyAll(); //* notifica agli image collector che l'immagine è stata inserita
    }

    public synchronized Image getImage() throws InterruptedException {
        while(images.size() == 0){
            wait();
        }
        Image img = images.remove(0); //* acquisisco l'immagine
        notifyAll();
        return img;
    }
}

class Vehicle extends Thread{
    ArrayList<Image> localQueue = new ArrayList<Image>();
    LocationTracker locationTracker;
    float x, y;

    public Vehicle(LocationTracker locationTracker, float x, float y){
        this.locationTracker = locationTracker;
        this.x = x;
        this.y = y;
    }

    public void run(){
        try{
            move();
            int prior = locationTracker.getPrior(x, y);
            Image img = new Image(prior);
            synchronized (Vehicle.class){
                localQueue.add(img);
                notify(); //* basta notify perchè ha soltanto un uploader
            }
            sleep(1000);
        }catch (InterruptedException e) {
            System.out.println("interrotto");
        }
    }

    private void move(){
        float dx = (float) ( Math.random()*2 - 1 );
        float dy = (float) ( Math.random()*2 - 1 );
        x += dx;
        y += dy;
    }
}