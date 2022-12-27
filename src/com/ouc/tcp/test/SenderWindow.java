package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

public class SenderWindow {

    // 接收到ACK
    public void receiveACK(int number) {
        if (number != lastAck) { //是新的序号
            for (int i = lastAck + 1; i <= number; i++) {//累计确认，可以将之前的包移出窗口了
                packets.remove(i);
                if (timer.containsKey(i)) {
                    timer.get(i).cancel();
                    timer.remove(i);
                }
            }
            lastAck = number;
            ackCount = 0;  //第一次收到这个包
            if (cwnd < ssthresh) { //慢启动
                System.out.println("###### 慢启动 ######");
                System.out.println("cwnd: " + cwnd + " ---> " + (cwnd + 1));
                cwnd ++; // 每收到一个ACK，cwnd+1，即每经过RTT翻倍

            }else { //拥塞避免

                System.out.println("###### 拥塞避免 ######");
                System.out.println("cwnd: " + cwnd + " :: " + congestionCount);
                congestionCount ++;
                if (congestionCount >= cwnd) {  // 收到ACK数量超过 cwnd
                    congestionCount -= cwnd;  // 重置计数器
                    System.out.println("cwnd: " + cwnd + " ---> " + (cwnd + 1));
                    cwnd ++;
                }
            }
        }else{ //旧序号
            ackCount++;
            if(ackCount>=3){ //有三次重复的ack
                TCP_PACKET packet = packets.get(number + 1);
                if (packet != null) {
                    System.out.println("###### 快重传 ,num = "+number+" ###### ");
                    client.send(packet);
                    timer.get(number + 1).cancel();
                    timer.put(number + 1, new UDT_Timer());
                    timer.get(number + 1).schedule(new Reno_Timer(client, packet), 1000, 1000);
                }
                System.out.println("ssthresh: " + ssthresh + " ---> " + Math.max(cwnd / 2, 2));
                System.out.println("cwnd: " + cwnd + " ---> " + Math.max(cwnd / 2, 2)+"+3");
                ssthresh = Math.max(cwnd / 2, 2); // ssthresh 不得小于2
                cwnd = ssthresh+3;
            }
        }
    }

    // 加入包到窗口
    public void addPacket(TCP_PACKET packet) {
        int number = (packet.getTcpH().getTh_seq() - 1) / 100;

        timer.put(number, new UDT_Timer());
        timer.get(number).schedule(new Reno_Timer(client, packet), 1000, 1000);
        packets.put(number, packet);
    }

    public SenderWindow(Client client) {
        this.client = client;
    }

    /*判断窗口是否已满*/
    public boolean isFull() {
        return cwnd <= packets.size();
    }

    public Client client;  //客户端
    private volatile int ssthresh = 16;
    public int cwnd = 1;
    private int lastAck = -1; //上一次收到的ACK的包的seq
    // 使用哈希表，存储窗口内的包
    private Hashtable<Integer, TCP_PACKET> packets = new Hashtable<>();
    // 使用哈希表，储存每个包的计时器
    private Hashtable<Integer, UDT_Timer> timer = new Hashtable<>();
    private int ackCount = -1; //重复的Ack数
    private int congestionCount = 0; // 进入拥塞避免状态时收到的ACK数


    class Reno_Timer extends UDT_RetransTask {
        private TCP_PACKET packet;

        public Reno_Timer(Client client, TCP_PACKET packet) {
            super(client, packet);
            this.packet = packet;
        }

        @Override
        public void run() {
            System.out.println("###### 超时重传 ,num = "+(packet.getTcpH().getTh_seq() - 1) / 100+" ###### ");
            System.out.println("cwnd: " + cwnd + " ---> " + 1);
            System.out.println("ssthresh: " + ssthresh + " ---> " + Math.max(cwnd / 2, 2));

            ssthresh = Math.max(cwnd / 2, 2); // ssthresh 不得小于2
            cwnd = 1;
            super.run();

            int number = (packet.getTcpH().getTh_seq() - 1) / 100;
            if(timer.get(number) != null){
                timer.get(number).cancel();
                timer.remove(number);
            }
            timer.put(number, new UDT_Timer());
            timer.get(number).schedule(new Reno_Timer(client, packet), 1000, 1000);
        }
    }

}


