package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        boolean status = nachos.machine.Machine.interrupt().disable();
        long curTime = nachos.machine.Machine.timer().getTime() ;
        while (ThreadList.size() > 0) {
            //System.out.println(curTime+"  "+TimeList.peek());
            if (TimeList.peek() <= curTime) {
                //System.out.println("sdhklghdshl"+TimeList.peek());
                ThreadList.poll().ready();
                TimeList.poll();
            }
            else
                break;
        }
        KThread.currentThread().yield();
        nachos.machine.Machine.interrupt().restore(status) ;
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
    boolean status = nachos.machine.Machine.interrupt().disable() ;
	long wakeTime = Machine.timer().getTime() + x;
	if (ThreadList.size() == 0){
        //System.out.println(wakeTime);
        ThreadList.add(nachos.threads.KThread.currentThread());
        TimeList.add(wakeTime);
    }
    else{
    int cnt = 0;
    for (long time : TimeList){
        if (wakeTime < time){
            //System.out.println(x);
            TimeList.add(cnt, wakeTime);
            ThreadList.add(cnt, nachos.threads.KThread.currentThread());
            break;
        }
        cnt += 1;
    }
    if (cnt == TimeList.size()){
        //System.out.println(x);
        ThreadList.add(nachos.threads.KThread.currentThread());
        TimeList.add(wakeTime);
    }
    }
    nachos.threads.KThread.sleep() ;
    nachos.machine.Machine.interrupt().restore(status) ;
    }
    private LinkedList<nachos.threads.KThread> ThreadList = new LinkedList<nachos.threads.KThread>() ;
    private LinkedList<Long > TimeList = new LinkedList<Long>() ;


}
