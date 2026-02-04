package com.njst.ai;

import java.io.*;
import java.net.*;

public class tcp {
    public static void main(String[] args) {
        String serverAddress = "192.168.99.41"; // Server's address (use IP address or hostname)
        int port = 12345; // Port number to connect to
        
        try (Socket socket = new Socket(serverAddress, port)) {
            System.out.println("Connected to server at " + serverAddress + ":" + port);

            // Create input and output streams to send and receive messages
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            // Send messages to the server and receive responses
            String message;
            while (true) {
                System.out.print("Enter message: ");
                message = userInput.readLine();
                output.println(message); // Send message to the server

                // Receive and display server's response
                String serverMessage = input.readLine();
                System.out.println(serverMessage);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
