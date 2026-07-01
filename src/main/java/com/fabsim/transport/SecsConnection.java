package com.fabsim.transport;

import com.fabsim.protocol.SecsMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public final class SecsConnection implements AutoCloseable {

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public SecsConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    public synchronized void send(SecsMessage message) throws IOException {
        byte[] frame = message.encode();
        out.writeInt(frame.length);
        out.write(frame);
        out.flush();
    }

    public SecsMessage receive() throws IOException {
        int length = in.readInt();
        byte[] frame = new byte[length];
        in.readFully(frame);
        return SecsMessage.decode(frame);
    }

    public boolean isOpen() {
        return !socket.isClosed() && socket.isConnected();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
