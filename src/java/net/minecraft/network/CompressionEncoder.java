package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.util.zip.Deflater;

public class CompressionEncoder extends MessageToByteEncoder<ByteBuf> {
    private final byte[] encodeBuf = new byte[8192];
    private final Deflater deflater;
    private int threshold;

    public CompressionEncoder(int pThreshold) {
        this.threshold = pThreshold;
        this.deflater = new Deflater();
    }

    protected void encode(ChannelHandlerContext pContext, ByteBuf pEncodingByteBuf, ByteBuf pByteBuf) {
        int i = pEncodingByteBuf.readableBytes();
        if (i > 8388608) {
            throw new IllegalArgumentException("Packet too big (is " + i + ", should be less than 8388608)");
        } else {
            if (i < this.threshold) {
                VarInt.write(pByteBuf, 0);
                pByteBuf.writeBytes(pEncodingByteBuf);
            } else {
                byte[] abyte = new byte[i];
                pEncodingByteBuf.readBytes(abyte);
                VarInt.write(pByteBuf, abyte.length);
                this.deflater.setInput(abyte, 0, i);
                this.deflater.finish();

                while (!this.deflater.finished()) {
                    int j = this.deflater.deflate(this.encodeBuf);
                    pByteBuf.writeBytes(this.encodeBuf, 0, j);
                }

                this.deflater.reset();
            }
        }
    }

    public int getThreshold() {
        return this.threshold;
    }

    public void setThreshold(int pThreshold) {
        this.threshold = pThreshold;
    }
}