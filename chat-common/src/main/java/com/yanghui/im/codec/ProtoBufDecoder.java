package com.yanghui.im.codec;

import com.yanghui.im.bean.msg.ProtoMsg;
import com.yanghui.im.constant.ProtoInstant;
import com.yanghui.im.exception.InvalidFrameException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Protobuf解码器
 */
@Slf4j
public class ProtoBufDecoder extends ByteToMessageDecoder {

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(in.readableBytes() < 8){
            //数据包还不够包头长度
            return;
        }
        //标记当前readerIndex
        in.markReaderIndex();
        //读取魔数
        short magic = in.readShort();
        if(magic != ProtoInstant.MAGIC_CODE){
            String error = "客户端没对上交头暗号: " + ctx.channel().remoteAddress();
            throw new InvalidFrameException(error);
        }
        //读取版本号
        short version = in.readShort();
        //读取消息体content长度
        int length = in.readInt();
        if(length < 0){
            //长度小于0，非法数据，关闭连接
            ctx.close();
        }
        if(in.readableBytes() < length){
            //读取到的消息体长度小于传过来的消息长度,重置读取位置
            in.resetReaderIndex();
            return;
        }
        byte[] data;
        if(in.hasArray()){
            //堆缓冲
            ByteBuf slice = in.slice();
            data = slice.array();
        }
        else{
            //直接缓冲
            data = new byte[length];
            in.readBytes(data, 0 ,length);
        }
        //字节反序列化为对象
        ProtoMsg.Message message = ProtoMsg.Message.parseFrom(data);
        if(message != null){
            out.add(message);
        }
    }
}
