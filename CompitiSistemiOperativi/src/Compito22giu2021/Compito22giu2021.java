package Compito22giu2021;

import java.util.ArrayList;

public class Compito22giu2021 {
    public static void main(String[] args) throws InterruptedException {
        int m = 5;
        int n = 10;
        int k = 7;
        LocationTracker locationTracker = new LocationTracker();
        ImageQueue imageQueue = new ImageQueue(k);
        Vehicle[] vehicles = new Vehicle[n];
        Uploader[] uploaders = new Uploader[vehicles.length];
        LocalQueue[] localQueues = new LocalQueue[vehicles.length];
        ImageCollector[] imageCollectors = new ImageCollector[m];

        for (int i = 0; i<imageCollectors.length; i++){
            imageCollectors[i] = new ImageCollector(imageQueue);
            imageCollectors[i].setName("I"+i);
            imageCollectors[i].start();
        }

        for (int i = 0; i<vehicles.length; i++){
            localQueues[i] = new LocalQueue();
            vehicles[i] = new Vehicle(locationTracker,localQueues[i], (float)(Math.random()*20-10),(float)(Math.random()*20-10));
            vehicles[i].setName("V"+i);
            vehicles[i].start();
            uploaders[i] = new Uploader(localQueues[i], imageQueue);
            uploaders[i].setName("U"+i);
            uploaders[i].start();
        }

        for(int i = 0; i< 30; i++){
            //TODO: aggiungere le stampe di debug
            Thread.sleep(1000);
        }
        for (int i = 0; i<vehicles.length; i++){
            vehicles[i].interrupt();
            vehicles[i].join();
            uploaders[i].interrupt();
            uploaders[i].join();
        }
        for (int i = 0; i<imageCollectors.length; i++){
            imageCollectors[i].interrupt();
            imageCollectors[i].join();
        }

        //TODO: stampare i vari valori richeisti nel testo

    }
}

class Image{
    int prior; //* 0 = bassa priorità, 1 = media priorità, 2 = alta priorità
    String owner;

    public Image(int prior, String owner){
        this.prior = prior;
        this.owner = owner;
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
class LocalQueue{
    ArrayList<Image> imagesH = new ArrayList<Image>();
    ArrayList<Image> imagesM = new ArrayList<Image>();
    ArrayList<Image> imagesB = new ArrayList<Image>();

    public synchronized void put(Image image){
        if(image.prior == 2)
            imagesH.add(image);
        else if(image.prior == 1)
            imagesM.add(image);
        else imagesB.add(image);
        notifyAll();
    }
    public synchronized Image get() throws InterruptedException {
        while(imagesH.size()==0 && imagesM.size()==0 && imagesB.size() == 0)
            wait();
        if(imagesH.size() > 0)
            return imagesH.remove(0);
        else if(imagesM.size() > 0)
            return imagesM.remove(0);
        return imagesB.remove(0);
    }
}

class Vehicle extends Thread{
    LocalQueue localQueue;
    LocationTracker locationTracker;
    float x, y;

    public Vehicle(LocationTracker locationTracker, LocalQueue localQueue, float x, float y){
        this.locationTracker = locationTracker;
        this.localQueue = localQueue;
        this.x = x;
        this.y = y;
    }

    public void run(){
        try{
            while(true){
                move();
                int prior = locationTracker.getPrior(x, y);
                Image img = new Image(prior, getName());
                System.out.println(getName()+" ha creato una immagine");
                localQueue.put(img);
                System.out.println(getName()+" ha messo una immagine in coda locale");
                sleep(1000);
            }
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

class Uploader extends Thread{
    LocalQueue localQueue;
    ImageQueue imageQueue;

    public Uploader(LocalQueue localQueue, ImageQueue imageQueue) {
        this.localQueue = localQueue;
        this.imageQueue = imageQueue;
    }

    public void run(){
        try{
            while (true){
                Image img = localQueue.get();
                System.out.println(getName()+" ha preso una immagine nella coda locale");
                imageQueue.putImage(img);
                System.out.println(getName()+" ha messo una immagine nella coda globale");
                sleep(500);
            }
        }catch (InterruptedException e){

        }
    }
}

class ImageCollector extends Thread{
    ImageQueue imageQueue;

    public ImageCollector(ImageQueue imageQueue) {
        this.imageQueue = imageQueue;
    }

    @Override
    public void run() {
        try{
            while (true){
                imageQueue.getImage();
                System.out.println(getName()+" ha preso una immagine dalla coda globale");
                sleep(2000);
            }
        }catch (InterruptedException e){

        }
    }
}