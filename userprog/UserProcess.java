package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.ArrayList;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
    boolean intStatus = Machine.interrupt().disable();
    pid = ++processcnt;

    openfiles = new OpenFile[16];
    openfiles[0] = UserKernel.console.openForReading();
    openfiles[1] = UserKernel.console.openForWriting();
    children = new ArrayList<UserProcess>();
    Machine.interrupt().restore(intStatus);
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return  a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
    return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param   name    the name of the file containing the executable.
     * @param   args    the arguments to pass to the executable.
     * @return  <tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
    if (!load(name, args))
        return false;
    
    running_cnt++;
    uthread = new UThread(this);
    uthread.setName(name).fork();

    return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
    Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param   vaddr   the starting virtual address of the null-terminated
     *          string.
     * @param   maxLength   the maximum number of characters in the string,
     *              not including the null terminator.
     * @return  the string read, or <tt>null</tt> if no null terminator was
     *      found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
    Lib.assertTrue(maxLength >= 0);

    byte[] bytes = new byte[maxLength+1];

    int bytesRead = readVirtualMemory(vaddr, bytes);

    for (int length=0; length<bytesRead; length++) {
        if (bytes[length] == 0)
        return new String(bytes, 0, length);
    }

    return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param   vaddr   the first byte of virtual memory to read.
     * @param   data    the array where the data will be stored.
     * @return  the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
    return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param   vaddr   the first byte of virtual memory to read.
     * @param   data    the array where the data will be stored.
     * @param   offset  the first byte to write in the array.
     * @param   length  the number of bytes to transfer from virtual memory to
     *          the array.
     * @return  the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
                 int length) {
    Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

    byte[] memory = Machine.processor().getMemory();
    
    int memorylength = numPages * pageSize;
    if (vaddr < 0 || vaddr >= memorylength)
        return 0;

    int amount = Math.min(length, memorylength-vaddr);
    if(amount  == 0) return 0;

    int vend = vaddr + amount -1;
    //System.arraycopy(memory, vaddr, data, offset, amount);

    int begin_ind = Machine.processor().pageFromAddress(vaddr);
    int end_ind = Machine.processor().pageFromAddress(vend);
    int read_cnt = 0;
    boolean writing = false;
    for (int vpn = begin_ind; vpn <= end_ind; vpn++ ){
        TranslationEntry entry = pageTable[vpn];
        int begin = Math.max(vaddr, vpn * pageSize);
        int end = Math.min(vend, vpn*pageSize + pageSize - 1);
        int address_offset = Machine.processor().offsetFromAddress(begin);

        if (!entry.valid || entry.readOnly && writing) break;
        int ppn = entry.ppn;
        if (ppn < 0 || ppn >= Machine.processor().getNumPhysPages()) break;
        entry.used = true;
        if (writing) entry.dirty = true;
        int membegin = (ppn*pageSize) + address_offset;
        System.arraycopy(memory, membegin, data, offset, end - begin + 1);
        offset += end-begin+1;
        read_cnt += end-begin+1;
    }
    return read_cnt;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param   vaddr   the first byte of virtual memory to write.
     * @param   data    the array containing the data to transfer.
     * @return  the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
    return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param   vaddr   the first byte of virtual memory to write.
     * @param   data    the array containing the data to transfer.
     * @param   offset  the first byte to transfer from the array.
     * @param   length  the number of bytes to transfer from the array to
     *          virtual memory.
     * @return  the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
                  int length) {
    Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

    byte[] memory = Machine.processor().getMemory();
    
    int memorylength = numPages * pageSize;
    if (vaddr < 0 || vaddr >= memorylength)
        return 0;

    int amount = Math.min(length, memorylength-vaddr);
    if(amount  == 0) return 0;

    int vend = vaddr + amount -1;
    //System.arraycopy(data, offset, memory, vaddr, amount);

    int begin_ind = Machine.processor().pageFromAddress(vaddr);
    int end_ind = Machine.processor().pageFromAddress(vend);
    int write_cnt = 0;
    boolean writing = true;
    for (int vpn = begin_ind; vpn <= end_ind; vpn++ ){
        TranslationEntry entry = pageTable[vpn];
        int begin = Math.max(vaddr, vpn * pageSize);
        int end = Math.min(vend, vpn*pageSize + pageSize - 1);
        int address_offset = Machine.processor().offsetFromAddress(begin);

        if (!entry.valid || entry.readOnly && writing) break;
        int ppn = entry.ppn;
        if (ppn < 0 || ppn >= Machine.processor().getNumPhysPages()) break;
        entry.used = true;
        if (writing) entry.dirty = true;
        int membegin = (ppn*pageSize) + address_offset;
        System.arraycopy(data, offset, memory, membegin, end - begin + 1);
        offset += end-begin+1;
        write_cnt += end-begin+1;
    }
    return write_cnt;

    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param   name    the name of the file containing the executable.
     * @param   args    the arguments to pass to the executable.
     * @return  <tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
    Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
    
    OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
    if (executable == null) {
        Lib.debug(dbgProcess, "\topen failed");
        return false;
    }

    try {
        coff = new Coff(executable);
    }
    catch (EOFException e) {
        executable.close();
        Lib.debug(dbgProcess, "\tcoff load failed");
        return false;
    }

    // make sure the sections are contiguous and start at page 0
    numPages = 0;
    for (int s=0; s<coff.getNumSections(); s++) {
        CoffSection section = coff.getSection(s);
        if (section.getFirstVPN() != numPages) {
        coff.close();
        Lib.debug(dbgProcess, "\tfragmented executable");
        return false;
        }
        numPages += section.getLength();
    }

    // make sure the argv array will fit in one page
    byte[][] argv = new byte[args.length][];
    int argsSize = 0;
    for (int i=0; i<args.length; i++) {
        argv[i] = args[i].getBytes();
        // 4 bytes for argv[] pointer; then string plus one for null byte
        argsSize += 4 + argv[i].length + 1;
    }
    if (argsSize > pageSize) {
        coff.close();
        Lib.debug(dbgProcess, "\targuments too long");
        return false;
    }

    // program counter initially points at the program entry point
    initialPC = coff.getEntryPoint();   

    // next comes the stack; stack pointer initially points to top of it
    numPages += stackPages;
    initialSP = numPages*pageSize;

    // and finally reserve 1 page for arguments
    numPages++;

    if (!loadSections())
        return false;

    coff.close();////// avoid strange bugs
    // store arguments in last page
    int entryOffset = (numPages-1)*pageSize;
    int stringOffset = entryOffset + args.length*4;

    this.argc = args.length;
    this.argv = entryOffset;
    
    for (int i=0; i<argv.length; i++) {
        byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
        Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
        entryOffset += 4;
        Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==argv[i].length);
        stringOffset += argv[i].length;
        Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
        stringOffset += 1;
    }

    return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return  <tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
        UserKernel.pageLock.acquire();
    if (numPages > UserKernel.freePages.size()) {
        UserKernel.pageLock.release();
        coff.close();
        Lib.debug(dbgProcess, "\tinsufficient physical memory");
        return false;
    }

    pageTable = new TranslationEntry[numPages];
    for (int i=0; i<numPages; i++) {
        int freepage = UserKernel.freePages.get(UserKernel.freePages.size()-1);
        UserKernel.freePages.remove(UserKernel.freePages.size()-1);
        pageTable[i] = new TranslationEntry(i,freepage, true,false,false,false);
    }
    UserKernel.pageLock.release(); 
    for (int s=0; s<coff.getNumSections(); s++) {
        CoffSection section = coff.getSection(s);
        Lib.debug(dbgProcess, "\tinitializing " + section.getName()
              + " section (" + section.getLength() + " pages)");
        for (int i=0; i<section.getLength(); i++) {
        int vpn = section.getFirstVPN()+i;
        section.loadPage(i, pageTable[vpn].ppn);
        pageTable[vpn].readOnly=section.isReadOnly();
        }
    }
    
    return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        for (int id = 0; id < 16; id++)
            if (openfiles[id] != null) handleClose(id);
        UserKernel.pageLock.acquire();
        for (int i=0; i<numPages; i++) {
            int freepage = pageTable[i].ppn;
            UserKernel.freePages.add(freepage);
        }
        UserKernel.pageLock.release();
        if(stdoutlock) {
                stdoutlock=false;
                UserKernel.stdoutLock.release();
        }
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
    Processor processor = Machine.processor();

    // by default, everything's 0
    for (int i=0; i<processor.numUserRegisters; i++)
        processor.writeRegister(i, 0);

    // initialize PC and SP according
    processor.writeRegister(Processor.regPC, initialPC);
    processor.writeRegister(Processor.regSP, initialSP);

    // initialize the first two argument registers to argc and argv
    processor.writeRegister(Processor.regA0, argc);
    processor.writeRegister(Processor.regA1, argv);
    }

    private static int remove(String name) {
        boolean flag = UserKernel.fileSystem.remove(name);
        UserKernel.fileInfo.remove(name);
        if (flag) return 0;
        else return -1;
    }
    private static int open(String name) {
        UserKernel.fileinfoLock.acquire();
        if  (!UserKernel.fileInfo.containsKey(name)) {
            UserKernel.fileInfo.put(name, new UserKernel.fileInfoStruct());
        }
        UserKernel.fileInfoStruct file_info = UserKernel.fileInfo.get(name);
        if (file_info.tounlink) {
            UserKernel.fileinfoLock.release();
            return -1;
        }
        file_info.count++;
        UserKernel.fileinfoLock.release();
        return 0;
    }
    private static int close(String name) {
        UserKernel.fileinfoLock.acquire();
        if  (!UserKernel.fileInfo.containsKey(name)) {
            UserKernel.fileinfoLock.release();
            return -1;
        }
        UserKernel.fileInfoStruct file_info = UserKernel.fileInfo.get(name);
        file_info.count--;
        if (file_info.tounlink && file_info.count == 0) {
            int ind = remove(name);
            UserKernel.fileinfoLock.release();
            return ind;
        } else {
            UserKernel.fileinfoLock.release();
            return 0;
        }
    }
    public static int unlink(String name){
        UserKernel.fileinfoLock.acquire();
        if  (!UserKernel.fileInfo.containsKey(name)) {
            UserKernel.fileInfo.put(name, new UserKernel.fileInfoStruct());
        }
        UserKernel.fileInfoStruct file_info = UserKernel.fileInfo.get(name);
        file_info.tounlink = true;

        if (file_info.count == 0) {
            int ind = remove(name);
            UserKernel.fileinfoLock.release();
            return ind;
        } else {
            UserKernel.fileinfoLock.release();
            return 0;
        }
    }
    private String getFileName(int name) {
            String filename = readVirtualMemoryString(name, 256);
            if (filename == null || filename.length() == 0) {
                    return null;
        }
        return filename;
    }
    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

        if (pid == 1) {
            unloadSections();
            Machine.halt();
            Lib.assertNotReached("Machine.halt() did not halt machine!");
            return -1;
        }
        
        return -1;
    }
    private int handleExec(int name, int argc, int argv) {
        if (argc<0 || argv<0 || name<0) return -1;
        String filename = getFileName(name);
        if (filename == null) return -1;
        if (filename.length()<5)return -1;
        if (!filename.substring(filename.length()-5,filename.length()).equals(".coff"))return -1;
        UserProcess child = UserProcess.newUserProcess();
        String[] arg = new String[argc];
        for (int i=0;i<argc;i++) {
            byte[] addrbytes = new byte[4];
            if (readVirtualMemory(argv + i * 4, addrbytes)<4) return -1;

            int addr = Lib.bytesToInt(addrbytes, 0);
            arg[i] = readVirtualMemoryString(addr, 256);
            if (arg[i] == null) return -1;
        }
        if (child.execute(filename, arg)) {
            children.add(child);
            return child.pid;
        } 
        else return -1;
    }
    private int handleJoin(int pid, int status) {
        UserProcess child = null;
        for (UserProcess t: children) {
                if (t.pid==pid) {
                        child= t;
                        break;
            }
        }
        if(child == null) return -1;
        if(child.uthread!=null)  child.uthread.join();
        children.remove(child);
        UserKernel.exitLock.acquire();
        if (!UserKernel.exitCode.containsKey(pid)) { 
            UserKernel.exitLock.release();
            return 0;
        }
        int exit_code = UserKernel.exitCode.get(pid);
        UserKernel.exitLock.release();
        if (exit_code ==EXCEPTION) return 0;
        byte[] codebyte = Lib.bytesFromInt(exit_code);
        if (writeVirtualMemory(status, codebyte)==0) return -1;
        return 1;
    }
    private int handleExit(int code) {
        Machine.interrupt().disable();
        unloadSections();
        running_cnt --;
        UserKernel.exitLock.acquire();
        UserKernel.exitCode.put(pid, code);
        UserKernel.exitLock.release();
        if (running_cnt == 0) Kernel.kernel.terminate();
        else uthread.finish();
        Lib.assertNotReached("Machine.Exit() Exit Error");
        return -1;
    }
    private int handleOpen(int name) {
        String filename = getFileName(name);
        if (filename==null) return -1;
        int id = 0;
        for (id = 0; id < 16; id++)
                if (openfiles[id] == null) break;
        if (id==16) return -1;
        OpenFile file = UserKernel.fileSystem.open(filename, false);
        if (file == null) return -1;
        if (open(filename) == -1) {
                file.close();
                return -1;
        }
        openfiles[id] = file;
        return id;
    }
    private int handleRead(int id, int buffer, int size) {
        if (id<0 || id>=16) return -1;
        if (size<0) return -1;
        if (buffer<0) return -1;
        OpenFile file = openfiles[id];
        if (file == null) return -1;

        int read_cnt = 0;
        byte[] mybuf = new byte[1024];
        while (size>0) {
            int toread = 1024;
            if (size<toread) toread = size;
            int read_num = file.read(mybuf, 0, toread);
            if (read_num==-1) return -1;
            int written = writeVirtualMemory(buffer, mybuf, 0, read_num);
            if (written < read_num) return -1;
            read_cnt += read_num;
            buffer += read_num;
            if (read_num<toread) break;
            size -= read_num;
        }
        return read_cnt;
    }
    private int handleCreate(int name) {
        String filename = getFileName(name);
        if (filename==null)return -1;
        int id = 0;
        for (id = 0; id < 16; id++) 
                if (openfiles[id] == null) break;
        if (id==16) return -1;
        OpenFile file = UserKernel.fileSystem.open(filename, true);
        if (file == null) return -1;
        if (open(filename) == -1) {
                file.close();
                return -1;
        }
        openfiles[id] = file;
        return id;
    }
    private int handleWrite(int id, int buffer, int size) {
        if (id<0 || id>=16) return -1;
        if (size<0) return -1;
        if (buffer<0)return -1;
        OpenFile file = openfiles[id];
        if (file == null) return -1;

        int write_cnt = 0;
        byte[] mybuf = new byte[1024];
        if(id==1) {
            UserKernel.stdoutLock.acquire();
            stdoutlock = true;
        }
        while (size > 0) {
            int towrite = 1024;
            if (size<towrite) towrite = size;

            int avail = readVirtualMemory(buffer, mybuf, 0, towrite);

            if (avail < towrite) {
                if(id==1) {
                    stdoutlock = false;
                    UserKernel.stdoutLock.release();
                }
                    return -1;
            }
            int written_num  = file.write(mybuf, 0, avail);
            if (written_num == -1 || written_num < avail ) {
                if(id==1) {
                    stdoutlock = false;
                        UserKernel.stdoutLock.release();
                }
                return -1;
            }
            write_cnt += written_num;
            buffer += written_num;
            size -= written_num;
        }
        if(id==1){
            stdoutlock = false;
            UserKernel.stdoutLock.release();
        }
        return write_cnt;
    }
    private int handleClose(int id) {
        if (id<0 || id>=16) return -1;
        if (openfiles[id]==null) return -1;
        OpenFile file = openfiles[id];
        String filename = file.getName();
        file.close();
        openfiles[id]=null;
        int ind = close(filename);
        return ind;
    }
    private int handleUnlink(int name) {
        String filename = getFileName(name);
        if (filename==null)return -1;
        int ind =  unlink(filename);
        return ind;
    }
    private static final int
    syscallHalt = 0,
    syscallExit = 1,
    syscallExec = 2,
    syscallJoin = 3,
    syscallCreate = 4,
    syscallOpen = 5,
    syscallRead = 6,
    syscallWrite = 7,
    syscallClose = 8,
    syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     *                              </tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *                              </tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *                              </tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param   syscall the syscall number.
     * @param   a0  the first syscall argument.
     * @param   a1  the second syscall argument.
     * @param   a2  the third syscall argument.
     * @param   a3  the fourth syscall argument.
     * @return  the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {

    switch (syscall) {
    case syscallHalt:
        return handleHalt();
    case syscallExit:
        return handleExit(a0);
    case syscallExec:
        return handleExec(a0, a1, a2);
    case syscallJoin:
        return handleJoin(a0, a1);
    case syscallCreate:
        return handleCreate(a0);
    case syscallOpen:
        return handleOpen(a0);
    case syscallRead:
        return handleRead(a0, a1, a2);
    case syscallWrite:
        return handleWrite(a0, a1, a2);
    case syscallClose:
        return handleClose(a0);
    case syscallUnlink:
        return handleUnlink(a0);
    default:
        Lib.debug(dbgProcess, "Unknown syscall " + syscall);
        Lib.assertNotReached("Unknown system call!");
    }
    return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param   cause   the user exception that occurred.
     */
    public void handleException(int cause) {
    Processor processor = Machine.processor();
    switch (cause) {
    case Processor.exceptionSyscall:
        int result = handleSyscall(processor.readRegister(Processor.regV0),
                       processor.readRegister(Processor.regA0),
                       processor.readRegister(Processor.regA1),
                       processor.readRegister(Processor.regA2),
                       processor.readRegister(Processor.regA3)
                       );
        processor.writeRegister(Processor.regV0, result);
        processor.advancePC();
        break;                     
                       
    default:
        Lib.debug(dbgProcess, "Unexpected exception: " +
              Processor.exceptionNames[cause]);
        handleExit(EXCEPTION);
        Lib.assertNotReached("Unexpected exception");
    }
    }
    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    private final int EXCEPTION = -10000;
    
    private int initialPC, initialSP;
    private int argc, argv;
    private int pid;
    private UThread uthread;
    private OpenFile[] openfiles;
    private ArrayList<UserProcess> children;
    private boolean stdoutlock = false;
    
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

    private static int processcnt = 0;
    private static int running_cnt = 0;
}
