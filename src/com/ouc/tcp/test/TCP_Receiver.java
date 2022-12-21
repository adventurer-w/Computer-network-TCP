/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Receiver extends TCP_Receiver_ADT {

    private TCP_PACKET ackPack;    //回复的ACK报文段
    int sequence = 1;//用于记录当前待接收的包序号，注意包序号不完全是
    int preSequence = -1; //记录上一个包的序号
    int expSequence = 0;  //记录期望收到的序号

    private ReceiverSlidingWindow slidingWindow = new ReceiverSlidingWindow(client);

    /*构造函数*/
    public TCP_Receiver() {
        super();    //调用超类构造函数
        super.initTCP_Receiver(this);    //初始化TCP接收端
    }

    @Override
    //接收到数据报：检查校验和，设置回复的ACK报文段
    public void rdt_recv(TCP_PACKET recvPack) {
        if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
            int toACKSequence = -1;
            try {
                //得到确认号
                toACKSequence = slidingWindow.receivePacket(recvPack.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }

            if (toACKSequence != -1) {
                tcpH.setTh_ack(toACKSequence * 100 + 1);
                ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
                tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));

                reply(ackPack);// 回复 ACK 报文段
            }
        }
    }

    @Override
    //交付数据（将数据写入文件）；不需要修改
    public void deliver_data() { }

    @Override
    //回复ACK报文段
    public void reply(TCP_PACKET replyPack) {
        //设置错误控制标志
        tcpH.setTh_eflag((byte) 7);
        //发送数据报
        client.send(replyPack);
    }
}
