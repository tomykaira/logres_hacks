if ARGV.length == 0
  exit 1
end
DIRECTORY = ARGV[0]
Dir[DIRECTORY + '/in_*'].map do |file|
  puts "Processing #{file}"
  packets = File.read(file).split(/ |\n/).select { |x| x.length > 0 }.map { |x| x.to_i(16) }

  begin
    while packets.length > 0
      length = (packets[4] << 8) + packets[5] + 6

      if packets[7..10] == [0x35, 0x84, 0x31, 0x31]
        h = packets[6]
        length += 1 + ((h - 1) << 7)
      elsif packets[6..9] == [0x35, 0x84, 0x31, 0x31]
      else
        raise "no 35 84 31 31"
      end

      puts length

      id = (packets[2] << 16) + (packets[0] << 8) + packets[1]
      File.open(DIRECTORY + "/is_#{id}", 'w') do |f|
        packets[0...length].each_with_index do |p, i|
          f.print('%02x ' % p)
          f.puts '' if i % 16 == 15
        end
      end

      packets = packets[length..-1]
    end
  rescue => e
    puts e.message
  end
end
