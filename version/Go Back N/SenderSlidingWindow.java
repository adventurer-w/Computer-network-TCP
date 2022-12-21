package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Timer;

public class SenderSlidingWindow extends Window {

    private Timer timer;  // 计时器
    private RetransmitTask task;  // 重传任务

    public SenderSlidingWindow(Client client) {
        super(client);
    }

    // 加入包到窗口
    public void putPacket(TCP_PACKET packet) {
        packets[nextIndex] = packet;  // 在窗口的下一个插入位置，放入包
        if (nextIndex == 0) {  // 如果nextIndex==0，即在窗口左沿，则开启计时器
            timer = new Timer();
            task = new RetransmitTask(client, packets);
            timer.schedule(task, 1000, 1000);
        }
        nextIndex++;  // 更新窗口的下一个插入位置
    }

    // 接收到ACK
    public void receiveACK(int currentSequence) {
        if (base <= currentSequence && currentSequence < base + size)
        {  // 如果该ACK在窗口范围内

            for (int i = 0; currentSequence - base + 1 + i < size; i++)
            {  // 向右移动滑动窗口，相当于将相应数据左移
                packets[i] = packets[currentSequence - base + 1 + i];
                packets[currentSequence - base + 1 + i] = null;
            }

            nextIndex -= currentSequence - base + 1;  // 更新nextIndex
            base = currentSequence + 1;  // 更新base

            timer.cancel();  // 停止计时器

            if (nextIndex != 0) {  // 窗口中仍有包，需要重开计时器
                timer = new Timer();
                task = new RetransmitTask(client, packets);
                timer.schedule(task, 1000, 1000);
            }
        }
    }


}
