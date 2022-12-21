package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Hashtable;
import java.util.Timer;

public class SenderSlidingWindow {

    public SenderSlidingWindow(Client client) { client = client; }

    public boolean isFull() {
        return this.cwnd <= this.packets.size();
    }

    //
    public void putPacket(TCP_PACKET packet) {
        int currentSequence = (packet.getTcpH().getTh_seq() - 1) / 100;
        this.packets.put(currentSequence, packet);
        this.timers.put(currentSequence, new UDT_Timer());
        this.timers.get(currentSequence).schedule(new RetransmitTask(this.client, packet, this), 1000, 1000);
    }

    public void receiveACK(int currentSequence) {
        if (currentSequence == this.lastACKSequence) {
            this.lastACKSequenceCount++;
            if (this.lastACKSequenceCount == 4) {
                TCP_PACKET packet = this.packets.get(currentSequence + 1);
                if (packet != null) {
                    this.client.send(packet);
                    this.timers.get(currentSequence + 1).cancel();
                    this.timers.put(currentSequence + 1, new UDT_Timer());
                    this.timers.get(currentSequence + 1).schedule(new RetransmitTask(this.client, packet, this), 3000, 3000);
                }

                slowStart();
            }
        } else {
            for (int i = this.lastACKSequence + 1; i <= currentSequence; i++) {
                this.packets.remove(i);

                if (this.timers.containsKey(i)) {
                    this.timers.get(i).cancel();
                    this.timers.remove(i);
                }
            }

            this.lastACKSequence = currentSequence;
            this.lastACKSequenceCount = 1;

            if (this.cwnd < this.ssthresh) {
                this.cwnd++;
                System.out.println("########### window expand ############");
            } else {
                this.count++;
                if (this.count >= this.cwnd) {
                    this.count -= this.cwnd;
                    this.cwnd++;
                    System.out.println("########### window expand ############");
                }
            }
        }
    }
    private Client client;
    public int cwnd = 1;
    private volatile int ssthresh = 16;
    private int count = 0;  // 拥塞避免： cwmd = cwmd + 1 / cwnd，每一个对新包的 ACK count++，所以 count == cwmd 时，cwnd = cwnd + 1
    private Hashtable<Integer, TCP_PACKET> packets = new Hashtable<>();
    private Hashtable<Integer, UDT_Timer> timers = new Hashtable<>();
    private int lastACKSequence = -1;
    private int lastACKSequenceCount = 0;
}
