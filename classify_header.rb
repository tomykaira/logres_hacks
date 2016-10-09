# -*- coding: utf-8 -*-
require 'fileutils'

DIRECTORY = ARGV[0]

def type_name(type)
  type.map { |b| '%02x' % b }.join('_')
end

Dir[DIRECTORY + '/in_*'].each do |file|
  packet = File.read(file).split(/ |\n/).select { |x| x.length > 0 }.map { |x| x.to_i(16) }
  sep = 6
  until packet[sep..sep + 3] == [0x35, 0x84, 0x31, 0x31]
    sep += 1
  end
  length = packet[sep + 4] + (packet[sep + 5] << 8) + (packet[sep + 6] << 16) + (packet[sep + 7] << 24)
  type = packet[sep + 8..sep + 11]

  dir = File.join(DIRECTORY, type_name(type))
  FileUtils.mkdir(dir) unless File.directory?(dir)
  FileUtils.cp(file, dir)
end
