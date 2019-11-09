package edu.illinois.cs.cs125.fall2019.mp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketError;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketOpcode;

import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import edu.illinois.cs.cs125.robolectricsecurity.Trusted;

@Trusted
final class WebSocketMocker {

    private Consumer<JsonObject> client;

    private boolean connected = false;

    private String connectionUrl = null;

    private WebSocket webSocket;

    private JsonParser jsonParser = new JsonParser();

    private List<JsonObject> pendingMessages = new ArrayList<>();

    WebSocketMocker() {
        this(true);
    }

    private WebSocketMocker(boolean createNow) {
        if (createNow) createWebSocket();
    }

    static WebSocketMocker expectConnection() {
        WebApiMocker.ensureMocked();
        WebSocketMocker mocker = new WebSocketMocker(false);
        PowerMockito.doAnswer(invocation -> {
            String url = invocation.getArgument(0);
            Consumer<Throwable> errorReceiver = invocation.getArgument(4);
            if (!url.startsWith("ws")) {
                errorReceiver.accept(new WebSocketException(WebSocketError.NOT_SWITCHING_PROTOCOLS, "Not a websocket endpoint: " + url));
                return null;
            }
            if (!url.endsWith("/play")) {
                errorReceiver.accept(new WebSocketException(WebSocketError.NO_SEC_WEBSOCKET_ACCEPT_HEADER, "Not a game websocket endpoint: " + url));
                return null;
            }
            mocker.connectionUrl = url;
            mocker.client = invocation.getArgument(1);
            Consumer<WebSocket> wsReceiver = invocation.getArgument(2);
            mocker.createWebSocket();
            wsReceiver.accept(mocker.getWebSocket());
            return null;
        }).when(WebApi.class);
        WebApi.connectWebSocket(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        return mocker;
    }

    WebSocket getWebSocket() {
        return webSocket;
    }

    void sendData(JsonObject data) {
        if (client != null) client.accept(data);
    }

    boolean isConnected() {
        return connected;
    }

    String getConnectionUrl() {
        return connectionUrl;
    }

    void processMessages(Consumer<JsonObject> handler) {
        List<JsonObject> toProcess = new ArrayList<>(pendingMessages);
        try {
            toProcess.forEach(handler);
        } finally {
            pendingMessages.removeAll(toProcess);
        }
    }

    void processOneMessage(String failMessage, Consumer<JsonObject> handler) {
        if (pendingMessages.size() == 0) Assert.fail(failMessage);
        if (pendingMessages.size() > 1) Assert.fail("Expected only one websocket message");
        processMessages(handler);
    }

    void processOneMessage(String failMessage, Predicate<JsonObject> filter, Consumer<JsonObject> handler) {
        boolean any = false;
        try {
            for (JsonObject message : pendingMessages) {
                if (filter.test(message)) {
                    Assert.assertFalse("Expected only one websocket message", any);
                    handler.accept(message);
                    any = true;
                }
            }
            Assert.assertTrue(failMessage, any);
        } finally {
            pendingMessages.clear();
        }
    }

    void assertNoMessagesMatch(String failMessage, Predicate<JsonObject> predicate) {
        processMessages(message -> {
            if (predicate.test(message)) Assert.fail(failMessage);
        });
    }

    private void createWebSocket() {
        connected = true;
        webSocket = Mockito.mock(WebSocket.class);
        Mockito.when(webSocket.sendFrame(Mockito.any())).then(iom -> {
            WebSocketFrame frame = iom.getArgument(0);
            if (frame.getOpcode() == WebSocketOpcode.TEXT) addMessage(frame.getPayloadText());
            return webSocket;
        });
        Mockito.when(webSocket.sendText(Mockito.any())).then(iom -> {
            addMessage(iom.getArgument(0));
            return webSocket;
        });
        Mockito.when(webSocket.sendText(Mockito.any(), Mockito.anyBoolean())).then(iom -> webSocket.sendText(iom.getArgument(0)));
        Mockito.when(webSocket.disconnect()).then(iom -> {
            connected = false;
            return webSocket;
        });
        Answer<WebSocket> disconnectAnswer = iom -> webSocket.disconnect();
        Mockito.when(webSocket.disconnect(Mockito.anyInt())).then(disconnectAnswer);
        Mockito.when(webSocket.disconnect(Mockito.anyString())).then(disconnectAnswer);
        Mockito.when(webSocket.disconnect(Mockito.anyInt(), Mockito.anyString())).then(disconnectAnswer);
        Mockito.when(webSocket.disconnect(Mockito.anyInt(), Mockito.any(), Mockito.anyLong())).then(disconnectAnswer);
    }

    private void addMessage(String text) {
        try {
            pendingMessages.add(jsonParser.parse(text).getAsJsonObject());
        } catch (Exception e) {
            throw new RuntimeException("The websocket endpoint requires valid JSON objects", e);
        }
    }

}
