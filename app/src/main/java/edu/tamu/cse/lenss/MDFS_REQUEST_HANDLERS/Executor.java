package edu.tamu.cse.lenss.MDFS_REQUEST_HANDLERS;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


//A class that only contains thread
public class Executor {

    public static ExecutorService executor = Executors.newSingleThreadExecutor();
}
