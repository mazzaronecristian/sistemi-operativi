package Compito20lug2022;

import java.util.ArrayList;
import java.util.List;

public class Compito20lug2022 {
	public static void main(String[] args) throws InterruptedException{
		int m = 6;		//* numero dei voli
		int n = 4;		//* numero delle stazioni di check in

		IdProvider provider = new IdProvider();
		CodaBagagli coda = new CodaBagagli();

		StazioneCheckIn[] checkIn = new StazioneCheckIn[n];
		StazioneGestioneBagagli[] gestioneBagagli = new StazioneGestioneBagagli[m*2];  //* due gestori per volo

		for(int i = 0; i < checkIn.length; i++){
			checkIn[i] = new StazioneCheckIn(coda, provider);
			checkIn[i].setName("Check in " + i);
			checkIn[i].start();
		}

		for(int i = 0; i < m; i++){
			gestioneBagagli[i*2] = new StazioneGestioneBagagli(coda, i);
			gestioneBagagli[i*2].start();

			gestioneBagagli[i*2+1] = new StazioneGestioneBagagli(coda, i);
			gestioneBagagli[i*2+1].start();
		}

		for(int i = 0; i < 10; i++){
			try{
				Thread.sleep(1000);
			}catch (InterruptedException e){
				System.out.println("interrotto");
			}
			System.out.println("numero bagali nel sistema: " + provider.id + " " + "numero di bagagli nella coda: " + coda.coda.size() + " " +
					"numero di bagagli in attesa: "+ StazioneGestioneBagagli.nAttesa + " "+
					"numero di bagagli consegnati: "+ StazioneGestioneBagagli.nConsegnati);
		}

		Thread.sleep(10000);		//* aspetta 10 secondi
		for (int i = 0; i < checkIn.length; i++){
			checkIn[i].interrupt();
		}

		while(coda.coda.size() > 0){
			System.out.println("numero bagali nel sistema: " + provider.id + " " + "numero di bagagli nella coda: " + coda.coda.size() + " " +
					"numero di bagagli in attesa: "+ StazioneGestioneBagagli.nAttesa + " "+
					"numero di bagagli consegnati: "+ StazioneGestioneBagagli.nConsegnati);
		}
		coda.setFinito();

		while(StazioneGestioneBagagli.nAttesa != 0){
			Thread.sleep(500);
			System.out.println("numero bagali nel sistema: " + provider.id + " " + "numero di bagagli nella coda: " + coda.coda.size() + " " +
					"numero di bagagli in attesa: "+ StazioneGestioneBagagli.nAttesa + " "+
					"numero di bagagli consegnati: "+ StazioneGestioneBagagli.nConsegnati);
		}

		for (int i = 0; i<gestioneBagagli.length;i++){
			gestioneBagagli[i].interrupt();
		}
	}
}

class Bagaglio{
		int peso;
		int nVolo;
		int id;
		
		public Bagaglio(int peso, int nVolo, int id) {
			this.peso = peso;
			this.nVolo = nVolo;
			this.id = id;
		}
}

class IdProvider {
	int id = 0;         //* numero di bagagli inseriti nel sistema
	public synchronized int getId() {
		return ++id;
	}
}

class CodaBagagli{
	List<Bagaglio> coda = new ArrayList<Bagaglio>();
	boolean finito = false;
	public synchronized void putBagaglio(Bagaglio b) {
		coda.add(b);
		notifyAll();
	}
	public synchronized Bagaglio getBagaglio(int nVolo) throws InterruptedException{
		int pos = -1;
		while(!finito && (pos = checkBag(nVolo)) == -1) {  //* se la coda è vuota attende, ma attende anche se non c'è nessuno bagaglio per quel volo all'interno della coda
			wait();
		}
		if(finito){
			return null;  					//* non c'è bagaglio per quel volo
		}
		Bagaglio b = coda.remove(pos);  	//* prende il bagaglio
		return b;
	}

	private int checkBag(int nVolo){  		//* controlla se c'è un bagaglio per quel volo
		int pos = -1;  						//* posizione del bagaglio
		for(int i=0; i<coda.size(); i++){  	//* scorre la coda
			if(coda.get(i).nVolo == nVolo){	//* se trova un bagaglio per quel volo
				pos = i;   					//* salva la posizione
				break;  					//* se trova un bagaglio per quel volo esce dal ciclo
			}
		}
		return pos;
	}

	public  synchronized void setFinito(){
		finito = true;
		notifyAll();
	}
}

class StazioneCheckIn extends Thread{
	public CodaBagagli coda;
	public IdProvider idProvider;

	public StazioneCheckIn(CodaBagagli coda, IdProvider idProvider) {
		this.coda = coda;
		this.idProvider = idProvider;
	}

	public void run(){
		try{
			while(true){
				int peso = (int) ((Math.random() * 15) + 5);
				int nVolo = (int)(Math.random() * 6);
				int idBagaglio = idProvider.getId();
				Bagaglio b = new Bagaglio(peso, nVolo, idBagaglio);  //* creazione di un nuovo bagaglio

				coda.putBagaglio(b);

				sleep(200);
			}
		}catch (InterruptedException e){
			System.out.println(getName() + "interrotto");
		}
	}
}

class StazioneGestioneBagagli extends Thread{
	public CodaBagagli coda;
	public int nVolo;
	static int nAttesa = 0;
	static int nConsegnati = 0;

	public StazioneGestioneBagagli(CodaBagagli coda, int nVolo) {
		this.coda = coda;
		this.nVolo = nVolo;
	}

	public void run(){
		try{
			int peso = 0;
			int inAttesa = 0;
			while(true){
				while(peso < 100 && !coda.finito){		//* sto caricando roba sul trattore
					Bagaglio bagaglio = coda.getBagaglio(nVolo);
					if(bagaglio == null)
						break;
					peso += bagaglio.peso;
					inAttesa ++;
					synchronized (StazioneGestioneBagagli.class){
						nAttesa++;
					}
				}
				//* ha finito di caricare la roba sul trattore
				if(inAttesa > 0){
					sleep(2000);		//* sto inviando i bagagli al volo
					synchronized (StazioneGestioneBagagli.class){
						nAttesa -= inAttesa;
						nConsegnati += inAttesa;
					}
				}
				peso = 0;
				inAttesa = 0;
			}

		}catch(InterruptedException e){
			System.out.println("interrotto");
		}
	}
}

