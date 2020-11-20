#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main(int argc, char** argv) {
	// index out of bound
	int a[8], b[8], i;
	for (i = 0; i < 8; ++i) a[i] = i;
	for (i = 0; i < 8; ++i) b[i] = i + 8;
	for (i = 0; i < 8; ++i) a[i + 8] = i;
	for (i = 0; i < 8; ++i) assert(b[i] == i);
	for (i = 0; i < 8; ++i) b[i + 1024] = i;
	return 0;
}
