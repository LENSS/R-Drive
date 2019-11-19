package edu.tamu.lenss.MDFS.Utils;

//simple tuple class in java
public class Pair{

    private String string_1;
    private String string_2;

    private Pair(){ }

    public Pair(String first, String second){this.string_1 = first; this.string_2 = second;}

    public String getString_2() {
        return string_2;
    }

    public void setString_2(String string_2) {
        this.string_2 = string_2;
    }

    public String getString_1() {
        return string_1;
    }

    public void setString_1(String string_1) {
        this.string_1 = string_1;
    }
}