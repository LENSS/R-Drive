package edu.tamu.lenss.mdfs.RSock;

import java.util.UUID;

public class testRsock {

    public static String testrsock(){
        System.out.println("inside test.java file");

        //test
        test();

        return "Rsock test!";
    }


    private static void test(){



        //64049
        String data_64049 = "";
        for(int i=0; i< 64049; i++){
            data_64049 = data_64049 + "Z";
        }

        //send 64049
        RSockConstants.intrfc_test.send(
                UUID.randomUUID().toString().substring(0, 12),
                data_64049.getBytes(),
                data_64049.length(),
                "nothing",
                "nothing",
                "8877417A2CBA0D19636B44702E7DB497B5834559",
                0,
                "default",
                "default",
                "default");

        System.out.println("testRsock sent data of " + data_64049.length());



        //------------------------------------------

        //64050
        String data_64050 = "";
        for(int i=0; i< 64050; i++){
            data_64050 = data_64050 + "Z";
        }


        //send 64050
        RSockConstants.intrfc_test.send(
                UUID.randomUUID().toString().substring(0, 12),
                data_64050.getBytes(),
                data_64050.length(),
                "nothing",
                "nothing",
                "8877417A2CBA0D19636B44702E7DB497B5834559",
                0,
                "default",
                "default",
                "default");

        System.out.println("testRsock sent data of " + data_64050.length());


        //-------------------------------------------------------------------



        //64051
        String data_64051 = "";
        for(int i=0; i< 64051; i++){
            data_64051 = data_64051 + "Z";
        }

        //send 64051
        RSockConstants.intrfc_test.send(
                UUID.randomUUID().toString().substring(0, 12),
                data_64051.getBytes(),
                data_64051.length(),
                "nothing",
                "nothing",
                "8877417A2CBA0D19636B44702E7DB497B5834559",
                0,
                "default",
                "default",
                "default");

        System.out.println("testRsock sent data of " + data_64051.length());


        //-------------------------------------------------------------------


        //12899
        String data_12899 = "";
        for(int i=0; i< 12899; i++){
            data_12899 = data_12899 + "Z";
        }

        //send 12899
        RSockConstants.intrfc_test.send(
                UUID.randomUUID().toString().substring(0, 12),
                data_12899.getBytes(),
                data_12899.length(),
                "nothing",
                "nothing",
                "8877417A2CBA0D19636B44702E7DB497B5834559",
                0,
                "default",
                "default",
                "default");

        System.out.println("testRsock sent data of " + data_12899.length());



        //-------------------------------------------------------------------


        //128100
        String data_128100 = "";
        for(int i=0; i< 128100; i++){
            data_128100 = data_128100 + "Z";
        }


        //send 128100
        RSockConstants.intrfc_test.send(
                UUID.randomUUID().toString().substring(0, 12),
                data_128100.getBytes(),
                data_128100.length(),
                "nothing",
                "nothing",
                "8877417A2CBA0D19636B44702E7DB497B5834559",
                0,
                "default",
                "default",
                "default");


        System.out.println("testRsock sent data of " + data_128100.length());

        //-------------------------------------------------------------------



        //128101
        String data_128101 = "";
        for(int i=0; i< 128101; i++){
            data_128101 = data_128101 + "Z";
        }

        //send 128101
        RSockConstants.intrfc_test.send(
                UUID.randomUUID().toString().substring(0, 12),
                data_128101.getBytes(),
                data_128101.length(),
                "nothing",
                "nothing",
                "8877417A2CBA0D19636B44702E7DB497B5834559",
                0,
                "default",
                "default",
                "default");

        System.out.println("testRsock sent data of " + data_128101.length());


        //-------------------------------------------------------------------

        //1
        String data_1 = "";
        for(int i=0; i< 1; i++){
            data_1 = data_1 + "Z";
        }

        //send 128101
        RSockConstants.intrfc_test.send(
                UUID.randomUUID().toString().substring(0, 12),
                data_1.getBytes(),
                data_1.length(),
                "nothing",
                "nothing",
                "8877417A2CBA0D19636B44702E7DB497B5834559",
                0,
                "default",
                "default",
                "default");

        System.out.println("testRsock sent data of " + data_1.length());
        //-------------------------------------------------------------------

        //0
        String data_0 = "";

        //send 0
        RSockConstants.intrfc_test.send(
                UUID.randomUUID().toString().substring(0, 12),
                data_0.getBytes(),
                data_0.length(),
                "nothing",
                "nothing",
                "8877417A2CBA0D19636B44702E7DB497B5834559",
                0,
                "default",
                "default",
                "default");

        System.out.println("testRsock sent data of " + data_0.length());
    }
}



//-------------------------------------------------------------------
