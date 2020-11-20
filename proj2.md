### File System Calls

We basically just call the functions provided by `ThreadedKernel.fileSystem` as well as checking the bound of indices and file descriptors. We initialize every process with `stdin` and `stdout` opened as the file descriptors 0 and 1. 

### Memory Management for Multiprogramming

This part is mainly about page tables. In `UserKernel`, we maintain a `LinkedList` of available pages. In `loadSections`, we first check whether there are enough free pages, then make a page table which maps virtual addresses to physical addresses, and mark the physical pages as occupied. In `unloadSections`, the physical pages are marked free again so that we can reuse the memory when the process exits. All operations on virtual memories are converted to operations on the physical memory by the page table. We have to be careful with these address translations so that we skip no address.

### `exec, join, exit`

To implement these 3 system calls, we need to maintain PID, and the PID of its parent and child for each `UserProcess`.

* When `exec` is invoked, we start a new process initialized with a new PID (according to an increasing counter). Then we set its parent to be the running process, and add the new process to the children of the running process.

* When `join` is invoked, we check whether the PID is a child of the running process. If it is not, we return -1. Then the process sleeps using a condition variable until the child wakes it up when the child `exit`.

* When `exit` is invoked, we close all opened files, wake up its parent, remove itself from the parent's children, invoke `unloadSections`, halt if it is the final process, and finish the thread. 

### Lottery Scheduler

In lottery scheduling, the next thread is selected randomly: each thread gets some lottery tickets, and their chance of winning the next time slot is proportional to the number of their 'effective lottery tickets' where a waiting thread could donate its tickets to the queue owner.

We implement random selection in `LotteryThreadState NextThread()`. 

### Test cases

test/selfcase.c is the test case for the first 3 tasks. We have several test functions for exit, halt, memory operations, exec, launch and system call. 

For lottery scheduler we have one test in `KThread.java` named `selftest_lotteryscheduler()`.  


