package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ReceiverSlidingWindow{
    public ReceiverSlidingWindow(Client client) {
        client = client;
    }

    public int receivePacket(TCP_PACKET packet) {
        int currentSequence = (packet.getTcpH().getTh_seq() - 1) / 100;

        if (currentSequence < base) {
            // ACK号在[base - size, base - 1]
            if (base - size <= currentSequence && currentSequence <= base - 1)
                return currentSequence;
        } else if (base <= currentSequence &&
                currentSequence < base + size) {//位置正确
            //加入窗口
            packets[currentSequence - base] = packet;
            if (currentSequence == base) { //收到的分组好刚好为窗口左端
                slid();// 移动窗口并交付数据
            }
            return currentSequence;
        }
        return -1;
    }

    private void slid() {
        int maxIndex = 0; //最大的收到ACK的包
        while (maxIndex + 1 < size && packets[maxIndex + 1] != null) {
            maxIndex++;
        }
        // 将已接收到的分组加入交付队列
        for (int i = 0; i < maxIndex + 1; i++)
            dataQueue.add(packets[i].getTcpS().getData());

        for (int i = 0; maxIndex + 1 + i < size; i++)// 右移窗口（即左移包）
            packets[i] = packets[maxIndex + 1 + i];

        for (int i = size - (maxIndex + 1); i < size; i++)//清空
            packets[i] = null;

        base += maxIndex + 1;// 更新base

        if (dataQueue.size() >= 20 || base == 1000)//交付数据
            deliver_data();
    }

    //交付数据: 将数据写入文件
    public void deliver_data() {
        // 检查 dataQueue，将数据写入文件
        try {
            File file = new File("recvData.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

            while (!dataQueue.isEmpty()) {
                int[] data = dataQueue.poll();

                // 将数据写入文件
                for (int i = 0; i < data.length; i++) {
                    writer.write(data[i] + "\n");
                }

                writer.flush();  // 清空输出缓存
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Client client;
    private int size = 16;
    private int base = 0;
    private TCP_PACKET[] packets = new TCP_PACKET[size];
    Queue<int[]> dataQueue = new LinkedBlockingQueue();
    private int counts = 0;
}
