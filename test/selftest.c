#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

void Tsyscall() {
	const int plen = 1024;
	char* argv[4];
	char longarg[plen + 1];
	int i;
	for (i = 0; i < plen; ++i)
		longarg[i] = 'f';
	longarg[plen] = 0;
	argv[0] = longarg;
	argv[1] = longarg;
	int outcome = exec("echo.coff", 2, argv);
	puts("*** success ***\n");
}

void IndexoutofBound() {
	char* argv[4];
	int outcome = exec("IndexoutofBound.coff", 0, argv);
	assert(outcome != -1);
	int status = 0;
	assert(join(outcome, &status) != -1);
	puts("*** success ***\n");
}

void TMemory() {
	const int round_num = 10, child_num = 4;
	int round;
	for (round = 0; round < 5; ++round) {
		char* argv[4];
		int i, childID[child_num];
		for (i = 0; i < child_num; ++i) {
			childID[i] = exec("sort.coff", 0, argv);
			assert(childID[i] != -1);
		}
		for (i = 0; i < child_num; ++i) {
			int status = 0;
			assert(join(childID[i], &status) != -1);
			assert(status == 0);
		}
	}
	puts("*** success ***\n");
}

void Execwparams() {
	const int child_num = 4;
	char* argv[child_num];
	char empty[2];
	empty[0] = 0;
	int f[child_num], childID[child_num], i, j;
	for (i = 0; i < child_num; ++i) {
		for (j = 0; j < child_num; ++j)
			if (j != i) argv[j] = empty; 
		char s[2];
		s[0] = '0' + (i % 10);
		s[1] = 0;
		argv[i] = s;
		childID[i] = exec("returnparams.coff", child_num, argv);
		assert(childID[i] != -1);
	}
	for (i = 0; i < child_num; ++i) {
		int status = 0;
		assert(join(childID[i], &status) != -1);
		assert(status == (i % 10));
	}
	puts("*** success ***\n");
}

void TLaunch() {
	const int child_num = 64;
	char* argv[child_num];
	int i, count = 0, childID[child_num];
	for (i = 0; i < child_num; ++i) {
		childID[i] = exec("returnparams.coff", 0, argv);
		if (childID[i] != -1) 
			++count;
	}
	for (i = 0; i < child_num; ++i)
		if (childID[i] != -1) {
			int status = 0;
			assert(join(childID[i], &status) != -1);
		}
	puts("*** success ***\n");
}

void THalt() {
	char* argv[4];
	int outcome = exec("halt.coff", 0, argv);
	assert(outcome != -1);
	int status = 0;
	assert(join(outcome, &status) != -1);
	puts("*** success ***\n");
}



void TFiles() {
	const int file_num = 12;
	int files[file_num];
	char names[file_num][8];
	int i;
	for (i = 0; i < file_num; ++i) {
		strcpy(names[i], "file_a0");
		names[i][7] = 0;
		names[i][5] += i;
	}
	for (i = 0; i < file_num; ++i) {
		files[i] = creat(names[i]);
		assert(files[i] != -1);
	}
	for (i = 0; i < file_num; ++i) {
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
		close(files[i]);
	}
	puts("*** success ***\n");
}

void Texit() {
	const int child_num = 3;
	char* argv[child_num];
	char empty[2];
	empty[0] = 0;
	int f[child_num], childID[child_num], i, j;
	for (i = 0; i < child_num; ++i) {
		for (j = 0; j < child_num; ++j)
			if (j != i) argv[j] = empty; 
		char s[2];
		s[0] = '0' + (i % 10);
		s[1] = 0;
		argv[i] = s;
		childID[i] = exec("exitparams.coff", child_num, argv);
		assert(childID[i] != -1);
	}
	for (i = 0; i < child_num; ++i) {
		int status = 0;
		assert(join(childID[i], &status) != -1);
		assert(status == (i % 10));
	}
	puts("*** success ***\n");
}



int main(int argc, char** argv) {
	Texit();
	THalt();
	TMemory();
	Execwparams();
	TLaunch();
	IndexoutofBound();
	Tsyscall();
	return 0;
}
