/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class TCP_Receiver extends TCP_Receiver_ADT {

    private TCP_PACKET ackPack;	//回复的ACK报文段
    private int nextNumber = 0;  // 用于记录期望收到的seq
    private Hashtable<Integer, TCP_PACKET> packets = new Hashtable<>(); // 用于缓存失序分组


    /*构造函数*/
    public TCP_Receiver() {
        super();    //调用超类构造函数
        super.initTCP_Receiver(this);    //初始化TCP接收端
    }

    @Override
    //接收到数据报：检查校验和，设置回复的ACK报文段
    public void rdt_recv(TCP_PACKET recvPack) {
        if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
            int nowNumber = (recvPack.getTcpH().getTh_seq() - 1) / 100;
            if (nextNumber == nowNumber) {
                // 将该包数据插入交付队列
                dataQueue.add(recvPack.getTcpS().getData());
                nextNumber ++;

                // 将窗口中nowNumber之后连续的序号的包的数据加入交付队列
                while(packets.containsKey(nextNumber)){
                    dataQueue.add(packets.get(nextNumber).getTcpS().getData());
                    packets.remove(nextNumber); //从接收方窗口中移出
                    nextNumber++;
                }
                //每20组数据交付一次
                if(dataQueue.size() >= 20|| nextNumber >= 999 )
                    deliver_data();
            } else {  // 收到了失序的包
                if (!packets.containsKey(nowNumber) && nowNumber > nextNumber)
                    packets.put(nowNumber, recvPack);// 需要被加入接收方窗口
            }

            tcpH.setTh_ack((nextNumber - 1) * 100 + 1);//生成ACK报文段
            ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
            tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
            reply(ackPack);
        }
    }

    @Override
    //交付数据（将数据写入文件）；不需要修改
    public void deliver_data() {
        //检查dataQueue，将数据写入文件
        File fw = new File("recvData.txt");
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(fw, true));
            //循环检查data队列中是否有新交付数据
            while(!dataQueue.isEmpty()) {
                int[] data = dataQueue.poll();
                //将数据写入文件
                for(int i = 0; i < data.length; i++) {
                    writer.write(data[i] + "\n");
                }
                writer.flush();		//清空输出缓存
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    //回复ACK报文段
    public void reply(TCP_PACKET replyPack) {
        //设置错误控制标志
        tcpH.setTh_eflag((byte) 7);
        //发送数据报
        client.send(replyPack);
    }
}
