CFLAGS=-g -Wall -Werror
LDFLAGS=-lpcap -lboost_system

pcap: client.o main.o
	$(CXX) $(CFLAGS) -o $@ $^ $(LDFLAGS)

client.o: client.cc message.h
	$(CXX) $(CFLAGS) -std=c++11 -c -o $@ $<

main.o: main.c message.h
	$(CC) $(CFLAGS) -std=gnu99 -c -o $@ $<

clean:
	\rm -rf pcap *.o
