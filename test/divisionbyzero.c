#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main(int argc, char** argv) {
	// index out of bound
	int x = 0, y = 0;
	int z = x / y;
	printf("%d\n", z);
	x = 1;
	z = x / y;
	printf("%d\n", z);
	return 0;
}
