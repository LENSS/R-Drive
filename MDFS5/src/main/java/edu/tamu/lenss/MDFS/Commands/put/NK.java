package edu.tamu.lenss.MDFS.Commands.put;

import android.content.Context;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.util.*;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.tamu.lenss.MDFS.Constants;

public class NK{

    private static ExecutorService foothread = Executors.newSingleThreadExecutor();

    private static Random fRandom = new Random(); //add an int as seed

    private static double getGaussian(double aMean, double aVariance){
        return aMean + fRandom.nextGaussian() * aVariance;
    }

    //global average of all storage of all non-garbage solutions
    public static List<Double> w_a_0_4_storage = new ArrayList<>();
    public static List<Double> w_a_0_5_storage = new ArrayList<>();
    public static List<Double> w_a_0_6_storage = new ArrayList<>();
    public static List<Double> w_a_0_7_storage = new ArrayList<>();
    public static List<Double> w_a_0_8_storage = new ArrayList<>();
    public static List<Double> w_a_0_9_storage = new ArrayList<>();
    public static List<Double> w_a_0_99_storage = new ArrayList<>();
    public static List<Double> w_a_1_0_storage = new ArrayList<>();

    //global average of all battery time of all non-garbage solutions
    public static List<Double> w_a_0_4_battery = new ArrayList<>();
    public static List<Double> w_a_0_5_battery = new ArrayList<>();
    public static List<Double> w_a_0_6_battery = new ArrayList<>();
    public static List<Double> w_a_0_7_battery = new ArrayList<>();
    public static List<Double> w_a_0_8_battery = new ArrayList<>();
    public static List<Double> w_a_0_9_battery = new ArrayList<>();
    public static List<Double> w_a_0_99_battery = new ArrayList<>();
    public static List<Double> w_a_1_0_battery = new ArrayList<>();

    //global average of all cost of all non-garbage solutions
    public static List<Double> w_a_0_4_cost = new ArrayList<>();
    public static List<Double> w_a_0_5_cost = new ArrayList<>();
    public static List<Double> w_a_0_6_cost = new ArrayList<>();
    public static List<Double> w_a_0_7_cost = new ArrayList<>();
    public static List<Double> w_a_0_8_cost = new ArrayList<>();
    public static List<Double> w_a_0_9_cost = new ArrayList<>();
    public static List<Double> w_a_0_99_cost = new ArrayList<>();
    public static List<Double> w_a_1_0_cost = new ArrayList<>();

    //global average of all fStr of all non-garbage solutions
    public static List<Double> w_a_0_4_fStr = new ArrayList<>();
    public static List<Double> w_a_0_5_fStr = new ArrayList<>();
    public static List<Double> w_a_0_6_fStr = new ArrayList<>();
    public static List<Double> w_a_0_7_fStr = new ArrayList<>();
    public static List<Double> w_a_0_8_fStr = new ArrayList<>();
    public static List<Double> w_a_0_9_fStr = new ArrayList<>();
    public static List<Double> w_a_0_99_fStr = new ArrayList<>();
    public static List<Double> w_a_1_0_fStr = new ArrayList<>();

