package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.TimerTask;

public class RetransmitTask  extends TimerTask {
    private Client client;
    private TCP_PACKET packet;
    private SenderSlidingWindow window;

    public RetransmitTask(Client client, TCP_PACKET packet, SenderSlidingWindow window) {
        this.client = client;
        this.packet = packet;
        this.window = window;
    }

    @Override
    public void run() {
        this.window.slowStart();
        this.client.send(this.packet);
    }
}
