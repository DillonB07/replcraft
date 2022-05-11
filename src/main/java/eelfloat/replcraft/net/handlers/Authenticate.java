package eelfloat.replcraft.net.handlers;

import eelfloat.replcraft.ReplCraft;
import eelfloat.replcraft.util.StructureUtil;
import eelfloat.replcraft.exceptions.ApiError;
import eelfloat.replcraft.exceptions.InvalidStructure;
import eelfloat.replcraft.net.Client;
import io.javalin.websocket.WsMessageContext;
import org.json.JSONObject;

@WebsocketAction(route = "authenticate", permission = "", cost = FuelCost.None, authenticated = false)
public class Authenticate implements WebsocketActionHandler {
    @Override
    public void execute(Client client, WsMessageContext ctx, JSONObject request, JSONObject response) throws InvalidStructure, ApiError {
        client.setStructure(
            StructureUtil.verifyToken(request.getString("token")),
            request.getString("token")
        );
        ReplCraft.plugin.logger.info("Client " + ctx.session.getRemoteAddress() + " authenticated: " + client.getStructure());
    }
}