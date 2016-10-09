#ifndef MESSAGE_H
#define MESSAGE_H

typedef struct _message {
  int tag_count;

  const unsigned char *tags[16];
  int tag_lengths[16];

  const unsigned char *body;
  int body_length;
} message;

#endif
