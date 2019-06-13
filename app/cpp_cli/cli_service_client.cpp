#include "cli_service_client.h"


cli_service_client::cli_service_client(string server_ip)
{
  port = CLI_JAVA_PORT;
  address = server_ip;
}


  //This function delas with the socket communication.
  int cli_service_client::conn()
  {
      //Create socket
      int sock = socket(AF_INET , SOCK_STREAM , 0);
      if (sock == -1)
      {
          perror("Could not create socket \n");
          return -1;
      }

      //setup address structure
      server.sin_addr.s_addr = inet_addr( address.c_str() );
      server.sin_family = AF_INET;
      server.sin_port = htons( port );

      //Connect to remote server
      if (connect(sock , (struct sockaddr *)&server , sizeof(server)) < 0)
      {
          perror("connect failed. Error \n");
          return -1;
      }

      //cout<<"Connected\n";
      return sock;
  }



  //This function sends some request and waits for reply 
  string cli_service_client::send_receive(string msg)
  {

      //char* buffer = (char*)malloc(sizeof(char)*BUFFER_SIZE);
      char buffer[1000];
      string reply;
      string req = msg+"\n\n";

      int sock = conn();
      // try to connect
      if (sock==-1 ){
      	perror("Can not connect to CLI JAVA Client \n");
      	return "could not connect with local CLI service \n";
      }


      //Send the request
      if( send(sock , req.c_str() , strlen( req.c_str() ) , 0) < 0)
      {
          perror ("Send failed :");
          return ("ERROR: send error");
      }
   
      //Receive a reply from the server
      if( recv(sock , buffer , sizeof(buffer) , 0) < 0)
      {
          perror("recv failed");
          return "ERROR: receive error";
      }
      reply = buffer;
      
      //close the socket
      close(sock);

      //discard the last few garbled characters
      return reply.substr(0,reply.find('\n'));
  }
