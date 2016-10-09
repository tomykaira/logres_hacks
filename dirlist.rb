require 'pp'
time_map = {}

`find pcap -type d`.split("\n").each do |line|
  dot, time, code = line.split('/')
  next unless code
  next unless code.match(/[0-9a-z]{2}_[0-9a-z]{2}_[0-9a-z]{2}_[0-9a-z]{2}/)
  time_map[code] ||= []
  time_map[code] << time
end

time_map.each do |code, times|
  if times.sort == ["07_19_19_44", "07_21_12_28"]
    p [code, times]
  end
end
# >> ["d4_16_11_f7", ["07_19_19_44", "07_21_12_28"]]
# >> ["2d_8f_d6_0d", ["07_19_19_44", "07_21_12_28"]]
# >> ["bc_b6_79_06", ["07_19_19_44", "07_21_12_28"]]
