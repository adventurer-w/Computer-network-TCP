package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.TimerTask;

public class RetransmitTask extends TimerTask {
    public RetransmitTask(Client client, TCP_PACKET[] packets) {
        super();
        this.senderClient = client;
        this.packets = packets;
    }

    @Override
    public void run() {
        for (int i = 0; i < packets.length; i++) {
            if (packets[i] == null) {  // 如果没有包，break
                break;
            } else {  // 递交各个包
                senderClient.send(packets[i]);
            }
        }
    }

    private Client senderClient;  // 客户端
    private TCP_PACKET[] packets;  // 维护窗口内包的数组
}
