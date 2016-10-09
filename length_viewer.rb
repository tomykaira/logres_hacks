OFFSET = 0
LENGTH = 40

values = LENGTH.times.map { [] }

Dir['pcap/in_*'].each do |file|
  all = File.read(file).split(/ |\n/).select { |x| x.length > 0 }
  bytes = all.map { |x| x.to_i(16) }[OFFSET...OFFSET + LENGTH]

  bytes.each_with_index do |v, i|
    next unless bytes[i + 1]
    values[i] << ((v << 8) + bytes[i + 1]).to_f / all.length.to_f
  end
end

values.each do |row|
  next if row.empty?
  sum = row.inject(0, &:+)
  mean = sum / row.length
  dist = row.inject(0) { |s, v| s + (v - mean) * (v - mean) }
  sd = Math.sqrt(dist)/row.length
  puts "%.5e %.5e %.5e" % [mean, sd, sd/mean]
end
