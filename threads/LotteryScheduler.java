package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    public static int priorityDefault = 1;
    public static int priorityMinimum = 1;
    public static int priorityMaximum = Integer.MAX_VALUE;
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new LotteryQueue(transferPriority);
    }
    protected class LotteryQueue extends PriorityQueue {
        LotteryQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
            waitQueue = new LinkedList<LotteryThreadState>();
        }
        protected LotteryThreadState NextThread() {
            if (isEmpty()) return null;
            int sum = 0;
            for (int i=0;i<waitQueue.size();i++)
                sum += waitQueue.get(i).getEffectivePriority();
            int pick = Lib.random(sum) + 1;
            for (int i=0;i<waitQueue.size();i++) {
                int tmp_priority = waitQueue.get(i).getEffectivePriority();
                if (pick <= tmp_priority) {
                    return waitQueue.get(i);
                }
                pick -= tmp_priority;
            }
            return null;
        }
        public void add(LotteryThreadState state) {
            waitQueue.add(state);
        }

        public boolean isEmpty() {
            return waitQueue.isEmpty();
        }
        public LinkedList<LotteryThreadState> waitQueue;
    }
    protected class LotteryThreadState extends ThreadState {
        public LotteryThreadState(KThread thread) {
            this.thread = thread;
            holdQueues=new LinkedList<LotteryQueue>();
            
            setPriority(priorityDefault);
            getEffectivePriority();
        }
        public int getEffectivePriority() {
            int ret = priority;
            for (int i = 0; i < waitQueue.waitQueue.size(); ++i)
                ret += waitQueue.waitQueue.get(i).getEffectivePriority();
            return ret;
        }
        protected LotteryQueue waitQueue;
    }
}
