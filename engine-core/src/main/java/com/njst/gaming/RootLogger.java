package com.njst.gaming;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class RootLogger {

    
    private final String logFileName;

    public RootLogger( String logFileName) {
       
        this.logFileName = logFileName;
    }

  //   Log data to external storage root directory
    public void logToRootDirectory(String data) {
        System.out.println(data);
         if (isExternalStorageWritable()) {
             File rootDir = new File("/");  //Root of external storage
             File logFile = new File(rootDir, logFileName);

             FileWriter writer = null;
             try {
                 writer = new FileWriter(logFile, true);//  Append mode
                 writer.append(data).append("\n");
             } catch (IOException e) {
                 e.printStackTrace();
             } finally {
                 if (writer != null) {
                     try {
                         writer.close();
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                 }
             }
         } else {
             System.out.println("External storage is not writable.");
         }
    }

    // Check if external storage is writable
    private boolean isExternalStorageWritable() {
        return true;
    }
}
