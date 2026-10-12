
package net.mazebound.bot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MinecraftServerStatus {

    private static final String HOST = "mazebound.minekeep.gg";
    private static final int PORT = 25565;
    private static final int TIMEOUT_MS = 4000;
    private static final long CACHE_INTERVAL_MS = 30_000;

    private static volatile Status cachedStatus;
    private static volatile long lastRefreshTime;

    private static final ScheduledExecutorService REFRESHER =
        Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(
                task, "mazebound-status-refresh"
            );
            thread.setDaemon(true);
            return thread;
        });

    private static boolean autoRefreshStarted;

    public record Status(
        boolean online,
        String version,
        int playersOnline,
        int maxPlayers
    ) {}

    private MinecraftServerStatus() {}

    public static synchronized void startAutoRefresh() {
        if (autoRefreshStarted) {
            return;
        }

        autoRefreshStarted = true;

        REFRESHER.scheduleAtFixedRate(() -> {
            synchronized (MinecraftServerStatus.class) {
                cachedStatus = queryServer();
                lastRefreshTime = System.currentTimeMillis();
            }
        }, 0, CACHE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public static Status query() {
        long now = System.currentTimeMillis();
        Status status = cachedStatus;

        if (status != null
                && now - lastRefreshTime < CACHE_INTERVAL_MS) {
            return status;
        }

        synchronized (MinecraftServerStatus.class) {
            now = System.currentTimeMillis();
            status = cachedStatus;

            if (status != null
                    && now - lastRefreshTime < CACHE_INTERVAL_MS) {
                return status;
            }

            status = queryServer();
            cachedStatus = status;
            lastRefreshTime = System.currentTimeMillis();
            return status;
        }
    }

    private static Status queryServer() {
        try (Socket socket = new Socket()) {
            socket.connect(
                new InetSocketAddress(HOST, PORT),
                TIMEOUT_MS
            );
            socket.setSoTimeout(TIMEOUT_MS);

            DataOutputStream out =
                new DataOutputStream(socket.getOutputStream());
            DataInputStream in =
                new DataInputStream(socket.getInputStream());

            ByteArrayOutputStream handshake =
                new ByteArrayOutputStream();
            DataOutputStream packet =
                new DataOutputStream(handshake);

            writeVarInt(packet, 0);
            writeVarInt(packet, 767);
            writeString(packet, HOST);
            packet.writeShort(PORT);
            writeVarInt(packet, 1);

            writePacket(out, handshake.toByteArray());

            ByteArrayOutputStream request =
                new ByteArrayOutputStream();
            writeVarInt(new DataOutputStream(request), 0);
            writePacket(out, request.toByteArray());

            int packetLength = readVarInt(in);
            if (packetLength <= 0 || packetLength > 1_000_000) {
                throw new IOException(
                    "Invalid status response length"
                );
            }

            int packetId = readVarInt(in);
            if (packetId != 0) {
                throw new IOException(
                    "Unexpected status response"
                );
            }

            int jsonLength = readVarInt(in);
            if (jsonLength < 0 || jsonLength > packetLength) {
                throw new IOException("Invalid status JSON length");
            }

            byte[] jsonBytes = new byte[jsonLength];
            in.readFully(jsonBytes);

            JsonObject json = JsonParser.parseString(
                new String(jsonBytes, StandardCharsets.UTF_8)
            ).getAsJsonObject();

            JsonObject versionInfo = json.getAsJsonObject("version");

            String version = versionInfo.has("name")
                    && !versionInfo.get("name").isJsonNull()
                ? versionInfo.get("name").getAsString()
                : "";

            if (version.isBlank()) {
                int protocol = versionInfo.has("protocol")
                        && !versionInfo.get("protocol").isJsonNull()
                    ? versionInfo.get("protocol").getAsInt()
                    : -1;

                version = switch (protocol) {
                    case 767 -> "1.21.1";
                    case 768 -> "1.21.2";
                    case 769 -> "1.21.4";
                    case 770 -> "1.21.5";
                    case 771 -> "1.21.6";
                    case 772 -> "1.21.7 / 1.21.8";
                    case 773 -> "1.21.9 / 1.21.10";
                    case 774 -> "1.21.11";
                    default -> "Unknown (protocol " + protocol + ")";
                };
            }

            JsonObject players = json.getAsJsonObject("players");

            return new Status(
                true,
                version,
                players.get("online").getAsInt(),
                players.get("max").getAsInt()
            );

        } catch (Exception e) {
            System.err.println(
                "Minecraft status check failed: " + e.getMessage()
            );
            return new Status(false, "", 0, 0);
        }
    }

    private static void writePacket(
        DataOutputStream out,
        byte[] data
    ) throws IOException {
        writeVarInt(out, data.length);
        out.write(data);
        out.flush();
    }

    private static void writeString(
        DataOutputStream out,
        String value
    ) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static void writeVarInt(
        DataOutputStream out,
        int value
    ) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(
        DataInputStream in
    ) throws IOException {
        int value = 0;
        int position = 0;

        while (position < 35) {
            int current = in.readUnsignedByte();
            value |= (current & 0x7F) << position;

            if ((current & 0x80) == 0) {
                return value;
            }

            position += 7;
        }

        throw new IOException("VarInt is too large");
    }
}