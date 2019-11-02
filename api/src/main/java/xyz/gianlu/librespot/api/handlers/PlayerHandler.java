package xyz.gianlu.librespot.api.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.gianlu.librespot.api.Utils;
import xyz.gianlu.librespot.core.Session;
import xyz.gianlu.librespot.player.PlayerRunner;

import java.util.Deque;
import java.util.Map;
import java.util.Objects;

public final class PlayerHandler implements HttpHandler {
    private final Session session;

    public PlayerHandler(@NotNull Session session) {
        this.session = session;
    }

    private void setVolume(HttpServerExchange exchange, @Nullable String valStr) {
        if (valStr == null) {
            Utils.invalidParameter(exchange, "volume");
            return;
        }

        int val;
        try {
            val = Integer.parseInt(valStr);
        } catch (Exception ex) {
            Utils.invalidParameter(exchange, "volume", "Not an integer");
            return;
        }

        if (val < 0 || val > PlayerRunner.VOLUME_MAX) {
            Utils.invalidParameter(exchange, "volume", "Must be >= 0 and <= " + PlayerRunner.VOLUME_MAX);
            return;
        }

        session.player().setVolume(val);
    }

    private void load(HttpServerExchange exchange, @Nullable String uri, boolean play) {
        if (uri == null) {
            Utils.invalidParameter(exchange, "uri");
            return;
        }

        session.player().load(uri, play);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.startBlocking();
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        Map<String, Deque<String>> params = Utils.readParameters(exchange);
        String cmdStr = Utils.getFirstString(params, "cmd");
        if (cmdStr == null) {
            Utils.invalidParameter(exchange, "cmd");
            return;
        }

        Command cmd = Command.parse(cmdStr);
        if (cmd == null) {
            Utils.invalidParameter(exchange, "cmd");
            return;
        }

        switch (cmd) {
            case SET_VOLUME:
                setVolume(exchange, Utils.getFirstString(params, "volume"));
                return;
            case VOLUME_UP:
                session.player().volumeUp();
                return;
            case VOLUME_DOWN:
                session.player().volumeDown();
                return;
            case LOAD:
                load(exchange, Utils.getFirstString(params, "uri"), Utils.getFirstBoolean(params, "play"));
                return;
            case PAUSE:
                session.player().pause();
                return;
            case RESUME:
                session.player().play();
                return;
            case PREV:
                session.player().previous();
                return;
            case NEXT:
                session.player().next();
                return;
            default:
                throw new IllegalArgumentException(cmd.name());
        }
    }

    private enum Command {
        LOAD("load"), PAUSE("pause"), RESUME("resume"),
        NEXT("next"), PREV("prev"), SET_VOLUME("set-volume"),
        VOLUME_UP("volume-up"), VOLUME_DOWN("volume-down");

        private String name;

        Command(String name) {
            this.name = name;
        }

        @Nullable
        private static Command parse(@NotNull String val) {
            for (Command cmd : values())
                if (Objects.equals(cmd.name, val))
                    return cmd;

            return null;
        }
    }
}
