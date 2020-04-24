package dev.lambdacraft.status_provider;

// import ;
import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServerStatus extends NanoHTTPD {

    public static final String MIME_JSON = "application/json";
    public static final String KEY_HEADER = "x-fabric-server-status";

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
    }

    @Override
    public Response serve(IHTTPSession session) {
        String key = session.getHeaders().get(KEY_HEADER);
        String secret = StatusMain.props.getProperty("secret", "");
        if (!secret.equals("")) {
            Boolean secretCheck = key != null && key.equals(secret);
            if (!secretCheck || session.getMethod() != Method.GET) {
                return newFixedLengthResponse("{\"error\": \"Incorrect secret. Secret should be specified in 'x-fabric-server-status' HTTP header \"}");
            }
        }

        List<PlayerStatus> players = new ArrayList<>();

        for (ServerPlayerEntity p :
                server.getPlayerManager().getPlayerList()) {
            PlayerStatus pl = new PlayerStatus();
            pl.Name = p.getName().getString();
            pl.UUID = p.getUuid().toString();
            pl.X = p.x;
            pl.Y = p.y;
            pl.Z = p.z;
            pl.Dimension = p.dimension.toString();
            pl.Health = p.getHealth();
            pl.IsBot = pl.Name.startsWith("[BOT] ");

            players.add(pl);
        }

        ServerStatusResponse r = new ServerStatusResponse();
        r.Online = server.getPlayerManager().getCurrentPlayerCount();
        r.Players = players;

        r.StartTime = server.getServerStartTime();

        Response res = newFixedLengthResponse(Response.Status.OK, MIME_JSON, new Gson().toJson(r));
        res.addHeader("Access-Control-Allow-Origin", "*");
        return res;
    }
}
