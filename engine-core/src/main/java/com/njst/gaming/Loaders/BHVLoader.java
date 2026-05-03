package com.njst.gaming.Loaders;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.njst.gaming.Bone;

public class BHVLoader {
    public static Bone load(String path) throws IOException {
        Bone root = new Bone();
        ArrayList<String> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                data.add(line);
            }
        }
        int last = data.size() - 1;
        for (int i = last; i > -1; i--) {
            if (data.get(i).contains("}")) {
                last = i;
                break;
            }
        }
        System.out.println("last:" + last + " other" + data.size());
        return root;
    }

    public static Bone loadChild(ArrayList<String> reader, int i) throws IOException {
        System.out.println("loading started");

        Bone child = new Bone();

        return child;
    }

    public static void print_bone(Bone b, int tabs) {
        String tab = "    ";
        System.out.println(tab.repeat(tabs) + b.name);
        for (Bone child : b.Children) {
            print_bone(child, tabs + 1);
        }
    }

    public static void main(String[] args) {
        try {
            load("/users/noliw/OneDrive/Documents/untitled.bvh");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
