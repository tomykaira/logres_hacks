#!/usr/bin/env ruby
# -*- coding: utf-8 -*-

def as_int(bytes)
  bytes.reverse.inject(0) { |s, b| (s << 8) + b }
end

def as_utf_string(packets, cursor)
  length = as_int(packets[cursor...cursor + 4])
  return if length < 3 || length >= 1024 || cursor + 4 + length > packets.length
  exp = packets[cursor + 4...cursor + 4 + length]
  return if exp.include?(0)
  utf = exp.map(&:chr).join.force_encoding('utf-8')
  if utf.valid_encoding?
    [utf, length]
  end
end

files = if File.directory?(ARGV[0])
          Dir[File.join(ARGV[0], '*')]
        else
          [ARGV[0]]
        end

files.each do |file|
  p file
  packets = File.read(file).split(/ |\n/).select { |x| x.length > 0 }.map { |x| x.to_i(16) }

  cursor = 0
  result = ""

  length = (packets[4] << 8) + packets[5] + 6
  body_offset = 0

  if packets[8..11] == [0x35, 0x84, 0x31, 0x31]
    length += 1 + ((packets[6] - 1) << 7) + ((packets[7] - 1) << 15)
    body_offset = 12
  elsif packets[7..10] == [0x35, 0x84, 0x31, 0x31]
    length += 1 + ((packets[6] - 1) << 7)
    body_offset = 11
  elsif packets[6..9] == [0x35, 0x84, 0x31, 0x31]
    body_offset = 10
  else
    puts "no 35 84 31 31"
    next
  end

  length2 = body_offset + 4 + packets[body_offset]
  if (h = packets[body_offset + 1]) != 0
    length2 += (h << 8)
  end

  puts "mismatch #{length} #{length2}" if length != length2

  while cursor < packets.length
    if packets[cursor...cursor + 16] == [0] * 16
      count = 16
      while packets[cursor + count] == 0
        count += 1
      end
      result << "/*#{count}*/ "
      cursor += count
      next
    elsif resp = as_utf_string(packets, cursor)
      str, len = resp
      result << '[%s](%d) ' % resp
      cursor += 4 + len
      next
    else
      result << '%02x ' % packets[cursor]
    end
    cursor += 1
  end

  puts result
end
