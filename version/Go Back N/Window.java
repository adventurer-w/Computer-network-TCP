package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

public class Window {
    public Client client;  //客户端
    public int size = 32; //窗口大小
    public TCP_PACKET[] packets = new TCP_PACKET[size];  // 存储窗口内的包
    public int base = 0;  // 窗口左指针
    public int nextIndex = 0;  // 下一个包的指针

    public Window(Client client) {
        this.client = client;
    }

    /*判断窗口是否已满*/
    public boolean isFull() {
        return size <= nextIndex;
    }

}
