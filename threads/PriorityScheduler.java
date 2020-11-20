package nachos.threads;

import nachos.machine.*;
import java.util.*;

public class PriorityScheduler extends Scheduler {
	public PriorityScheduler() {
	}

	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}
   
	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum &&
				priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority+1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority-1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public static int priorityDefault = 1;
	public static int priorityMinimum = 0;
	public static int priorityMaximum = 7;

	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	protected class PriorityQueue extends ThreadQueue {
		PriorityQueue() {
			this.transferPriority = true;
			init();
		}
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			init();
		}
		public void init() {
			cnt = 0;
			waitSet = new TreeSet[ priorityMaximum + 1];
			for(int i = 0; i <= priorityMaximum; i ++)
				waitSet[i]=new TreeSet<ThreadState>();
		}

		public void waitForAccess(nachos.threads.KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
			if(transferPriority)
				lockholder=getThreadState(thread);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState res=pickNextThread();

			return res == null ? null : res.thread;
		}

		protected ThreadState pickNextThread() {
			ThreadState res=NextThread();

			if(lockholder!=null) {
				lockholder.holdQueues.remove(this);
				lockholder.getEffectivePriority();
				lockholder=res;
			}
			if(res!=null) res.waitQueue=null;
			return res;
		}

		protected ThreadState NextThread() {
			ThreadState res = null;

			for(int i = priorityMaximum ; i >= priorityMinimum ; i--)
				if((res=waitSet[i].pollFirst())!=null) break;

			return res;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
		}

		public void add(ThreadState state) {
			waitSet[state.effectivepriority].add(state);
		}

		public boolean isEmpty() {
			for(int i = 0 ; i <= priorityMaximum; i++)
				if(!waitSet[i].isEmpty()) return false;
			return true;
		}

		protected long cnt;
		public boolean transferPriority;
		protected TreeSet<ThreadState>[] waitSet;
		protected ThreadState lockholder=null;
	}

	protected class ThreadState implements Comparable<ThreadState> {
		public ThreadState() {}
		public ThreadState(KThread thread) {
			this.thread = thread;
			holdQueues=new LinkedList<PriorityQueue>();

			setPriority(priorityDefault);
			getEffectivePriority();
		}

		public int getPriority() {
			return priority;
		}

		public int getEffectivePriority() {
			int res=priority;
			if(!holdQueues.isEmpty()) {
				Iterator it=holdQueues.iterator();
				while(it.hasNext()) {
					PriorityQueue holdQueue=(PriorityQueue)it.next();
					for(int i=priorityMaximum;i>res;i--)
						if(!holdQueue.waitSet[i].isEmpty()) { res=i;break;}
				}
			}
			if(waitQueue!=null&&res!=effectivepriority) {
				((PriorityQueue)waitQueue).waitSet[effectivepriority].remove(this);
				((PriorityQueue)waitQueue).waitSet[res].add(this);
			}
			effectivepriority=res;
			if(lockholder!=null)
				lockholder.getEffectivePriority();
			return res;
		}

		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;

			getEffectivePriority();
		}

		public void waitForAccess(PriorityQueue waitQueue) {
			Lib.assertTrue(Machine.interrupt().disabled());

			time = ++waitQueue.cnt;

			this.waitQueue=waitQueue;
			waitQueue.add(this);
			lockholder=waitQueue.lockholder;
			getEffectivePriority();
		}

		public void acquire(PriorityQueue waitQueue) {
			Lib.assertTrue(Machine.interrupt().disabled());

			if(waitQueue.transferPriority) holdQueues.add(waitQueue);
			Lib.assertTrue(waitQueue.isEmpty());
		}

		protected nachos.threads.KThread thread;
		protected int priority,effectivepriority;
		protected long time;
		protected nachos.threads.ThreadQueue waitQueue=null;
		protected LinkedList holdQueues;
		protected ThreadState lockholder=null;

		public int compareTo(ThreadState ano) {
			if(time == ano.time) return 0 ;
			return time > ano.time ? 1 : -1 ;
		}
	}
}