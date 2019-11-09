package edu.illinois.cs.cs125.fall2019.mp;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.powermock.api.mockito.PowerMockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import edu.illinois.cs.cs125.robolectricsecurity.Trusted;

@Trusted
final class WebApiMocker {

    private static List<InvocationOnMock> toDeliver = new ArrayList<>();

    private static boolean isMocked = false;

    private WebApiMocker() { }

    static void ensureMocked() {
        if (isMocked) return;
        PowerMockito.mockStatic(WebApi.class);
        isMocked = true;
    }

    static void reset() {
        isMocked = false;
        toDeliver.clear();
    }

    static void interceptHttp() {
        ensureMocked();
        toDeliver.clear();
        PowerMockito.doAnswer(invocation -> {
            Objects.requireNonNull(invocation.getArgument(0));
            toDeliver.add(invocation);
            return null;
        }).when(WebApi.class);
        WebApi.startRequest(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any());
        PowerMockito.doCallRealMethod().when(WebApi.class);
        WebApi.startRequest(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    static void process(MockedApiCallHandler handler) {
        List<InvocationOnMock> initialToDeliver = new ArrayList<>(toDeliver);
        try {
            for (InvocationOnMock iom : initialToDeliver) {
                Response.ErrorListener errorListener = iom.getArgument(5);
                String url = iom.getArgument(1);
                if (!url.startsWith(WebApi.API_BASE)) {
                    errorListener.onErrorResponse(new VolleyError("Tried to connect to an incorrect server."));
                    continue;
                }
                int method = iom.getArgument(2);
                if (method != Request.Method.GET && method != Request.Method.POST) {
                    errorListener.onErrorResponse(new VolleyError("Unsupported HTTP method."));
                    continue;
                }
                handler.onApiRequest(url.substring(WebApi.API_BASE.length()), method,
                        iom.getArgument(3), iom.getArgument(4), iom.getArgument(5));
            }
        } finally {
            toDeliver.removeAll(initialToDeliver);
        }
    }

    static void processOne(String failureMessage, MockedApiCallHandler handler) {
        if (toDeliver.size() == 0) Assert.fail(failureMessage);
        if (toDeliver.size() > 1) Assert.fail("Extra HTTP requests were made");
        process(handler);
    }

    interface MockedApiCallHandler {

        void onApiRequest(String path, int method, JsonElement body, Response.Listener<JsonObject> callback, Response.ErrorListener errorListener);

    }

}
