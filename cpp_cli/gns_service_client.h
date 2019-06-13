
#include<iostream>    //cout
#include<cstdlib>
#include<unistd.h>
#include<stdio.h> //printf
#include<string.h>    //strlen
#include<string>  //string
#include<sys/socket.h>    //socket
#include<arpa/inet.h> //inet_addr
#include<netdb.h> //hostent



using namespace std;

//#define GNS_JAVA_IP		"192.168.0.42"
#define GNS_JAVA_PORT	22223
#define BUFFER_SIZE 1024
using namespace std;
/**
 * The GUID cache will contain a map where guid is the Key and value will be list of IPs associated with it.
 */


/**
 * This class handles the communication with GNS-service through socket communication.
 * This can be used for clients running in both Desktop and Android.
 */
class gns_service_client
  {
  private:
	//int sock;
	int port;
	struct sockaddr_in server;

	int conn();

  public:
	   string send_receive(string);

	string address;
	gns_service_client(string );

  };
