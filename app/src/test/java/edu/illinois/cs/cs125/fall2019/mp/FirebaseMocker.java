package edu.illinois.cs.cs125.fall2019.mp;

import android.content.Intent;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import edu.illinois.cs.cs125.robolectricsecurity.Trusted;

@Trusted
final class FirebaseMocker {

    static final String UI_INTENT_PROPERTY = "_created_by_SIIB";
    static final String UI_INTENT_TOKEN = RandomHelper.randomId();

    private static String userEmail = null;
    private static String banMessage = null;

    private FirebaseMocker() { }

    static void mock() {
        PowerMockito.mockStatic(FirebaseAuth.class);
        PowerMockito.doAnswer(invocation -> {
            if (banMessage != null) {
                Assert.fail(banMessage);
            }
            FirebaseAuth authInstance = Mockito.mock(FirebaseAuth.class);
            Mockito.when(authInstance.getCurrentUser()).thenAnswer(userInvocation -> {
                if (banMessage != null) {
                    Assert.fail(banMessage);
                }
                if (userEmail == null) {
                    return null;
                } else {
                    FirebaseUser user = Mockito.mock(FirebaseUser.class);
                    Mockito.when(user.getEmail()).thenReturn(userEmail);
                    return user;
                }
            });
            return authInstance;
        }).when(FirebaseAuth.class);
        FirebaseAuth.getInstance();
    }

    static void mockAuthUI() {
        PowerMockito.mockStatic(AuthUI.class);
        Answer<AuthUI> answer = invocation -> {
            AuthUI uiInstance = Mockito.mock(AuthUI.class);
            Mockito.when(uiInstance.createSignInIntentBuilder()).thenCallRealMethod();
            return uiInstance;
        };
        PowerMockito.doAnswer(answer).when(AuthUI.class);
        AuthUI.getInstance();
        PowerMockito.doAnswer(answer).when(AuthUI.class);
        AuthUI.getInstance(Mockito.any());
        Intent intent = new Intent();
        intent.putExtra(UI_INTENT_PROPERTY, UI_INTENT_TOKEN);
        PowerMockito.stub(PowerMockito.method(AuthUI.SignInIntentBuilder.class, "build")).toReturn(intent);
    }

    static void setEmail(String email) {
        userEmail = email;
    }

    static void setBan(String message) {
        banMessage = message;
    }

}
