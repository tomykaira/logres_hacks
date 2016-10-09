// g++ -o client -Wall client.cc -lboost_system

#include <iostream>
#include <iomanip>
#include <cstring>
#include <boost/asio.hpp>
#include "message.h"

namespace asio = boost::asio;
using asio::ip::tcp;
static std::unique_ptr<tcp::socket> espresso_sock;

extern "C" {
u_char *putByte(u_char *data, u_char value) {
  *data = value;
  return data + 1;
}

u_char *putInt(u_char *data, uint32_t value) {
  data[0] = value & 0xff;
  data[1] = (value >> 8) & 0xff;
  data[2] = (value >> 16) & 0xff;
  data[3] = (value >> 24) & 0xff;
  return data + 4;
}

u_char *putBytes(u_char *data, const u_char *value, int len) {
  putInt(data, len);
  memcpy(data + 4, value, len);
  return data + 4 + len;
}

u_char *putString(u_char *data, char *string) {
  return putBytes(data, (u_char *)string, strlen(string));
}

const u_char *readLong(const u_char *data, uint64_t *value) {
  *value = (uint64_t)data[0] | ((uint64_t)data[1] << 8) | ((uint64_t)data[2] << 16) | ((uint64_t)data[3] << 24) |
    ((uint64_t)data[4] << 32) | ((uint64_t)data[5] << 40) | ((uint64_t)data[6] << 48) | ((uint64_t)data[7] << 56);
  return data + 8;
}

const u_char *readInt(const u_char *data, uint32_t *value) {
  *value = (uint32_t)data[0] | ((uint32_t)data[1] << 8) | ((uint32_t)data[2] << 16) | ((uint32_t)data[3] << 24);
  return data + 4;
}

int connectToEspresso()
{
  asio::io_service io_service;
  espresso_sock = std::unique_ptr<tcp::socket>(new tcp::socket(io_service));

  boost::system::error_code error;
  espresso_sock->connect(tcp::endpoint(asio::ip::address::from_string("127.0.0.1"), 8282), error);

  if (error) {
    std::cerr << "connect failed : " << error.message() << std::endl;
    return -1;
  }
  return 0;
}

static int messageByteLength(message *msg)
{
  int length = 1 + 4;  // first byte, tag count
  for (int i = 0; i < msg->tag_count; ++i) {
    length += 4 + msg->tag_lengths[i];
  }
  length += 4 + msg->body_length;
  return length;
}

uint64_t writeMessage(message *msg)
{
  boost::system::error_code error;
  std::unique_ptr<u_char[]> write(new u_char[messageByteLength(msg)]);
  u_char *ptr;

  ptr = putByte(write.get(), 0); // write
  ptr = putInt(ptr, msg->tag_count);    // tag count
  for (int i = 0; i < msg->tag_count; ++i) {
    ptr = putBytes(ptr, msg->tags[i], msg->tag_lengths[i]);
  }
  ptr = putBytes(ptr, msg->body, msg->body_length);
  asio::write(*espresso_sock, asio::buffer(write.get(), ptr - write.get()), error);

  if (error) {
    std::cerr << "write failed: " << error.message() << std::endl;
    return -1;
  }

  asio::streambuf receive_buffer;
  asio::read(*espresso_sock, receive_buffer, asio::transfer_at_least(1), error);

  if (error && error != asio::error::eof) {
    std::cerr << "receive failed: " << error.message() << std::endl;
    return -1;
  } else {
    const u_char* data = asio::buffer_cast<const u_char*>(receive_buffer.data());
    uint64_t timestamp = 0;
    if ((int)data[0] != 0 || (int)data[1] != 0) {
      std::cerr << "response error: " << "status: " << (int)data[0] << "type: " << (int)data[1] << std::endl;
    }
    readLong(data + 2, &timestamp);
    return timestamp;
  }
}
}
