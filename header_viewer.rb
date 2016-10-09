OFFSET = 3
LENGTH = 40

nodes = LENGTH.times.map { Hash.new }
edges = {}

def node_name(i, v)
  '%d_%02x' % [OFFSET + i, v]
end

if ARGV.length == 0
  puts "set dir"
end

Dir[File.join(ARGV[0], '/in_*')].each do |file|
  bytes = File.read(file).split(/ |\n/).select { |x| x.length > 0 }.map { |x| x.to_i(16) }
  if bytes[7..10] == [0x35, 0x84, 0x31, 0x31]
    bytes = bytes[7..-1]
  elsif bytes[6..9] == [0x35, 0x84, 0x31, 0x31]
    bytes = bytes[6..-1]
  else
    raise "unknown"
  end
  bytes[0...LENGTH].each_with_index do |v, i|
    nodes[i][v] ||= 0
    nodes[i][v] += 1

    next_byte = bytes[i + 1]
    next unless next_byte
    name = node_name(i, v)
    next_name = node_name(i + 1, next_byte)
    edges[%Q{"#{name}" -> "#{next_name}"}] ||= 0
    edges[%Q{"#{name}" -> "#{next_name}"}] += 1
  end
end

nodes_string = ''
nodes.each_with_index do |set, i|
  nodes_string << "{\n  rank = same;"
  names = []
  set.each do |byte, count|
    name = node_name(i, byte)
    nodes_string << %Q{"%s" [label="%s (%d)"];\n} % [name, name, count]
    names << name
  end
  # nodes_string << names.sort.map{ |n| '"' + n + '"' }.join(" -> ") + " [color=white];\n"
  nodes_string << "rankdir=LR;\n"
  nodes_string << "}"
end

edges_string = ''
edges.each do |e, count|
  edges_string << ('%s [label=%d]' + "\n") % [e, count]
end

File.open(File.join(ARGV[0], 'header.gv'), 'w') do |f|
  f.puts <<HERE
digraph in {
  node [shape=box];
  #{ nodes_string }

  #{ edges_string }
}
HERE
end

system("dot -Tpng #{File.join(ARGV[0], 'header.gv')} -o #{File.join(ARGV[0], 'header.png')}")
