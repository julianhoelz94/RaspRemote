package com.julianhoelz.raspremote;

import java.io.IOException;
import java.net.*;

public class UdpSocket implements Runnable{

    private DatagramSocket udpSocket;
    private InetAddress IPAddress;
    private int returnPort;
    private int port;

    private boolean run;

    public UdpSocket(int port, int returnPort) {
        try {
            this.udpSocket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.port = port;
        this.returnPort = returnPort;
    }

    public void receive() {
        run = true;
        try {
            udpSocket.setSoTimeout(10000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while (run) {
            byte[] receiveData = new byte[32];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                udpSocket.receive(receivePacket);
            } catch (IOException e) {
                continue;
            }

            IPAddress = receivePacket.getAddress();
            Main.inform(clean(new String(receivePacket.getData())));
        }
        udpSocket.close();
    }

    public void send(String message) {
        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, returnPort);
        try {
            udpSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String clean(String input) {
        char[] array = input.toCharArray();
        StringBuilder cleanedUp = new StringBuilder();
        for(char a : array) {
            if(Character.isLetter(a) || Character.isDigit(a)) {
                cleanedUp.append(a);
            }
        }
        return cleanedUp.toString();
    }

    public void stop() {
        run = false;
    }

    @Override
    public void run() {
        receive();
    }
}
