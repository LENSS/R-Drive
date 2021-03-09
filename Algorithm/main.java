import java.io.*;
import java.lang.reflect.Array;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption.*;
import java.nio.*;
import java.sql.SQLOutput;
import java.util.*;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.APPEND;



public class main {

    public static void main(String[] args) throws Exception {
        //Double[] avail_storage = {0.5, 0.5, 0.5, 0.5, 0.5}; double filesize = 1.0;
        Double[] avail_storage = {0.2 , 0.2, 0.2, 0.2, 0.2 , 0.2, 0.2, 0.2, 0.2}; double filesize = 1.0;
        n_k(filesize, Arrays.asList(avail_storage));

    }


    //R-Drive algorithm draft
    public static void n_k(double fs_gb, List<Double> all_storage){

        //get total size of available edge storage capacity
        double avail_strg = 0.0;
        for(int i=0; i< all_storage.size(); i++){avail_strg = avail_strg + all_storage.get(i);}

        if(avail_strg<fs_gb){

            System.out.println("Edge is outOfMemory");

        }else {

            //remove all 0.0 elements
            List<Double> avail_storage = new ArrayList<>();
            for(int i=0; i< all_storage.size(); i++){
                if(all_storage.get(i)!=0.0){
                    avail_storage.add(all_storage.get(i));
                }
            }

            //algorithm starts
            for (int n = avail_storage.size(); n > 0; n--) {
                for (int k = all_storage.size(); k > 0; k--) {
                     if (k > n) {
                        double cr = (double) k / (double) n;
                        double f_str = -0.0;
                        double each = -0.0;
                        String decision = "n/a";
                        //System.out.println("case#1 " + "n:" + n + " k:" + k + " cr:" + cr +" F_str:" + f_str + " each:" + each + " decision:" + decision);

                    } else if (n == k) {

                        double cr = (double) k / (double) n;
                        double f_str = round(fs_gb / (k / n), 2);
                        double each = round(f_str / n, 2);
                        String decision = "";
                        int count = 0;
                        for(int i=0; i< avail_storage.size(); i++){
                            if(avail_storage.get(i)<each){
                                count++;
                            }
                        }
                        if(count>=n){
                            decision = "outOfMemory";
                            System.out.println("case#2 " + "n:" + n + " k:" + k + " F_str:" + f_str + " each:" + each + " decision:" + decision);

                        } else {
                            decision = "least cost_avail_ft";
                            System.out.println("case#2 " + "n:" + n + " k:" + k + " cr:" + cr + " F_str:" + f_str + " each:" + each + " decision:" + decision);
                        }

                    }else {
                        double cr = round((double) k / (double) n, 2);
                        double f_str = round((double) (fs_gb / cr), 2);
                        double each = round(f_str / n, 2);
                        String decision = "";

                        int count = 0;
                        for(int i=0; i < avail_storage.size(); i++){
                            if(avail_storage.get(i)>=each){
                                count++;
                            }
                        }

                        if (count>=n) {
                            decision = "possible_choice";
                            System.out.println("case#4 " + "n:" + n + " k:" + k + " cr:" + cr + " F_str:" + f_str + " each:" + each + " decision:" + decision);  ///
                        }else{
                            decision = "outOfMemory";
                            System.out.println("case#4 " + "n:" + n + " k:" + k + " cr:" + cr + " F_str:" + f_str + " each:" + each + " decision:" + decision);
                        }
                    }
                }

                System.out.println();

            }

            if(avail_strg>=fs_gb){
                System.out.println("Non EC storage available.");
            }
        }

    }
    private static void printArray(List<Double> array) {
        for(int i=0; i< array.size(); i++){
            System.out.print(array.get(i) + "   ");
        }
        System.out.println();
        System.out.println();
    }


    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}

