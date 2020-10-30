package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    private static nachos.threads.KThread parentThread;
    private static int nchild_in_oahu, nadult_in_oahu, nchild_in_molokai;
    private static nachos.threads.Condition child_oahu, child_molokai, adult_oahu;
    private static nachos.threads.Lock lock;
    private static boolean adult_ready, need_pilot, boat_in_oahu, not_stop;

    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
  	begin(1, 2, b);

  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
    	bg = b;
    	parentThread = nachos.threads.KThread.currentThread();
    	nadult_in_oahu = adults;
    	nchild_in_oahu = children;
    	nchild_in_molokai = 0;

    	lock = new nachos.threads.Lock();
    	child_oahu = new nachos.threads.Condition(lock);
    	child_molokai = new nachos.threads.Condition(lock);
    	adult_oahu = new nachos.threads.Condition(lock);
    	adult_ready = false;
    	need_pilot = true;
    	boat_in_oahu = true;
    	not_stop = true;

    }

    static void AdultItinerary()
    {
    bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
    //DO NOT PUT ANYTHING ABOVE THIS LINE. 
	
	lock.acquire();
	while (!(adult_ready && boat_in_oahu)) adult_oahu.sleep();
	bg.AdultRowToMolokai();
	nadult_in_oahu --;
	adult_ready = false;
	boat_in_oahu = false;
	child_molokai.wake();
	lock.release();
	
    }

    static void ChildItinerary()
    {
	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
	lock.acquire();
	boolean im_in_oahu = true;
	while (!not_stop){
		if (im_in_oahu){
			while(adult_ready || !boat_in_oahu) child_oahu.sleep();
			nchild_in_oahu --;
			nchild_in_molokai ++;
			im_in_oahu = false;
			if (need_pilot){
				need_pilot = false;
				bg.ChildRowToMolokai();
				child_oahu.wake();
			}
			else{
				bg.ChildRideToMolokai();
				need_pilot = true;
				if (nadult_in_oahu == 0 && nchild_in_oahu == 0) not_stop = false;
				else {boat_in_oahu = false; child_molokai.wake();}
			}
			child_molokai.sleep();
		}
		else{
			while (boat_in_oahu) child_molokai.sleep();
			bg.ChildRideToOahu();
			im_in_oahu = true;
			boat_in_oahu = true;
			nchild_in_oahu ++;
			nchild_in_molokai --;
			if (nadult_in_oahu > 0 && nchild_in_molokai > 0) {adult_ready = true; adult_oahu.wake();}
			else child_oahu.wake();
			child_oahu.sleep();
		}
	}
	lock.release();
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
