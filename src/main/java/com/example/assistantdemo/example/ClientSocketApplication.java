package com.example.assistantdemo.example;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

public class ClientSocketApplication {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 10000;

        try {
            System.out.println("Enter message length:");
            Scanner sc = new Scanner(System.in);
            int messageLength = sc.nextInt();

            Socket socket = new Socket();
            SocketAddress address = new InetSocketAddress(host, port);
            socket.connect(address);

            sendFixedLength(socket, messageLength);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendFixedLength(Socket socket, int messageLength) throws IOException {
        int delimiterLength = 256;

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < messageLength; i++)
            stringBuilder.append("A");
        byte[] totalData = stringBuilder.toString().getBytes();

        System.out.println("Sending message");
        try {
            OutputStream os = socket.getOutputStream();

            for (int i = 0; i < messageLength / delimiterLength; i++) {
                byte[] sending = Arrays.copyOfRange(totalData, i * delimiterLength, (i + 1) * delimiterLength);
                System.out.println("sending... " + (i + 1));
                os.write(sending);
                os.flush();
                Thread.sleep(500);
            }

            System.out.println("Receiving message");

            byte[] reply = new byte[messageLength];
            socket.getInputStream().read(reply);
            System.out.println(new String(reply));
        }
        catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        finally {
            socket.close();
        }
    }
}
