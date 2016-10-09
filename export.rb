# -*- coding: utf-8 -*-
require 'socket'
require 'benchmark'

def num_to_bytes(number, length)
  0.upto(length - 1).map do |i|
    (number >> (i * 8)) & 0xff
  end
end

def string_to_bytes(string)
  num_to_bytes(string.length, 4) + string.split(//).map(&:ord)
end

def write_packet(tags, data)
  [0] +                                           # write
    num_to_bytes(tags.length, 4) +                # tag count
    tags.map { |t| string_to_bytes(t) }.flatten + # tags data
    string_to_bytes(data)                         # body
end

def read_packet(timestamp, count, tags)
  [1] + num_to_bytes(timestamp, 8) + num_to_bytes(count, 4) + num_to_bytes(tags.length, 4) +
    tags.map { |t| string_to_bytes(t) }.flatten
end

def packet_to_string(packet)
  packet.map(&:chr).join
end

def read_num(bytes, length)
  [(0...length).inject(0) { |s, i| s + (bytes[i] << (i * 8)) }, bytes.drop(length)]
end

def parse_response(raw)
  bytes = raw.split(//).map(&:ord)
  code, type, *rest = bytes
  count, rest = read_num(rest, 4)
  data = count.times.map do
    key, rest = read_num(rest, 8)
    len, rest = read_num(rest, 4)
    str = rest[0...len]
    rest = rest.drop(len)
    [key, str]
  end
  { code: code, type: type, count: count, data: data }
end

socket = TCPSocket.open('localhost', 8282)

socket.write(packet_to_string(read_packet(0, - 1, ['in'])))
response = ''
loop do
  part = socket.readpartial(1024)
  response << part
  break if part.length < 1024
end
result = parse_response(response)
socket.close

needle = "ライオットバイト"

p result[:data].select { |x| x[1].map(&:chr).join.include?(needle.force_encoding('ascii-8bit')) }


[[1375264094478770, [240, 211, 3, 245, 93, 0, 4, 1, 171, 104, 243, 81, 151, 0, 0, 0, 8, 0, 0, 0, 97, 0, 4, 1, 171, 104, 243, 81, 184, 11, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 21, 0, 0, 0, 227, 131, 142, 227, 131, 188, 227, 131, 147, 227, 130, 185, 227, 131, 143, 227, 131, 131, 227, 131, 136, 21, 0, 0, 0, 227, 129, 174, 227, 131, 188, 227, 129, 179, 227, 129, 153, 227, 129, 175, 227, 129, 163, 227, 129, 168, 45, 1, 1, 2, 0, 0, 0, 0, 1, 0, 0, 0, 110, 156, 0, 0, 63, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 171, 104, 243, 81, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 93, 0, 4, 1, 171, 104, 243, 81, 6, 0, 1, 1, 96, 140, 243, 81, 209, 0, 2, 101, 1, 0, 0, 0, 0, 0, 0, 0, 24, 0, 0, 0, 227, 131, 169, 227, 130, 164, 227, 130, 170, 227, 131, 131, 227, 131, 136, 227, 131, 144, 227, 130, 164, 227, 131, 136, 24, 0, 0, 0, 227, 130, 137, 227, 129, 132, 227, 129, 138, 227, 129, 163, 227, 129, 168, 227, 129, 176, 227, 129, 132, 227, 129, 168, 201, 0, 2, 1, 9, 0, 0, 0, 0, 2, 0, 0, 50, 117, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 96, 140, 243, 81, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 110, 0, 5, 2, 1, 0, 0, 0, 0, 93, 0, 4, 1, 171, 104, 243, 81, 79, 0, 5, 1, 2, 125, 244, 81, 76, 4, 2, 3, 1, 0, 0, 0, 0, 0, 0, 0, 36, 0, 0, 0, 227, 130, 180, 227, 131, 188, 227, 131, 137, 227, 131, 179, 227, 131, 144, 227, 131, 136, 227, 131, 171, 227, 131, 141, 227, 131, 131, 227, 130, 175, 227, 131, 172, 227, 130, 185, 36, 0, 0, 0, 227, 129, 148, 227, 131, 188, 227, 129, 169, 227, 130, 147, 227, 129, 176, 227, 129, 168, 227, 130, 139, 227, 129, 173, 227, 129, 163, 227, 129, 143, 227, 130, 140, 227, 129, 153, 201, 0, 100, 3, 11, 0, 0, 0, 0, 8, 0, 0, 87, 195, 0, 0, 63, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 5, 0, 0, 0, 2, 125, 244, 81, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 93, 0, 4, 1, 171, 104, 243, 81, 85, 0, 2, 1, 68, 126, 244, 81, 136, 19, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 27, 0, 0, 0, 227, 131, 149, 227, 130, 161, 227, 130, 164, 227, 130, 191, 227, 131, 188, 227, 130, 176, 227, 131, 170, 227, 131, 188, 227, 131, 150, 27, 0, 0, 0, 227, 129, 181, 227, 129, 129, 227, 129, 132, 227, 129, 159, 227, 131, 188, 227, 129, 144, 227, 130, 138, 227, 131, 188, 227, 129, 182, 245, 1, 2, 2, 8, 0, 0, 0, 0, 1, 0, 0, 69, 156, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 68, 126, 244, 81, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 93, 0, 4, 1, 171, 104, 243, 81, 66, 0, 4, 1, 39, 121, 243, 81, 76, 4, 1, 3, 1, 0, 0, 0, 0, 0, 0, 0, 33, 0, 0, 0, 227, 130, 180, 227, 131, 188, 227, 131, 137, 227, 131, 179, 227, 130, 162, 227, 130, 191, 227, 131, 131, 227, 130, 175, 227, 131, 170, 227, 131, 179, 227, 130, 176, 33, 0, 0, 0, 227, 129, 148, 227, 131, 188, 227, 129, 169, 227, 130, 147, 227, 129, 130, 227, 129, 159, 227, 129, 163, 227, 129, 143, 227, 130, 138, 227, 130, 147, 227, 129, 144, 101, 0, 100, 3, 10, 0, 0, 0, 0, 4, 0, 0, 86, 195, 0, 0, 63, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 5, 0, 0, 0, 39, 121, 243, 81, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 93, 0, 4, 1, 171, 104, 243, 81, 98, 0, 4, 1, 171, 104, 243, 81, 160, 15, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 24, 0, 0, 0, 227, 131, 142, 227, 131, 188, 227, 131, 147, 227, 130, 185, 227, 131, 144, 227, 131, 179, 227, 130, 176, 227, 131, 171, 24, 0, 0, 0, 227, 129, 174, 227, 131, 188, 227, 129, 179, 227, 129, 153, 227, 129, 176, 227, 130, 147, 227, 129, 144, 227, 130, 139, 145, 1, 1, 2, 4, 0, 0, 0, 16, 0, 0, 0, 112, 156, 0, 0, 63, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 171, 104, 243, 81, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 93, 0, 4, 1, 171, 104, 243, 81, 95, 0, 4, 1, 171, 104, 243, 81, 232, 3, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 21, 0, 0, 0, 227, 131, 142, 227, 131, 188, 227, 131, 147, 227, 130, 185, 227, 131, 153, 227, 130, 185, 227, 131, 136, 21, 0, 0, 0, 227, 129, 174, 227, 131, 188, 227, 129, 179, 227, 129, 153, 227, 129, 185, 227, 129, 153, 227, 129, 168, 101, 0, 1, 2, 2, 0, 0, 0, 4, 0, 0, 0, 111, 156, 0, 0, 63, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 171, 104, 243, 81, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 93, 0, 4, 1, 171, 104, 243, 81, 84, 0, 2, 1, 68, 126, 244, 81, 208, 7, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 30, 0, 0, 0, 227, 131, 149, 227, 130, 161, 227, 130, 164, 227, 130, 191, 227, 131, 188, 227, 131, 149, 227, 130, 169, 227, 131, 188, 227, 131, 171, 227, 131, 137, 30, 0, 0, 0, 227, 129, 181, 227, 129, 129, 227, 129, 132, 227, 129, 159, 227, 131, 188, 227, 129, 181, 227, 129, 137, 227, 131, 188, 227, 130, 139, 227, 129, 169, 201, 0, 2, 2, 5, 0, 0, 0, 32, 0, 0, 0, 68, 156, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 68, 126, 244, 81, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 93, 0, 4, 1, 171, 104, 243, 81]],
  [1375267898300176, [9, 236, 174, 47, 93, 0, 4, 1, 171, 104, 243, 81, 151, 0, 0, 0, 6, 0, 1, 1, 96, 140, 243, 81, 209, 0, 2, 101, 201, 0, 2, 1, 24, 0, 0, 0, 227, 131, 169, 227, 130, 164, 227, 130, 170, 227, 131, 131, 227, 131, 136, 227, 131, 144, 227, 130, 164, 227, 131, 136, 42, 0, 0, 0, 227, 131, 150, 227, 131, 170, 227, 131, 131, 227, 131, 132, 227, 131, 150, 227, 131, 172, 227, 130, 164, 227, 130, 171, 227, 131, 188, 73, 73, 32, 227, 128, 138, 229, 164, 137, 229, 189, 162, 227, 128, 139, 0, 0, 0, 0, 50, 117, 0, 0, 2, 0, 0, 0, 1, 0, 0, 1, 0, 1, 3, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 133, 0, 0, 0, 248, 255, 255, 255, 18, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 221, 255, 255, 255, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 80, 195, 103, 100, 19, 0, 0, 0, 231, 178, 137, 231, 160, 149, 227, 129, 174, 228, 184, 128, 230, 137, 147, 32, 226, 152, 134, 1, 0, 0, 0, 97, 234, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 8, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]]]
