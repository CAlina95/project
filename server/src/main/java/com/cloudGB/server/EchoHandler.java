package com.cloudGB.server;

import com.cloudGB.common.Commands;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.util.logging.Level;

public class EchoHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise){

        byte[] bytes = (byte[]) msg;

        if (bytes[0] == Commands.getCommandLogin()) {
            Log.echoLogger.log(Level.INFO, "Команда принята [Log In]");
        }

        if (bytes[0] == Commands.getEmpty()) {
            Log.echoLogger.log(Level.INFO, "Команда принята[EMPTY]");
            returnBytes(ctx, "Пусто".getBytes());
        }
        if (bytes[0] == Commands.getCommandUpload()) {
            Log.echoLogger.log(Level.INFO, "Команда принята [Upload]");
            returnBytes(ctx, "Успешно загружено".getBytes());
        }
        if (bytes[0] == Commands.getCommandDownload()) {
            Log.echoLogger.log(Level.INFO, "Команда принята [Download]");
            returnBytes(ctx, bytes);
        }
        if (bytes[0] == Commands.getCommandView() || bytes[0] == Commands.getCommandLogin()) {
            if (bytes[0] == Commands.getCommandView()) {
                Log.echoLogger.log(Level.INFO, "Команда принята [View]");
            }

            byte[] answer = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, answer, 0, bytes.length - 1);
            returnBytes(ctx, answer);
        }
        if (bytes[0] == Commands.getCommandDelete()) {
            Log.echoLogger.log(Level.INFO, "Команда принята [Delete]");
            returnBytes(ctx, "Успешно удалено".getBytes());
        }
        if (bytes[0] == Commands.getNotFound()) {
            Log.echoLogger.log(Level.INFO, "Команда принята [Not Found]");
            returnBytes(ctx, "Данный файл отсутствует".getBytes());
        }
        if (bytes[0] == Commands.getNameNotFound()) {
            Log.echoLogger.log(Level.INFO, "Команда принята [Name Not Found]");
            returnBytes(ctx, new byte[] {Commands.getNameNotFound()});
        }
        if (bytes[0] == Commands.getLogOut()) {
            Log.echoLogger.log(Level.INFO, "Команда принята [Log Out]");
            returnBytes(ctx, new byte[]{Commands.getLogOut()});
        }
    }

    private void returnBytes(ChannelHandlerContext ctx, byte[] bytes) {
        ByteBuf buf = ctx.alloc().buffer(bytes.length);
        buf.writeBytes(bytes);
        ctx.writeAndFlush(buf);
        Log.echoLogger.log(Level.INFO, "Данные успешно отправлены");
        ctx.close();
        buf.release();
    }
}
