package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList ;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        lock = new nachos.threads.Lock() ;
        q = new LinkedList<Integer>() ;
        speakCondition = new nachos.threads.Condition(lock) ;
        listenCondition = new nachos.threads.Condition(lock) ;
        speakercnt = 0;
        listenercnt = 0 ;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        boolean status = nachos.machine.Machine.interrupt().disabled() ;
        lock.acquire() ;
        if (listenercnt == 0) {
            speakercnt ++ ;
            speakCondition.sleep() ;
            q.add(word) ;
            listenCondition.wake() ;
            speakercnt -- ;
        }
        else {
            q.add(word) ;
            listenCondition.wake() ;
        }
        lock.release();
        nachos.machine.Machine.interrupt().restore(status) ;
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
        boolean status = nachos.machine.Machine.interrupt().disabled() ;
        lock.acquire() ;
       // System.out.println("heihei") ;
        if (speakercnt > 0) {
            speakCondition.wake() ;
            listenCondition.sleep() ;
        }
        else {
            listenercnt ++ ;
            listenCondition.sleep() ;
            listenercnt -- ;
        }
        int message = q.poll() ;
        lock.release();
        nachos.machine.Machine.interrupt().restore(status) ;
        return message;
    }
    private nachos.threads.Lock lock;
    private nachos.threads.Condition speakCondition, listenCondition;
    private int word,speakercnt,listenercnt;
    private LinkedList<Integer> q;
}
