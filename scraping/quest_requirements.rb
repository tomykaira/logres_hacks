# -*- coding: utf-8 -*-
require 'nokogiri'
require 'open-uri'
require 'pp'

def build_grams(query)
  chars = query.split(//)
  chars.zip(chars[1..-1]).map { |pair| pair[1] ? pair.join : pair[0] + '*' }
end

def match_thing(quest, query)
  grams = build_grams(query)
  haystack = quest[:thing] + '*'
  grams.select { |g| haystack.include?(g) }.count / grams.size.to_f >= 0.5
end

def match_important(quest, query)
  grams = build_grams(query)
  haystacks = quest.values.map { |v| v + '*' }
  haystacks.any? { |haystack| grams.select { |g| haystack.include?(g) }.count / grams.size.to_f >= 0.5 }
end

# 納品
doc = Nokogiri::HTML(open('http://wikiwiki.jp/mmo-logres/?%A5%AF%A5%A8%A5%B9%A5%C8%2F%C7%BC%C9%CA'))

thing_quests = doc.css('div#body table.style_table tr').map do |row|
  cols = row.css('td')
  next if cols.size != 6
  Hash[[:star, :name, :region, :route, :thing, :reward].zip(cols.map { |col| col.inner_html.encode('utf-8') })]
end.compact

doc = Nokogiri::HTML(open('http://wikiwiki.jp/mmo-logres/?%A5%AF%A5%A8%A5%B9%A5%C8%2F%BD%C5%CD%D7'))

important_quests = doc.css('div#body table.style_table').map do |table|
  headers = table.css('tr th').map(&:content)
  next if headers.size <= 1
  table.css('tr').map do |row|
    cols = row.css('td')
    next if cols.size != headers.size
    Hash[headers.zip(cols.map { |col| col.inner_html.encode('utf-8') })]
  end
end.flatten.compact

ARGV.each do |query|
  pp thing_quests.select { |quest| match_thing(quest, query) }
  pp important_quests.select { |quest| match_important(quest, query) }
end
