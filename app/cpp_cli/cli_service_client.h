
#include<iostream>
#include<cstdlib>
#include<unistd.h>
#include<stdio.h> 
#include<string.h>
#include<string>
#include<sys/socket.h>
#include<arpa/inet.h>
#include<netdb.h>



using namespace std;

//#define CLI_JAVA_IP		"192.168.0.42"
#define CLI_JAVA_PORT	2223
#define BUFFER_SIZE 1024
using namespace std;



//This class handles the communication with CLI-service through socket communication.
//This can be used for clients running in both Desktop and Android.
class cli_service_client
  {
  private:
	//int sock;
	int port;
	struct sockaddr_in server;
	int conn();

  public:
	string send_receive(string);
	string address;
	cli_service_client(string );

  };
