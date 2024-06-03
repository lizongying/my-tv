#ifndef AES_H
#define AES_H

#include <stddef.h>

void init();
void encode(const char *in, size_t size, char *out);

#endif
