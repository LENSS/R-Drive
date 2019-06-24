#include "cli_service_client.h"


/*
compilation:
g++ cli_service_client.h cli_service_client.cpp unit_test.cpp

run:
./a.out
*/

int main(int argc , char *argv[])
{


  string command;
  string reply;

  cli_service_client cli("192.168.0.2");

  while(true){
    cout<<"\n";
    cout<<"command to MDFS: ";
    //cin>>command;
    getline(cin, command);
    reply = cli.send_receive(command);
    cout<<"reply: " + reply;
    cout<<"\n";
  }





    return 0;
}
