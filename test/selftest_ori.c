#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

void test_divisionbyzero() {
	puts("*** testing division by zero ***\n");
	char* argv[4];
	int result = exec("divisionbyzero.coff", 0, argv);
	assert(result != -1);
	int status = 0;
	assert(join(result, &status) != -1);
	printf("[division by zero] return status = %d\n", status);
	puts("*** success ***\n");
}

void test_indexofb() {
	puts("*** testing index out of bound ***\n");
	char* argv[4];
	int result = exec("indexofb.coff", 0, argv);
	assert(result != -1);
	int status = 0;
	assert(join(result, &status) != -1);
	printf("[index out of bound] return status = %d\n", status);
	puts("*** success ***\n");
}

void test_memory() {
	puts("*** testing memory allocation ***\n");
	const int nrRound = 10, nrChild = 4;
	int round;
	for (round = 0; round < 5; ++round) {
		char* argv[4];
		int i, childID[nrChild];
		for (i = 0; i < nrChild; ++i) {
			childID[i] = exec("sort.coff", 0, argv);
			assert(childID[i] != -1);
		}
		for (i = 0; i < nrChild; ++i) {
			int status = 0;
			assert(join(childID[i], &status) != -1);
			assert(status == 0);
		}
	}
	puts("*** success ***\n");
}

void test_execwparams() {
	const int nrChild = 4;
	puts("*** testing exec with parameters ***\n");
	char* argv[nrChild];
	char empty[2];
	empty[0] = 0;
	int f[nrChild], childID[nrChild], i, j;
	for (i = 0; i < nrChild; ++i) {
		for (j = 0; j < nrChild; ++j)
			if (j != i) argv[j] = empty; 
		char s[2];
		s[0] = '0' + (i % 10);
		s[1] = 0;
		argv[i] = s;
		childID[i] = exec("returnparams.coff", nrChild, argv);
		assert(childID[i] != -1);
	}
	for (i = 0; i < nrChild; ++i) {
		int status = 0;
		assert(join(childID[i], &status) != -1);
		assert(status == (i % 10));
	}
	puts("*** success ***\n");
}

void test_launch() {
	const int nrChild = 64;
	puts("*** testing launch ***\n");
	char* argv[nrChild];
	int i, count = 0, childID[nrChild];
	for (i = 0; i < nrChild; ++i) {
		childID[i] = exec("returnparams.coff", 0, argv);
		if (childID[i] != -1) 
			++count;
	}
	for (i = 0; i < nrChild; ++i)
		if (childID[i] != -1) {
			int status = 0;
			assert(join(childID[i], &status) != -1);
		}
	printf("[launch] launched %d processes, %d succeed\n", nrChild, count);
	puts("*** success ***\n");
}

void test_halt() {
	puts("*** testing halt ***\n");
	char* argv[4];
	int result = exec("halt.coff", 0, argv);
	assert(result != -1);
	int status = 0;
	assert(join(result, &status) != -1);
	assert(status == 0);
	puts("*** success ***\n");
}

void test_systemcall() {
	puts("*** testing system call ***\n");
	const int plen = 1024;
	char* argv[4];
	char longarg[plen + 1];
	int i;
	for (i = 0; i < plen; ++i)
		longarg[i] = 'f';
	longarg[plen] = 0;
	argv[0] = longarg;
	argv[1] = longarg;
	int result = exec("echo.coff", 2, argv);
	printf("[system call with long params] return status = %d\n", result);
	puts("*** success ***\n");
}

void test_exit() {
	const int nrChild = 3;
	puts("*** testing exit ***\n");
	char* argv[nrChild];
	char empty[2];
	empty[0] = 0;
	int f[nrChild], childID[nrChild], i, j;
	for (i = 0; i < nrChild; ++i) {
		for (j = 0; j < nrChild; ++j)
			if (j != i) argv[j] = empty; 
		char s[2];
		s[0] = '0' + (i % 10);
		s[1] = 0;
		argv[i] = s;
		childID[i] = exec("exitparams.coff", nrChild, argv);
		assert(childID[i] != -1);
	}
	for (i = 0; i < nrChild; ++i) {
		int status = 0;
		assert(join(childID[i], &status) != -1);
		assert(status == (i % 10));
	}
	puts("*** success ***\n");
}

void test_files() {
	puts("*** testing files ***\n");
	const int nrFiles = 12;
	int files[nrFiles];
	char names[nrFiles][8];
	int i;
	for (i = 0; i < nrFiles; ++i) {
		strcpy(names[i], "file_a0");
		names[i][7] = 0;
		names[i][5] += i;
	}
	for (i = 0; i < nrFiles; ++i) {
		files[i] = creat(names[i]);
		printf("creating file: %s\n", names[i]);
		assert(files[i] != -1);
	}
	for (i = 0; i < nrFiles; ++i) {
		char* argv[4];
		argv[0] = "cp";
		char src[8] = "file_a0";
		char dst[8] = "file_a1";
		src[7] = dst[7] = 0;
		src[5] += i; dst[5] += i;
		argv[1] = src;
		argv[2] = dst;
		int childID = exec("mv.coff", 3, argv);
		assert(childID != -1);
		int status = 0;
		assert(join(childID, &status) == 1);
		assert(status == 0);
		unlink(dst);
		printf("removing file: %s\n", dst);
		close(files[i]);
	}
	puts("*** success ***\n");
}

int main(int argc, char** argv) {
	/* Functionality test.
	 * All the tests can run together.
	 */

	/* files */
	// test_files();
	/* exit */
	test_exit();
	/* non-root user halt */
	test_halt();
	/* memory & garbage collection */
	test_memory();
	/* exec with parameters */
	test_execwparams();
	/* launch too many processes */
	test_launch();
	
	/* Security test
	 * Machine should not crash. This process may be terminated.
	 * Run the following tests one by one.
	 */

	/* division by zero */
	test_divisionbyzero();
	/* index out of bound */
	test_indexofb();
	/* system call with long parameters */
	test_systemcall();
	return 0;
}
