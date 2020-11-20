#include "stdio.h"
#include "stdlib.h"

int main(int argc, char** argv)
{
  int i, ret = 0;

  for (i=0; i<argc; i++)
	  if (strlen(argv[i]) > 0)
		  ret = atoi(argv[i]);

  for (i=0; i<300000; i++);

  return ret;
}
