package edu.tamu.lenss.mdfs.hadoop;



import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;





public class hdfsClient {

    public static void ONE(){

        //user and hadoop setting
        System.setProperty("HADOOP_USER_NAME", "hadoopuser");
        System.setProperty("hadoop.home.dir", "/");  //root dir in hdfs

        //filename
        String filename = "test.txt";

        //file content
        String fileContent="if you smell what the rock is cooking!!!";

        //hdfs namenode ip and port
        String hdfsuri = "hdfs://192.168.1.100:9820";

        //configuration
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsuri);

        //file system object
        FileSystem fs = null;
        try {fs = FileSystem.get(URI.create(hdfsuri), conf); } catch (IOException  e) { e.printStackTrace(); }

        //create folder and pathin hdfs
        String p ="/user/hdfs/example/hdfs/";
        Path newFolderPath = null;
        if(fs!=null) {
            try {
                newFolderPath = new Path(p);
                if (!fs.exists(newFolderPath)) {
                    fs.mkdirs(newFolderPath);
                } else {
                    System.out.println("hadoop could not create directory.");
                }
            }catch(IOException e){e.printStackTrace();}
        }else{
            System.out.println("hadoop fs object is null.");

        }

        //write file and close
        try {
            Path hdfswritepath = new Path(new String(newFolderPath + "/" + filename));
            FSDataOutputStream outputStream = null;
            try {
                outputStream = fs.create(hdfswritepath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream.writeBytes(fileContent);
            outputStream.close();
        }catch(IOException e){e.printStackTrace();}

    }

    public static void TWO(){

        Configuration conf = new Configuration();
        //hdfs namenode ip and port
        String hdfsuri = "hdfs://192.168.1.100:9820";
        conf.set("fs.defaultFS", hdfsuri);

        FSDataInputStream in = null;
        FSDataOutputStream out = null;
        String p ="/user/hdfs/example/hdfs/";

        try {
            FileSystem fs = FileSystem.get(conf);
            // Input & Output file paths
            Path inFile = new Path(p);
            Path outFile = new Path(p);
            // check if file exists
            if (!fs.exists(inFile)) {
                System.out.println("Input file not found");
                throw new IOException("Input file not found");
            }
            if (fs.exists(outFile)) {
                System.out.println("Output file already exists");
                throw new IOException("Output file already exists");
            }

            in = fs.open(inFile);
            out = fs.create(outFile);
            IOUtils.copyBytes(in, out, 512, false);


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally {
            IOUtils.closeStream(in);
            IOUtils.closeStream(out);
        }


    }
}
