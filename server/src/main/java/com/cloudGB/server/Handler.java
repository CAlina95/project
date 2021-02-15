package com.cloudGB.server;

import com.cloudGB.common.Commands;
import com.cloudGB.common.Database;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.logging.Level;

public class Handler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = ((ByteBuf) msg);

        byte firstByte = buf.readByte();
        if (firstByte == Commands.getCommandLogin()) {
            Log.protocolLogger.log(Level.INFO, "Начать авторизацию");
            authorization(ctx, buf);
        }
        if (firstByte == Commands.getCommandUpload()) {
            Log.protocolLogger.log(Level.INFO, "Начать загружать");
            uploading(ctx, buf);
        }
        if (firstByte == Commands.getCommandDownload()) {
            Log.protocolLogger.log(Level.INFO, "Начать скачивать");
            downloading(ctx, buf);
        }
        if (firstByte == Commands.getCommandDelete()) {
            Log.protocolLogger.log(Level.INFO, "Начать удалять");
            deleting(ctx, buf);
        }
        if (firstByte == Commands.getCommandView()) {
            Log.protocolLogger.log(Level.INFO, "Начать просмотр");
            viewing(ctx, buf);
        }
    }

    private void authorization(ChannelHandlerContext ctx, ByteBuf buf) {
        short loginSize = buf.readShort();
        byte[] loginBytes = new byte[loginSize];
        buf.readBytes(loginBytes);
        String login = new String(loginBytes);
        short passwordSize = buf.readShort();
        byte[] passBytes = new byte[passwordSize];
        buf.readBytes(passBytes);
        String password = new String(passBytes);
        String name;
        try {
            name = Database.getNameByLogAndPass(login, password);
            if (name != null) {
                byte[] nameBytes = name.getBytes();
                byte[] resBytes = new byte[nameBytes.length + 1];
                resBytes[0] = Commands.getCommandLogin();
                System.arraycopy(nameBytes, 0, resBytes, 1, nameBytes.length);
                Log.protocolLogger.log(Level.CONFIG, name + " существует , доступ разрешен");
                ctx.channel().writeAndFlush(resBytes);
            } else {
                Log.protocolLogger.log(Level.CONFIG, "Имя не найдено");
                ctx.channel().writeAndFlush(new byte[]{Commands.getNameNotFound()});
            }
        } catch (SQLException e) {
            Log.protocolLogger.log(Level.WARNING, "Обнаружено SQL exception");
            e.printStackTrace();
        } finally {
            ctx.close();
        }
    }

    private void uploading(ChannelHandlerContext ctx, ByteBuf buf) {
        short nameSize = buf.readShort();
        byte[] nameBytes = new byte[nameSize];
        buf.readBytes(nameBytes);
        String name = new String(nameBytes);
        try {
            if (Database.isLogin(name)) {
                Log.protocolLogger.log(Level.INFO, "Вход подтвержден");
                short fileNameSize = buf.readShort();
                byte[] fileNameBytes = new byte[fileNameSize];
                buf.readBytes(fileNameBytes);
                String fileName = new String(fileNameBytes);
                if (!Files.exists(Paths.get("server_files/" + name))) {
                    Files.createDirectories(Paths.get("server_files/" + name));
                    Log.protocolLogger.log(Level.INFO, "Папка создана");
                }
                long size = buf.readLong();
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream("server_files/" + name + "/" + fileName))) {
                    for (int i = 0; i < size; i++) {
                        out.write(buf.readByte());
                    }
                    Log.protocolLogger.log(Level.INFO, "Загрузка успешна");
                    ctx.channel().writeAndFlush(new byte[]{Commands.getCommandUpload()});
                }
            } else {
                Log.protocolLogger.log(Level.WARNING, "Обнаружен выход из системы");
                ctx.channel().writeAndFlush(new byte[]{Commands.getLogOut()});
            }
        } catch (SQLException | IOException e) {
            Log.protocolLogger.log(Level.WARNING, "Обнаружено исключение");
            e.printStackTrace();
        } finally {
            ctx.close();
        }
    }

    private void downloading(ChannelHandlerContext ctx, ByteBuf buf) {
        short nameSize = buf.readShort();
        byte[] nameBytes = new byte[nameSize];
        buf.readBytes(nameBytes);
        String name = new String(nameBytes);
        try {
            if (Database.isLogin(name)) {
                Log.protocolLogger.log(Level.INFO, "Вход подтвержден");
                short fileNameSize = buf.readShort();
                byte[] fileNameBytes = new byte[fileNameSize];
                buf.readBytes(fileNameBytes);
                String fileName = new String(fileNameBytes);
                try {
                    if (Files.exists(Paths.get("server_files/" + name + "/" + fileName))) {
                        Log.protocolLogger.log(Level.INFO, "Файл найден");
                        byte[] fileBytes = Files.readAllBytes(Paths.get("server_files/" + name + "/" + fileName));
                        byte[] resBytes = new byte[fileBytes.length + 1];
                        resBytes[0] = Commands.getCommandDownload();
                        System.arraycopy(fileBytes, 0, resBytes, 1, fileBytes.length);
                        ctx.channel().writeAndFlush(resBytes);
                        Log.protocolLogger.log(Level.INFO, "Файл отправлен");
                    } else {
                        Log.protocolLogger.log(Level.INFO, "Файл не найден");
                        ctx.channel().writeAndFlush(new byte[]{Commands.getNotFound()});
                    }
                } catch (IOException e) {
                    Log.protocolLogger.log(Level.WARNING, "IOException обнаружено");
                    e.printStackTrace();
                }
            } else {
                Log.protocolLogger.log(Level.WARNING, "Обнаружен выход из системы");
                ctx.channel().writeAndFlush(new byte[]{Commands.getLogOut()});
            }
        } catch (SQLException e) {
            Log.protocolLogger.log(Level.WARNING, "Обнаружено SQL exception");
            e.printStackTrace();
        } finally {
            ctx.close();
        }
    }

    private void deleting(ChannelHandlerContext ctx, ByteBuf buf) {
        short nameSize = buf.readShort();
        byte[] nameBytes = new byte[nameSize];
        buf.readBytes(nameBytes);
        String name = new String(nameBytes);
        try {
            if (Database.isLogin(name)) {
                Log.protocolLogger.log(Level.INFO, "Вход подтвержден");
                short fileNameSize = buf.readShort();
                byte[] fileNameBytes = new byte[fileNameSize];
                buf.readBytes(fileNameBytes);
                String fileName = new String(fileNameBytes);
                if (Files.exists(Paths.get("server_files/" + name + "/" + fileName))) {
                    Files.delete(Paths.get("server_files/" + name + "/" + fileName));
                    Log.protocolLogger.log(Level.INFO, "Файл найден и удален");
                    ctx.channel().writeAndFlush(new byte[]{Commands.getCommandDelete()});
                } else {
                    Log.protocolLogger.log(Level.INFO, "Файл не найден");
                    ctx.channel().writeAndFlush(new byte[]{Commands.getNotFound()});
                }
            } else {
                Log.protocolLogger.log(Level.WARNING, "Обнаружен выход из системы");
                ctx.channel().writeAndFlush(new byte[]{Commands.getLogOut()});
            }
        } catch (SQLException | IOException e) {
            Log.protocolLogger.log(Level.WARNING, "Обнаружено исключение");
            e.printStackTrace();
        } finally {
            ctx.close();
        }
    }

    private void viewing(ChannelHandlerContext ctx, ByteBuf buf) {
        short nameSize = buf.readShort();
        byte[] nameBytes = new byte[nameSize];
        buf.readBytes(nameBytes);
        String name = new String(nameBytes);
        try {
            if (Database.isLogin(name)) {
                Log.protocolLogger.log(Level.INFO, "Вход подтвержден");
                if (Files.exists(Paths.get("server_files/" + name))) {
                    StringBuilder sb = new StringBuilder();
                    Path dir = Paths.get("server_files/" + name);
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                        for (Path file : stream) {
                            sb.append(file.getFileName()).append(";");
                        }
                    } catch (IOException | DirectoryIteratorException e) {
                        e.printStackTrace();
                    }
                    String resultLine = sb.toString().trim();
                    byte[] bytes = resultLine.getBytes();
                    if (bytes.length != 0) {
                        Log.protocolLogger.log(Level.INFO, "Файл найден в папке");
                        byte[] resBytes = new byte[bytes.length + 1];
                        resBytes[0] = Commands.getCommandView();
                        System.arraycopy(bytes, 0, resBytes, 1, bytes.length);
                        ctx.channel().writeAndFlush(resBytes);
                    } else {
                        Log.protocolLogger.log(Level.INFO, "В папке пусто");
                        ctx.channel().writeAndFlush(new byte[]{Commands.getEmpty()});
                    }
                } else {
                    Log.protocolLogger.log(Level.INFO, "Папка не найдена");
                    ctx.channel().writeAndFlush(new byte[]{Commands.getEmpty()});
                }
            } else {
                Log.protocolLogger.log(Level.WARNING, "Обнаружен выход из системы");
                ctx.channel().writeAndFlush(new byte[]{Commands.getLogOut()});
            }
        } catch (SQLException e) {
            Log.protocolLogger.log(Level.WARNING, "Обнаружено SQL exception");
            e.printStackTrace();
        } finally {
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
