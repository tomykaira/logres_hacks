/* ldev.c
   Martin Casado

   To compile:
   >gcc ldev.c -lpcap

   Looks for an interface, and lists the network ip
   and mask associated with that interface.
*/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pcap.h>  /* GIMME a libpcap plz! */
#include <errno.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <netinet/if_ether.h>
#include <time.h>
#include "message.h"
#define BUFSIZE 102400

// client.cc
int connectToEspresso();
uint64_t writeMessage(message *msg);

struct my_ip {
  u_int8_t	ip_vhl;		/* header length, version */
#define IP_V(ip)	(((ip)->ip_vhl & 0xf0) >> 4)
#define IP_HL(ip)	((ip)->ip_vhl & 0x0f)
  u_int8_t	ip_tos;		/* type of service */
  u_int16_t	ip_len;		/* total length */
  u_int16_t	ip_id;		/* identification */
  u_int16_t	ip_off;		/* fragment offset field */
#define	IP_DF 0x4000			/* dont fragment flag */
#define	IP_MF 0x2000			/* more fragments flag */
#define	IP_OFFMASK 0x1fff		/* mask for fragmenting bits */
  u_int8_t	ip_ttl;		/* time to live */
  u_int8_t	ip_p;		/* protocol */
  u_int16_t	ip_sum;		/* checksum */
  struct	in_addr ip_src,ip_dst;	/* source and dest address */
};

int is_ip_packet(const u_char* packet)
{
  struct ether_header *eh = (struct ether_header *)packet;
  return (ntohs(eh->ether_type) == ETHERTYPE_IP);
}

static int error_file_id = 0;

#define LOG (log_fp == NULL ? stderr : log_fp)
FILE *log_fp = NULL;

static void save_error_packet(int recv, const u_char* packet, const int length)
{
  char filename[255];
  sprintf(filename, "error_%s_%08d", recv ? "in" : "out", error_file_id);
  fprintf(LOG, "*** %s\n", filename);
  FILE* fp = fopen(filename, "w");
  for (int j = 0; j < length; ++j) {
    fprintf(fp, "%02x ", packet[j]);
    if (j % 16 == 15)
      fprintf(fp, "\n");
  }
  fclose(fp);
  error_file_id++;
}

static int is_separator(const u_char* packet)
{
  return (packet[0] == 0x35 && packet[1] == 0x84 && packet[2] == 0x31 && packet[3] == 0x31) ||
    (packet[0] == 0xbd && packet[1] == 0x14 && packet[2] == 0xe1 && packet[3] == 0xce);
}

static void add_tag(message *msg, const u_char *tag, const int length)
{
  int index = msg->tag_count++;
  if (index >= 16)
    return;
  msg->tag_lengths[index] = length;
  msg->tags[index] = tag;
}

u_char in_rest[BUFSIZE];
int in_rest_len = 0;
u_char out_rest[BUFSIZE];
int out_rest_len = 0;

#ifdef DUMP_PACKETS
static int in_packet_id = 0;
static int out_packet_id = 0;
#endif

void process_packet(const int recv, const u_char *body, const int body_len)
{
  u_char *buffer = NULL, *work = NULL;
  int buffer_len = 0, work_len = 0;
#ifdef DUMP_PACKETS
  FILE *dump = NULL;
#endif

  if (recv) {
#ifdef DUMP_PACKETS
    char filename[256];
    sprintf(filename, "b_in_%08d", in_packet_id++);
    dump = fopen(filename, "w");
#endif

    buffer_len = in_rest_len + body_len;
    buffer = calloc(buffer_len, 1);
    memcpy(buffer, in_rest, in_rest_len);
    memcpy(buffer + in_rest_len, body, body_len);
  } else {
#ifdef DUMP_PACKETS
    char filename[256];
    sprintf(filename, "b_out_%08d", out_packet_id++);
    dump = fopen(filename, "w");
#endif

    buffer_len = out_rest_len + body_len;
    buffer = calloc(buffer_len, 1);
    memcpy(buffer, out_rest, out_rest_len);
    memcpy(buffer + out_rest_len, body, body_len);
  }

#ifdef DUMP_PACKETS
  for (int j = 0; j < body_len; ++j) {
    fprintf(dump, "%02x ", body[j]);
    if (j % 16 == 15)
      fprintf(dump, "\n");
  }
  fclose(dump);
#endif

  work = buffer;
  work_len = buffer_len;

  while (work_len > 9) {
    int length = 0, length1, length2, body_offset = 6, length_start;
    while (body_offset < work_len && !is_separator(work + body_offset))
      body_offset++;

    if (body_offset == work_len) {
      fprintf(LOG, "*** separator not found\n");
      break; // tolerate no header, next packet will have
    }
    length_start = body_offset;
    while (length_start >= 0 && work[length_start-1] != 0)
      length_start --;

    // reset work according to length_start - 5
    work += length_start - 5;
    work_len -= length_start - 5;
    body_offset -= length_start - 5;
    length1 = body_offset + 1; // for first -1
    for (int i = 5; i < body_offset; ++i) {
      length1 += (work[i] - 1) << (7*(i - 5));
    }

    if (length1 > work_len)
      break;

    length2 = body_offset;
    do {
      message msg;
      memset(&msg, 0, sizeof(message));
      int part_length = (work[length2 + 7] << 24) | (work[length2 + 6] << 16) | (work[length2 + 5] << 8) | work[length2 + 4];
      const u_char *data = work + length2 + 8;
      char type_string[8];
      strcpy(type_string, recv ? "in" : "out");

      add_tag(&msg, (u_char *)type_string, strlen(type_string));
      add_tag(&msg, work + length2 + 8, 4);
      msg.body_length = part_length;
      msg.body = data;

      writeMessage(&msg);

      length2 += part_length + 8;
    } while (is_separator(work + length2));
    if (length1 == length2) {
      length = length1;
    } else {
      int start_pos = work - buffer;
      fprintf(LOG, "*** Length mismatch %d %d (at %d %d:%d)\n", length1, length2, start_pos, start_pos / 16 + 1, start_pos % 16);
      save_error_packet(recv, buffer, buffer_len);
      goto failure;
    }

    if (length > work_len)
      break;

    work = work + length;
    work_len -= length;
  }

  fprintf(LOG, "Done: work_len: %d\n", work_len);

  if (recv) {
    memcpy(in_rest, work, work_len);
    in_rest_len = work_len;
  } else {
    memcpy(out_rest, work, work_len);
    out_rest_len = work_len;
  }
  goto exit;

 failure:
  if (recv) {
    in_rest_len = 0;
  } else {
    out_rest_len = 0;
  }

 exit:
  free(buffer);
}