    public static void main(String[] args){

        long t1 = System.currentTimeMillis();

        Double F = 500.0; //in mb
        Double T = 300.0; //in mins
        Double w_a = 1.0; //availability value
        int network_size = 30;
        int exp_iteration = 10000;
        boolean print = true;
        boolean write = false;

        //when solution for an iteration impossible for an iteration
        int garbage_0_40 = 0;
        int garbage_0_50 = 0;
        int garbage_0_60 = 0;
        int garbage_0_70 = 0;
        int garbage_0_80 = 0;
        int garbage_0_90 = 0;
        int garbage_0_99 = 0;
        int garbage_1_0 = 0;

        //resultant CR values for 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 0.99 w_a values
        List<Double> _0_40 = new ArrayList<>();
        List<Double> _0_50 = new ArrayList<>();
        List<Double> _0_60 = new ArrayList<>();
        List<Double> _0_70 = new ArrayList<>();
        List<Double> _0_80 = new ArrayList<>();
        List<Double> _0_90 = new ArrayList<>();
        List<Double> _0_99 = new ArrayList<>();
        List<Double> _1_0 = new ArrayList<>();

        //resultant N,K  values for 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 0.99 w_a values
        List<Pair> _0_40_nk = new ArrayList<>();
        List<Pair> _0_50_nk = new ArrayList<>();
        List<Pair> _0_60_nk = new ArrayList<>();
        List<Pair> _0_70_nk = new ArrayList<>();
        List<Pair> _0_80_nk = new ArrayList<>();
        List<Pair> _0_90_nk = new ArrayList<>();
        List<Pair> _0_99_nk = new ArrayList<>();
        List<Pair> _1_0_nk = new ArrayList<>();


        double availableStorage=0;
        for(int it =0; it< exp_iteration; it++) {
            //generate one randoms sample
            List<Node> all_nodes = new ArrayList<>();
            for (int i = 0; i < network_size; i++) {
                Double storage = round(getGaussian(100.0, 20.0), 2);
                Double battery = round(getGaussian(300.0, 80.0), 2);
                all_nodes.add(new Node(storage, battery, Integer.toString(i), T));
            }

            w_a = 0.4;
            Solution _s_0_40 = simple_n_k_combinations(all_nodes, w_a, T, F);
            if(_s_0_40.CR>0.0){
                _0_40.add(_s_0_40.CR);
                _0_40_nk.add(new Pair(_s_0_40.N, _s_0_40.K));
            }else{
                garbage_0_40++;
            }

            w_a = 0.5;
            Solution _s_0_50 = simple_n_k_combinations(all_nodes, w_a, T, F);
            if(_s_0_50.CR>0.0){
                _0_50.add(_s_0_50.CR);
                _0_50_nk.add(new Pair(_s_0_50.N, _s_0_50.K));
            }else{
                garbage_0_50++;
            }

            w_a = 0.6;
            Solution _s_0_60 = simple_n_k_combinations(all_nodes, w_a, T, F);
            if(_s_0_60.CR>0.0){
                _0_60.add(_s_0_60.CR);
                _0_60_nk.add(new Pair(_s_0_60.N, _s_0_60.K));
            }else{
                garbage_0_60++;
            }


            w_a = 0.70;
            Solution _s_0_70 = simple_n_k_combinations(all_nodes, w_a, T, F);
            if(_s_0_70.CR>0.0){
                _0_70.add(_s_0_70.CR);
                _0_70_nk.add(new Pair(_s_0_70.N, _s_0_70.K));
            }else{
                garbage_0_70++;
            }

            w_a = 0.80;
            Solution _s_0_80 = simple_n_k_combinations(all_nodes, w_a, T, F);
            if(_s_0_80.CR>0.0){
                _0_80.add(_s_0_80.CR);
                _0_80_nk.add(new Pair(_s_0_80.N, _s_0_80.K));
            }else{
                garbage_0_80++;
            }

            w_a = 0.9;
            Solution _s_0_90 = simple_n_k_combinations(all_nodes, w_a, T, F);
            if(_s_0_90.CR>0.0){
                _0_90.add(_s_0_90.CR);
                _0_90_nk.add(new Pair(_s_0_90.N, _s_0_90.K));
            }else{
                garbage_0_90++;
            }

            w_a = 0.99;
            Solution _s_0_99 = simple_n_k_combinations(all_nodes, w_a, T, F);
            if(_s_0_99.CR>0.0){
                _0_99.add(_s_0_99.CR);
                _0_99_nk.add(new Pair(_s_0_99.N, _s_0_99.K));
            }else{
                garbage_0_99++;
            }

            w_a = 1.0;
            Solution _s_1_0 = simple_n_k_combinations(all_nodes, w_a, T, F);
            if(_s_1_0.CR>0.0){
                _1_0.add(_s_1_0.CR);
                _1_0_nk.add(new Pair(_s_1_0.N, _s_1_0.K));
            }else{
                garbage_1_0++;
            }
        }

        //PREPARE SOLUTION STRING
        String w_0_4_res = ("w_a: 0.40\niteration: " + _0_40.size() + "\ngarbage: " + garbage_0_40 + "\nave CR: " + Average(_0_40) + ", SDcr: " + calculateSD(_0_40) + "   \nave N: " + getAveN(_0_40_nk) + ", SDn: " + calculateSDn(_0_40_nk) + "\nave K: " + getAveK(_0_40_nk) + ", SDk: " + calculateSDk(_0_40_nk) + "\nFstr: " + Average(w_a_0_4_fStr) + "\nave S: " + Average(w_a_0_4_storage) + ", SDs: " + calculateSD(w_a_0_4_storage) + "\nave B: " + Average(w_a_0_4_battery) + ", SDb: " + calculateSD(w_a_0_4_battery) + "\nave Cost: " + AverageNoRound(w_a_0_4_cost) + "\n\n" + getNKString(_0_40_nk) + "\n");
        String w_0_5_res = ("w_a: 0.50\niteration: " + _0_50.size() + "\ngarbage: " + garbage_0_50 + "\nave CR: " + Average(_0_50) + ", SDcr: " + calculateSD(_0_50) + "   \nave N: " + getAveN(_0_50_nk) + ", SDn: " + calculateSDn(_0_50_nk) + "\nave K: " + getAveK(_0_50_nk) + ", SDk: " + calculateSDk(_0_50_nk) + "\nFstr: " + Average(w_a_0_5_fStr) + "\nave S: " + Average(w_a_0_5_storage) + ", SDs: " + calculateSD(w_a_0_5_storage) + "\nave B: " + Average(w_a_0_5_battery) + ", SDb: " + calculateSD(w_a_0_5_battery) + "\nave Cost: " + AverageNoRound(w_a_0_5_cost) + "\n\n" + getNKString(_0_50_nk) + "\n");
        String w_0_6_res = ("w_a: 0.60\niteration: " + _0_60.size() + "\ngarbage: " + garbage_0_60 + "\nave CR: " + Average(_0_60) + ", SDcr: " + calculateSD(_0_60) + "   \nave N: " + getAveN(_0_60_nk) + ", SDn: " + calculateSDn(_0_60_nk) + "\nave K: " + getAveK(_0_60_nk) + ", SDk: " + calculateSDk(_0_60_nk) + "\nFstr: " + Average(w_a_0_6_fStr) + "\nave S: " + Average(w_a_0_6_storage) + ", SDs: " + calculateSD(w_a_0_6_storage) + "\nave B: " + Average(w_a_0_6_battery) + ", SDb: " + calculateSD(w_a_0_6_battery) + "\nave Cost: " + AverageNoRound(w_a_0_6_cost) + "\n\n" + getNKString(_0_60_nk) + "\n");
        String w_0_7_res = ("w_a: 0.70\niteration: " + _0_70.size() + "\ngarbage: " + garbage_0_70 + "\nave CR: " + Average(_0_70) + ", SDcr: " + calculateSD(_0_70) + "   \nave N: " + getAveN(_0_70_nk) + ", SDn: " + calculateSDn(_0_70_nk) + "\nave K: " + getAveK(_0_70_nk) + ", SDk: " + calculateSDk(_0_70_nk) + "\nFstr: " + Average(w_a_0_7_fStr) + "\nave S: " + Average(w_a_0_7_storage) + ", SDs: " + calculateSD(w_a_0_7_storage) + "\nave B: " + Average(w_a_0_7_battery) + ", SDb: " + calculateSD(w_a_0_7_battery) + "\nave Cost: " + AverageNoRound(w_a_0_7_cost) + "\n\n" + getNKString(_0_70_nk) + "\n");
        String w_0_8_res = ("w_a: 0.80\niteration: " + _0_80.size() + "\ngarbage: " + garbage_0_80 + "\nave CR: " + Average(_0_80) + ", SDcr: " + calculateSD(_0_80) + "   \nave N: " + getAveN(_0_80_nk) + ", SDn: " + calculateSDn(_0_80_nk) + "\nave K: " + getAveK(_0_80_nk) + ", SDk: " + calculateSDk(_0_80_nk) + "\nFstr: " + Average(w_a_0_8_fStr) + "\nave S: " + Average(w_a_0_8_storage) + ", SDs: " + calculateSD(w_a_0_8_storage) + "\nave B: " + Average(w_a_0_8_battery) + ", SDb: " + calculateSD(w_a_0_8_battery) + "\nave Cost: " + AverageNoRound(w_a_0_8_cost) + "\n\n" + getNKString(_0_80_nk) + "\n");
        String w_0_9_res = ("w_a: 0.90\niteration: " + _0_90.size() + "\ngarbage: " + garbage_0_90 + "\nave CR: " + Average(_0_90) + ", SDcr: " + calculateSD(_0_90) + "   \nave N: " + getAveN(_0_90_nk) + ", SDn: " + calculateSDn(_0_90_nk) + "\nave K: " + getAveK(_0_90_nk) + ", SDk: " + calculateSDk(_0_90_nk) + "\nFstr: " + Average(w_a_0_9_fStr) + "\nave S: " + Average(w_a_0_9_storage) + ", SDs: " + calculateSD(w_a_0_9_storage) + "\nave B: " + Average(w_a_0_9_battery) + ", SDb: " + calculateSD(w_a_0_9_battery) + "\nave Cost: " + AverageNoRound(w_a_0_9_cost) + "\n\n" + getNKString(_0_90_nk) + "\n");
        String w_0_99_res = ("w_a: 0.99\niteration: " + _0_99.size() + "\ngarbage: " + garbage_0_99 + "\nave CR: " + Average(_0_99) + ", SDcr: " + calculateSD(_0_99) + "   \nave N: " + getAveN(_0_99_nk) + ", SDn: " + calculateSDn(_0_99_nk) + "\nave K: " + getAveK(_0_99_nk) + ", SDk: " + calculateSDk(_0_99_nk) + "\nFstr: " + Average(w_a_0_99_fStr) + "\nave S: " + Average(w_a_0_99_storage) + ", SDs: " + calculateSD(w_a_0_99_storage) + "\nave B: " + Average(w_a_0_99_battery) + ", SDb: " + calculateSD(w_a_0_99_battery) + "\nave Cost: " + AverageNoRound(w_a_0_99_cost) + "\n\n" + getNKString(_0_99_nk) + "\n");
        String w_1_0_res = ("w_a: 1.0\niteration: " + _1_0.size() + "\ngarbage: " + garbage_1_0 + "\nave CR: " + Average(_1_0) + ", SDcr: " + calculateSD(_1_0) + "   \nave N: " + getAveN(_1_0_nk) + ", SDn: " + calculateSDn(_1_0_nk) + "\nave K: " + getAveK(_1_0_nk) + ", SDk: " + calculateSDk(_1_0_nk) + "\nFstr: " + Average(w_a_1_0_fStr) + "\nave S: " + Average(w_a_1_0_storage) + ", SDs: " + calculateSD(w_a_1_0_storage) + "\nave B: " + Average(w_a_1_0_battery) + ", SDb: " + calculateSD(w_a_1_0_battery) + "\nave Cost: " + AverageNoRound(w_a_1_0_cost) + "\n\n" + getNKString(_1_0_nk) + "\n");

        //PRINT SOLUTION
        if(print){
            System.out.println(w_0_4_res);
            System.out.println(w_0_5_res);
            System.out.println(w_0_6_res);
            System.out.println(w_0_7_res);
            System.out.println(w_0_8_res);
            System.out.println(w_0_9_res);
            System.out.println(w_0_99_res);
            System.out.println(w_1_0_res);
        }

        //WRITE SOLUTION TO A FILE
        if(write) {
            try {
                String directory = "/home/msagor/ADB/";
                String filename = directory + network_size + "_" + exp_iteration;
                File f = new File(filename);
                f.delete();
                f.createNewFile();
                FileWriter fw = new FileWriter(f, true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(w_0_4_res);
                bw.write(w_0_5_res);
                bw.write(w_0_6_res);
                bw.write(w_0_7_res);
                bw.write(w_0_8_res);
                bw.write(w_0_9_res);
                bw.write(w_0_99_res);
                bw.write(w_1_0_res);
                bw.close();
                fw.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        long t2 = System.currentTimeMillis();
        System.out.println("Algorithm Execution Time: " + (t2 - t1));
    }

    public static Solution simple_n_k_combinations(List<Node> all_nodes, Double w_a, Double T, Double F){

        //System.out.println("==========================================================");
        Solution oldSol = new Solution();

        for(int N=1; N<= all_nodes.size(); N++){
            for(int K= 1; K<=N; K++){

                //calculate CR, f_str and f_each for this combination
                Double CR = round(((double)K/(double)N),2);
                Double f_str = round(F/CR,2);
                Double f_each = round(f_str / (double)N, 2);

                //check if at least N devices have f_each storage (eq 2)
                List<Node> approved_nodes = new ArrayList<>();
                for (Node node : all_nodes) {
                    if (node.storage >= f_each) {
                        approved_nodes.add(node);
                    }
                }

                //if less than N then stop the whole show for this n, k
                if (approved_nodes.size() <N) {
                    continue;
                }

                //check if approved_nodes have at least k devices with minimum T battery (eq 3)
                int T_count = 0;
                for (Node node : approved_nodes) {
                    if (node.avail_Battery_time >= T) {
                        T_count++;
                    }
                }

                //if less than K then stop the whole show for this n, k
                if (T_count <K) {
                    continue;
                }

                //up to this point this N, K values have fulfilled eq 2 and 3
                Double C_k_n_prime = (w_a * ((double)K/(double)N)) + ((1.0-w_a) * ((double)N/(double)K)); //eq 1
                //System.out.println("Cost=>   N:" + N + "   K:" + K + "    Cost:" + C_k_n_prime);

                //make this combination a solution obj
                Solution newSol = new Solution(N, K, CR, approved_nodes, F, f_str, w_a, T, C_k_n_prime);

                //if newSol has smaller cost than oldSol
                if(newSol.C_k_n_prime < oldSol.C_k_n_prime){
                    oldSol = newSol;

                }else if (Double.compare(newSol.C_k_n_prime, oldSol.C_k_n_prime)==0){
                    //compare availability A(k, n, p) as eq 6
                    if(newSol.systemAvailabilityScore>oldSol.systemAvailabilityScore){
                        //newSol has more system availability so choose newSol
                        oldSol = newSol;
                    }
                }
            }
        }

        //print solution
        //oldSol.printMe();

        //add the solution storage and battery values in global list
        if(oldSol.CR>0.0){
            if(w_a==0.4){
                w_a_0_4_storage.add(oldSol.getTopNNodesStorageSum());
                w_a_0_4_battery.add(oldSol.getTopNNodesBatterySum());
                w_a_0_4_cost.add(oldSol.C_k_n_prime);
                w_a_0_4_fStr.add(oldSol.getFstr());
            }
            if(w_a==0.5){
                w_a_0_5_storage.add(oldSol.getTopNNodesStorageSum());
                w_a_0_5_battery.add(oldSol.getTopNNodesBatterySum());
                w_a_0_5_cost.add(oldSol.C_k_n_prime);
                w_a_0_5_fStr.add(oldSol.getFstr());
            }

            if(w_a==0.6){
                w_a_0_6_storage.add(oldSol.getTopNNodesStorageSum());
                w_a_0_6_battery.add(oldSol.getTopNNodesBatterySum());
                w_a_0_6_cost.add(oldSol.C_k_n_prime);
                w_a_0_6_fStr.add(oldSol.getFstr());
            }

            if(w_a==0.7){
                w_a_0_7_storage.add(oldSol.getTopNNodesStorageSum());
                w_a_0_7_battery.add(oldSol.getTopNNodesBatterySum());
                w_a_0_7_cost.add(oldSol.C_k_n_prime);
                w_a_0_7_fStr.add(oldSol.getFstr());
            }

            if(w_a==0.8){
                w_a_0_8_storage.add(oldSol.getTopNNodesStorageSum());
                w_a_0_8_battery.add(oldSol.getTopNNodesBatterySum());
                w_a_0_8_cost.add(oldSol.C_k_n_prime);
                w_a_0_8_fStr.add(oldSol.getFstr());
            }

            if(w_a==0.9){
                w_a_0_9_storage.add(oldSol.getTopNNodesStorageSum());
                w_a_0_9_battery.add(oldSol.getTopNNodesBatterySum());
                w_a_0_9_cost.add(oldSol.C_k_n_prime);
                w_a_0_9_fStr.add(oldSol.getFstr());
            }

            if(w_a==0.99){
                w_a_0_99_storage.add(oldSol.getTopNNodesStorageSum());
                w_a_0_99_battery.add(oldSol.getTopNNodesBatterySum());
                w_a_0_99_cost.add(oldSol.C_k_n_prime);
                w_a_0_99_fStr.add(oldSol.getFstr());
            }

            if(w_a==1.0){
                w_a_1_0_storage.add(oldSol.getTopNNodesStorageSum());
                w_a_1_0_battery.add(oldSol.getTopNNodesBatterySum());
                w_a_1_0_cost.add(oldSol.C_k_n_prime);
                w_a_1_0_fStr.add(oldSol.getFstr());
            }
        }

        return oldSol;
    }

    public static Double calculateSDk(List<Pair> NK){
        List<Double> Ks = new ArrayList<>();
        for(int i=0; i< NK.size(); i++){
            Ks.add(new Double(NK.get(i).K));
        }

        return calculateSD(Ks);
    }

    public static Double calculateSDn(List<Pair> NK){
        List<Double> Ns = new ArrayList<>();
        for(int i=0; i< NK.size(); i++){
            Ns.add(new Double(NK.get(i).N));
        }

        return calculateSD(Ns);
    }

    public static Double getAveK(List<Pair> NK){
        Double total = 0.0;
        for(Pair p: NK){
            total+=p.K;
        }
        return round((total/new Double(NK.size())),2);
    }

    public static Double getAveN(List<Pair> NK){
        Double total = 0.0;
        for(Pair p: NK){
            total+=p.N;
        }
        return round((total/new Double(NK.size())),2);
    }

    public static String getNKString(List<Pair> NK){
        String res = "Resultant (N,K): ";
        for(Pair p: NK){
            res+="(" + p.N + ", " + p.K + ")";
        }

        return ""; //res;
    }
    public static String listToStr(List<Double> values){
        String res = "";
        for(Double v: values){
            res+= v + ", ";
        }
        return res;
    }


    public static double calculateSD(List<Double> numArray) {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.size();

        for(double num : numArray) {
            sum += num;
        }

        double mean = sum/length;

        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return round(Math.sqrt(standardDeviation/length), 2);
    }

    public static Double Average(List<Double> numbers){
        Double total = 0.0;
        for(int i=0; i< numbers.size(); i++){
            total+=numbers.get(i);
        }
        return round((total/numbers.size()), 2);
    }

    public static Double AverageNoRound(List<Double> numbers){
        Double total = 0.0;
        for(int i=0; i< numbers.size(); i++){
            total+=numbers.get(i);
        }
        return total/numbers.size();
    }

    //takes a Solution object and computes the whole system availability for that solution
    public static Double computeSystemAvailabilityScore(Solution s){

        //make an average of all device availability
        Double p = 0.0;
        for(int i=0;i<s.N;i++){ p += s.Nodes.get(i).p; }
        p = p/(double)s.N;

        Double sysAvailabilityScore = 0.0;

        try {
            for (int k = s.K; k <= s.N; k++) {
                Double comb = factorial(s.N) / (factorial(k) * factorial(s.N - k)); //eq 6
                Double avail = comb * (Math.pow(p, k)) * (Math.pow((1 - p), (s.N - k)));
                sysAvailabilityScore += avail;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sysAvailabilityScore;
    }

    //a class that represents a possible solution
    static class Solution implements Comparable<Solution>{
        public int N;
        public int K;
        public Double CR;
        List<Node> Nodes;
        Double F;
        Double Fstr;
        Double w_a;
        Double T;
        Double systemAvailabilityScore;
        Double C_k_n_prime;

        //dummy constructor, populates fields with garbage values
        public Solution(){
            this.N=-22;
            this.K=-7;
            this.CR=-5.0;
            this.Nodes=new ArrayList<>();
            this.F=-1.0;
            this.Fstr = -1.0;
            this.w_a = -1.0;
            this.T=-1.0;
            this.systemAvailabilityScore = -100.0;
            this.C_k_n_prime=100.0;
        }

        public Solution(int N, int K, Double CR, List<Node> Nodes, Double F, Double F_str, Double w_a, Double T, Double C_k_n_prime){
            this.N=N;
            this.K=K;
            this.CR=CR;
            this.Nodes= Nodes;
            this.F=F;
            this.Fstr = F_str;
            this.w_a=w_a;
            this.T=T;
            this.C_k_n_prime = C_k_n_prime;

            //sort the nodes by available battery
            Collections.sort(Nodes, new Comparator<Node>() {
                @Override
                public int compare(Node n1, Node n2) {
                    return n1.avail_Battery_time < n2.avail_Battery_time ? 1 : n1.avail_Battery_time > n2.avail_Battery_time ? -1 : 0;
                }
            });

            //compute systemAvailabilityScore for this solution for top N nodes
            this.systemAvailabilityScore = computeSystemAvailabilityScore(this);
        }

        public int compareTo(Solution cr) {
            return this.CR > cr.CR ? 1 : this.CR < cr.CR ? -1 : 0;
        }

        public Double getTopNNodesStorageSum(){
            Double storageSum = 0.0;
            for(int i=0; i< this.N; i++){
                storageSum+= Nodes.get(i).storage;
            }
            return storageSum/this.N;
        }

        public Double getTopNNodesBatterySum(){
            Double batterySum = 0.0;
            for(int i=0; i< this.N; i++){
                batterySum+= Nodes.get(i).avail_Battery_time;
            }
            return batterySum/this.N;
        }

        public Double getFstr(){
            return this.Fstr;
        }

        //prints this solution
        public void printMe(){
            System.out.println("F: "+F);
            System.out.println("T: "+T);
            System.out.println("w_a: "+w_a);
            Double F_str = round(F/CR, 2);
            System.out.println("F_str: "+ F_str);
            System.out.println("F_each: " +round(F_str / (double)N, 2));
            System.out.println("Cost: "+ C_k_n_prime);
            System.out.println("systemAvailabilityScore: " + systemAvailabilityScore);
            System.out.println("CR: "+CR);
            System.out.println("N: "+N);
            System.out.println("K: "+K);

            String nodes = "";
            int chooseCount=0;
            for(Node node: Nodes){
                nodes = nodes + node.guid + "(" + node.storage + "," + node.avail_Battery_time + ")   ";
                chooseCount++;
                if(chooseCount==N){ break; }
            }
            System.out.println("chosen "+ N +" nodes: " + nodes);
            System.out.println("chosen "+ N +" nodes size: " + Nodes.size());

            System.out.println();
            System.out.println();
        }

        public List<String> getTopNNodesGUIDs(){
            List<String> topNnodes = new ArrayList<>();
            for(int i=0; i< this.N; i++){
                topNnodes.add(Nodes.get(i).guid);
            }
            return topNnodes;
        }
    }


    static class Node{
        public Double storage;
        public Double avail_Battery_time;
        public String guid;
        public Double p; //device availability from eq 6, 7

        public Node(Double storage, Double avail_Battery, String guid, Double T){
            this.storage = storage;
            this.avail_Battery_time = avail_Battery;
            this.guid = guid;

            //compute device availability
            if(this.avail_Battery_time>T){
                this.p = 1.0;
            }else{
                this.p = avail_Battery_time/T;
            }
        }

        public static void printNodes(List<Node> nodes){
            String res = "";
            for(int i=0; i<nodes.size();i++){
                res+= nodes.get(i).storage + "   " + nodes.get(i).avail_Battery_time + "\n";
            }
            System.out.println(res);
        }
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    static Double factorial(int n) {
        Double fact = 1.0;
        int i = 1;
        while(i <= n) {
            fact *= new Double(i);
            i++;
        }
        return fact;
    }

    public static BigInteger factorialBG(int number) {
        BigInteger factorial = BigInteger.ONE;

        for (int i = number; i > 0; i--) {
            factorial = factorial.multiply(BigInteger.valueOf(i));
        }

        return factorial;
    }
    static class Pair{
        public int N;
        public int K;

        public Pair(int n, int k){
            this.N = n;
            this.K = k;
        }
    }
}