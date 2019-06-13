#include "gns_service_client.h"


/*
copilation:
g++ gns_service_client.h gns_service_client.cpp unit_test.cpp

run:
./a.out
*/

/**
 * These functions can be used by RSock
 * @param argc
 * @param argv
 * @return
 */

int main(int argc , char *argv[])
{


  string command;
  string reply;

  gns_service_client gns("192.168.0.1");

  while(true){
    cout<<"\n command to MDFS";
    cin>>command;
    reply = gns.send_receive(command);

    cout<<"\n"+reply;
  }





    return 0;
}
