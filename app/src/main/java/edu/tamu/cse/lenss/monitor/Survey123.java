package edu.tamu.cse.lenss.monitor;

import android.os.Environment;

import java.io.File;

import edu.tamu.cse.lenss.android.MainActivity;
import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;

public class Survey123 implements Runnable{

    public static boolean buttonPressed = false;
    public static Thread survey123_thd;


    @Override
    public void run() {

        try {
            while (buttonPressed || !Thread.interrupted()) {

                //remove the previous file
                String rm = "mdfs -rm " + getsqliteFilePathMDFS() + getsqliteFileNameLocal();
                MainActivity.Foo(rm, MainActivity.context, false);

                //put the new file
                String put = "mdfs -put " + getsqliteFilePathWithNameLocal() + " " + getsqliteFilePathMDFS();
                put = put.replace("My Surveys", "My<SuRvEy123>Surveys");  //special case handled in ProcessOneRequest.java class coz survey123 appData directory string has space on it.
                MainActivity.Foo(put, MainActivity.context, false);

                Thread.sleep(Constants.MONITOR_INTERVAL_IN_SECONDS * 1000);
            }

        }catch (Exception e ){
            //e.printStackTrace();
            buttonPressed= false;
        }
    }

    //get survey123 app .sqlite filepath
    public static String getsqliteFilePathLocal(){
        String dir = Environment.getExternalStorageDirectory() + File.separator + "ArcGIS" + File.separator + "My Surveys" + File.separator + "Databases" + File.separator;
        return dir;
    }

    //get survey123 app .sqlite filename
    public static String getsqliteFileNameLocal(){
        String dir = Environment.getExternalStorageDirectory() + File.separator + "ArcGIS" + File.separator + "My Surveys" + File.separator + "Databases" + File.separator;
        File[] files = new File(dir).listFiles();
        for (int i=0;i < files.length; i++){
            if(files[i].getName().contains(".sqlite")){
                return files[i].getName();
            }
        }

        return null;

    }

    //get survey123 app .sqlite filepath+filename
    public static String getsqliteFilePathWithNameLocal(){
        String dir = getsqliteFilePathLocal();

        File[] files = new File(dir).listFiles();

        String filename = null;
        for (int i=0;i < files.length; i++){
            if(files[i].getName().contains(".sqlite")){
                filename = files[i].getName();
            }
        }

        return (dir + filename);
    }


    //get survey123 app .sqlite filepath in R-Drive
    public static String getsqliteFilePathMDFS(){
        return "/Survey123/" + EdgeKeeper.ownName.replace(".distressnet.org","") + File.separator;
    }
}
