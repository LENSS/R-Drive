package edu.tamu.lenss.MDFS.HealthStatusUpdate;

import org.json.JSONObject;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;

public class HealthStatusUpdate implements Runnable {

    public HealthStatusUpdate(){}

    @Override
    public void run(){

        while (true) {
            try {
                //make json app
                JSONObject mdfsHealth = new JSONObject();
                mdfsHealth.put("mdfs_status", "Alive");
                mdfsHealth.put("mdfs_network_status", "Good");

                //QueueToSend
                EKClient.putAppStatus("MDFS", mdfsHealth);

                //sleep
                Thread.sleep(2000);

                //get edge status
                System.out.println(EKClient.getEdgeStatus().toString(4));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
