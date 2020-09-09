package edu.tamu.lenss.MDFS.Handler;

//this class starts GNS and rsock at the beginning when the app starts.

import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;


public class runGNSandRsock {

    public runGNSandRsock(){

        //we need EdgeKeeper, so init EdgeKeeper first
        new EdgeKeeper();

    }



    public void stopAll(){
        //stop edgeKeeper
        EdgeKeeper.stop();
    }

}
