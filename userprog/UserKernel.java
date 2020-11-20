package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
public class UserKernel extends ThreadedKernel {
    public UserKernel() {
	super();
    }
    public void initialize(String[] args) {
	super.initialize(args);
	console = new SynchConsole(Machine.console());	
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	});
	fileInfo = new HashMap<>();
	fileinfoLock = new Lock();
	pageLock = new Lock();
	pageLock.acquire();
	freePages = new ArrayList<Integer>();
	int num = Machine.processor().getNumPhysPages();
	for (int i=0;i<num;i++) freePages.add(i);
	pageLock.release();
	exitLock = new Lock();
	exitCode = new HashMap<Integer,Integer>();
	stdoutLock = new Lock();
    }	
    public void selfTest() {
	super.selfTest();
    }
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread)) return null;
	return ((UThread) KThread.currentThread()).process;
    }
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);
	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }
    public void run() {
	super.run();
	UserProcess process = UserProcess.newUserProcess();
	String shellProgram = Machine.getShellProgramName();	
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));
	KThread.currentThread().finish();
    }
    public void terminate() {
	super.terminate();
    }
    public static SynchConsole console;
    private static Coff dummy1 = null;
    protected static class fileInfoStruct {
	boolean tounlink;
        int count;
    }
    protected static ArrayList<Integer> freePages;
    protected static Lock pageLock;
    protected static Map<Integer,Integer> exitCode;
    protected static Lock fileinfoLock;
    protected static Map<String, fileInfoStruct> fileInfo;
    protected static Lock exitLock;
    protected static Lock stdoutLock;
}
