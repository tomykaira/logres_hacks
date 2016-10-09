# -*- coding: utf-8 -*-
require 'fileutils'

DIRECTORY = 'pcap/07_19_01_06'

$in_packets = Hash[Dir[DIRECTORY + '/is_*'].map do |file|
  [file, File.read(file).split(/ |\n/).select { |x| x.length > 0 }.map { |x| x.to_i(16) }]
end]

def classify(type, message)
  message_bytes = message.dup.force_encoding('ASCII-8BIT').split(//).map(&:ord)
  ever_found = false
  $in_packets.each do |file, packet|
    found = false
    (0..(packet.length - message_bytes.length)).each do |i|
      if packet[i...i + message_bytes.length] == message_bytes
        found = true
        break
      end
    end
    if found
      ever_found = true
      dir = File.join(DIRECTORY, type)
      FileUtils.mkdir(dir) unless File.directory?(dir)
      FileUtils.cp(file, dir)
    end
  end
  ever_found
end

logs = File.readlines(DIRECTORY + '/log').map { |l| l.strip }.select { |l| l.length > 0 }
logs.each do |line|
  next unless line.match(/\[(.*)\](.*)\Z/)

  type, message = $1, $2
  case type
  when 'Bt'
    case message
    when /^(.*)は(.*)を使用/
      type = 'Bt-Use'
      message = $2
    when /^(.*)は(.*)状態になりました/
      type = 'Bt-State'
      message = $2
    end
  when 'Gl'
    person, text = message.split(': ')
    message = text
  when 'Sys'
    case message
    when /^(.*) の効果がきれました。/
      type = 'Sys-state-done'
      message = $1
    end
  end

  ever_found = classify(type, message)
  p [type, ever_found, message]
end

classify('Guild', 'ギルドメンバーが')
