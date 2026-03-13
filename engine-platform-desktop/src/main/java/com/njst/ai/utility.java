package com.njst.ai;
import java.io.*;

public class utility {


    

    public static void main(String[] args) {
        String directoryPath = "/storage/emulated/0/JavaNIDE/test/app/src/main/java/com"; // ← Change this
        String target = "import com.njst.gaming.Natives.*;";                      // ← Change this
        String replacement = "import com.njst.gaming.Natives.*;\nimport com.njst.gaming.Math.Vector3;";                 // ← Change this

        File rootDir = new File(directoryPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.out.println("Invalid directory: " + directoryPath);
            return;
        }

        replaceInDirectory(rootDir, target, replacement);
        System.out.println("Replacement completed.");
    }

    public static void replaceInDirectory(File dir, String target, String replacement) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            if (file.isDirectory()) {
                replaceInDirectory(file, target, replacement); // Recursive call
            } else if (file.getName().endsWith(".java")) {
                try {
                    replaceInFile(file, target, replacement);
                } catch (IOException e) {
                    System.out.println("Error processing file: " + file.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        }
    }

    public static void replaceInFile(File file, String target, String replacement) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder contentBuilder = new StringBuilder();
        String line;

        boolean changed = false;
        while ((line = reader.readLine()) != null) {
            if (line.contains(target)) {
                line = line.replace(target, replacement);
                changed = true;
            }
            contentBuilder.append(line).append("\n");
        }
        reader.close();

        if (changed) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(contentBuilder.toString());
            writer.close();
            System.out.println("Updated: " + file.getAbsolutePath());
        }
    }

}
