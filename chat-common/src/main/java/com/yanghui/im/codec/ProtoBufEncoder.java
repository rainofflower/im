package com.yanghui.im.codec;

import com.yanghui.im.bean.msg.ProtoMsg;
import com.yanghui.im.constant.ProtoInstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * Protobuf编码器
 */
@Slf4j
public class ProtoBufEncoder extends MessageToByteEncoder<ProtoMsg.Message> {

    protected void encode(ChannelHandlerContext ctx, ProtoMsg.Message msg, ByteBuf out) throws Exception {
        //消息头-魔数
        out.writeShort(ProtoInstant.MAGIC_CODE);
        //消息头-版本号
        out.writeShort(ProtoInstant.VERSION_CODE);
        //对象序列化为字节数组
        byte[] bytes = msg.toByteArray();

        //省略数据加密...

        int length = bytes.length;
        //消息头-数据长度
        out.writeInt(length);
        //最后写入消息内容
        out.writeBytes(bytes);
    }
}
