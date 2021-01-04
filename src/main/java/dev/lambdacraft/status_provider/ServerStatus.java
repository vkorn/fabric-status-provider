package dev.lambdacraft.status_provider;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerStatus extends NanoHTTPD {

    public static final String MIME_JSON = "application/json";
    public static final String KEY_HEADER = "x-fabric-server-status";

    private static final String CarpetMPPatchName = "carpet.patches.EntityPlayerMPFake";
    private static final String LambdaBotNamePrefix = "[BOT] ";

    private final Lock queueLock = new ReentrantLock();
    private final FixedSizeQueue<String> messages = new FixedSizeQueue<>(20);
    private Style discordMessageStyle;
    private final Type msgType = new TypeToken<List<String>>() {}.getType();

    class PlayerStatus {
        public String Name;
        public String UUID;
        public double X;
        public double Y;
        public double Z;
        public float Health;
        public String Dimension;
        public boolean IsBot;
    }

    class ServerStatusResponse {
        public int Online;
        public long StartTime;
        public List<PlayerStatus> Players;
    }

    private MinecraftServer server;

    public ServerStatus(MinecraftServer server, int port) throws IOException {
        super(port);
        this.server = server;
        start();
        StatusMain.LOG.info("[Status Provider] Running on port " + port);
        discordMessageStyle = new Style();
        discordMessageStyle.setColor(Formatting.GOLD);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String key = session.getHeaders().get(KEY_HEADER);
        String secret = StatusMain.props.getProperty("secret", "");
        if (!secret.equals("")) {
            Boolean secretCheck = key != null && key.equals(secret);
            if (!secretCheck) {
                return newFixedLengthResponse("{\"error\": \"Incorrect secret. Secret should be specified in 'x-fabric-server-status' HTTP header \"}");
            }
        }

        switch (session.getMethod()) {
            case GET: {
                return getPlayersList();
            }
            case POST: {
                try {
                    int contentLength = Integer.parseInt(session.getHeaders().get("content-length"));
                    if (0 != contentLength) {
                        byte[] buffer = new byte[contentLength];
                        session.getInputStream().read(buffer, 0, contentLength);
                        ArrayList<String> msgs = new Gson().fromJson(new String(buffer), msgType);

                        for (String m : msgs) {
                            Text text = new LiteralText(m);
                            text.setStyle(discordMessageStyle);

                            server.getPlayerManager().sendToAll(text);
                        }

                    }
                } catch (Exception ignored) {
                }
                return getMessages();
            }
            default: {
                return newFixedLengthResponse("{\"error\": \"Unsupported method \"}");
            }
        }
    }

    private Response getPlayersList() {
        List<PlayerStatus> players = new ArrayList<>();

        for (ServerPlayerEntity p :
                server.getPlayerManager().getPlayerList()) {
            PlayerStatus pl = new PlayerStatus();
            pl.Name = p.getName().getString();
            pl.UUID = p.getUuid().toString();
            pl.X = p.getX();
            pl.Y = p.getY();
            pl.Z = p.getZ();
            pl.Dimension = p.dimension.toString();
            pl.Health = p.getHealth();
            pl.IsBot = p.getClass().getName().equals(CarpetMPPatchName);

            if (pl.IsBot && pl.Name.startsWith(LambdaBotNamePrefix)) {
                pl.Name = pl.Name.substring(LambdaBotNamePrefix.length());
            }

            players.add(pl);
        }

        ServerStatusResponse r = new ServerStatusResponse();
        r.Online = server.getPlayerManager().getCurrentPlayerCount();
        r.Players = players;

        r.StartTime = server.getServerStartTime();

        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, new Gson().toJson(r));
    }

    private Response getMessages() {
        queueLock.lock();
        Response r = newFixedLengthResponse(Response.Status.OK, MIME_JSON, new Gson().toJson(messages.toArray()));
        messages.clear();
        queueLock.unlock();

        return r;
    }

    public void ProcessChatMessage(Text text) {
        if (text instanceof TranslatableText) {
            if (((TranslatableText) text).getKey().equals("chat.type.text") ||
                    ((TranslatableText) text).getKey().startsWith("death")) {
                String msg = text.getString();
                if (!msg.startsWith("[Disc")) {
                    queueLock.lock();
                    messages.add(msg);
                    queueLock.unlock();
                }
            }
        }
    }
}