void callback(u_char *useless, const struct pcap_pkthdr* hdr, const u_char* packet)
{
  const struct my_ip* ip;
  u_int length = hdr->len;
  u_int hlen, version;

  int len;

  if (!is_ip_packet(packet)) {
    return;
  }

  /* jump pass the ethernet header */
  ip = (struct my_ip*)(packet + sizeof(struct ether_header));
  length -= sizeof(struct ether_header);

  if (length < sizeof(struct my_ip)) {
    fprintf(LOG, "*** Truncated ip %d",length);
    return;
  }

  len     = ntohs(ip->ip_len);
  hlen    = IP_HL(ip); /* header length */
  version = IP_V(ip);/* ip version */

  /* check version */
  if (version != 4) {
    fprintf(LOG, "*** Unknown version %d\n",version);
    return;
  }

  /* check header length */
  if (hlen < 5) {
    fprintf(LOG, "*** bad-hlen %d\n",hlen);
    return;
  }

  /* see if we have as much packet as we should */
  if(length < len)
    fprintf(LOG, "*** truncated IP - %d bytes missing\n",len - length);

  {
    struct tcphdr *tcp = (struct tcphdr *)(packet + sizeof(struct ether_header) + hlen*4);
    int recv = ntohs(tcp->source) == 9101;

    const int header_len = sizeof(struct ether_header) + hlen*4 + tcp->doff*4;
    const int body_len = hdr->len - header_len;
    const u_char *body = (packet + header_len);

    if (body_len == 0)
      return;

    process_packet(recv, body, body_len);
  }
}

int read_packet_file(const char *filename, u_char *packet)
{
  FILE *fp = fopen(filename, "r");
  int l = 0, v;
  while (fscanf(fp, "%x", &v) != EOF) {
    packet[l++] = v & 0xff;
  }
  fclose(fp);
  return l;
}

pcap_t* start_capture()
{
  char *dev;
  char errbuf[PCAP_ERRBUF_SIZE];
  pcap_t* descr;
  struct bpf_program program;
  bpf_u_int32 netp, maskp;
  char command[] = "port 9101";

  /* ask pcap to find a valid device for use to sniff on */
  dev = pcap_lookupdev(errbuf);
  if(dev == NULL) {
    fprintf(stderr, "%s\n",errbuf);
    return NULL;
  }
  printf("DEV: %s\n",dev);

  pcap_lookupnet(dev, &netp, &maskp, errbuf);

  descr = pcap_open_live(dev,BUFSIZ,0,1000,errbuf);

  if(descr == NULL) {
    fprintf(stderr, "pcap_open_live(): %s\n",errbuf);
    return NULL;
  }

  if (pcap_compile(descr, &program, command, 0, netp) == -1) {
    fprintf(stderr, "pcap_compile error\n");
    return NULL;
  }

  if (pcap_setfilter(descr, &program) == -1) {
    fprintf(stderr, "pcap_setfilter error\n");
    return NULL;
  }
  return descr;
}

int main(int argc, char **argv)
{
  if (connectToEspresso() != 0) {
    return 1;
  }
  if (argc == 1) {
    pcap_t* descr = start_capture();
    if (descr == NULL)
      exit(1);
    log_fp = fopen("pcap_log", "a");
    pcap_loop(descr, -1, callback, NULL);
    fclose(log_fp);
  } else {
    int len;
    u_char packet[BUFSIZE];
    for (int i = 1; i < argc; ++i) {
      char *us = strrchr(argv[i], '_');
      int recv = us != NULL && *(us-2) == 'i' && *(us-1) == 'n';
      printf("Replaying %s\n", argv[i]);
      len = read_packet_file(argv[i], packet);
      process_packet(recv, packet, len);
    }
  }
  return 0;
}
