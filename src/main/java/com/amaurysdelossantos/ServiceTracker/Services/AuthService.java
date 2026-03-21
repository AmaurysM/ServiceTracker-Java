package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.models.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuthService {

    private static final String BASE_URL = "https://www.linetrainer.org";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    private final StringProperty authToken = new SimpleStringProperty();
    private final ObjectProperty<User> currentUser = new SimpleObjectProperty<>();

    @Autowired
    private SessionPersistenceService sessionPersistence;

    public AuthService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    // ─── Session restore ───────────────────────────────────────────

    /**
     * Called at app startup. Attempts to restore the previous session from
     * the OS keychain. If a valid session cookie exists it hits /api/auth/session
     * to verify it's still live, then fetches the full user profile.
     *
     * Returns true if the session was successfully restored — the caller can
     * skip the login screen entirely. Returns false if the session is expired,
     * missing, or the server is unreachable.
     */
    public boolean restoreSession() {
        String storedCookies = sessionPersistence.loadSession();
        if (storedCookies == null) return false;

        try {
            // Verify the session is still valid with the server
            HttpRequest sessionRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/session"))
                    .header("Cookie", storedCookies)
                    .header("Accept", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> sessionResponse = httpClient.send(
                    sessionRequest, HttpResponse.BodyHandlers.ofString()
            );

            JsonNode session = objectMapper.readTree(sessionResponse.body());
            JsonNode userNode = session.path("user");

            // Empty session object means the cookie has expired
            if (userNode.isMissingNode() || userNode.isNull()) {
                sessionPersistence.clearSession();
                return false;
            }

            User user = new User();
            user.setEmail(userNode.path("email").asText());

            // Fetch full profile
            String userId = userNode.path("id").asText();
            if (!userId.isEmpty()) {
                HttpRequest userRequest = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/api/user/" + userId))
                        .header("Cookie", storedCookies)
                        .header("Accept", "application/json")
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> userResponse = httpClient.send(
                        userRequest, HttpResponse.BodyHandlers.ofString()
                );

                if (userResponse.statusCode() == 200) {
                    JsonNode u = objectMapper.readTree(userResponse.body());
                    user.setId(u.path("_id").asText());
                    user.setFirstname(u.path("firstname").asText());
                    user.setLastname(u.path("lastname").asText());
                    user.setEmail(u.path("email").asText());
                    user.setUsername(u.path("username").asText());
                    user.setNickname(u.path("nickname").asText());
                    user.setCompanyId(u.path("companyId").asText());
                }
            }

            authToken.set(storedCookies);
            currentUser.set(user);
            return true;

        } catch (Exception e) {
            // Network error — don't clear the stored session, the user may be offline.
            // They'll need to log in manually this time but the cookie stays for next time.
            System.err.println("[AuthService] Session restore failed (network?): " + e.getMessage());
            return false;
        }
    }

    // ─── Login ─────────────────────────────────────────────────────

    /**
     * Logs in via NextAuth credentials flow:
     *   1. GET  /api/auth/csrf
     *   2. POST /api/auth/callback/credentials
     *   3. GET  /api/auth/session
     *   4. GET  /api/user/{id}
     *
     * On success the session cookies are saved to the OS keychain.
     */
    public LoginResult login(String email, String password) {
        try {

            // ── Step 1: CSRF token ──────────────────────────────
            HttpRequest csrfRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/csrf"))
                    .header("Accept", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> csrfResponse = httpClient.send(
                    csrfRequest, HttpResponse.BodyHandlers.ofString()
            );

            List<String> csrfCookies = csrfResponse.headers().allValues("set-cookie");
            JsonNode csrfJson = objectMapper.readTree(csrfResponse.body());
            String csrfToken = csrfJson.path("csrfToken").asText();

            if (csrfToken.isEmpty()) {
                return new LoginResult(false, "Could not retrieve CSRF token", null, null);
            }

            String csrfCookieHeader = csrfCookies.stream()
                    .map(c -> c.split(";")[0])
                    .collect(Collectors.joining("; "));

            // ── Step 2: POST credentials ────────────────────────
            String formBody = "csrfToken=" + java.net.URLEncoder.encode(csrfToken, "UTF-8")
                    + "&email="       + java.net.URLEncoder.encode(email, "UTF-8")
                    + "&password="    + java.net.URLEncoder.encode(password, "UTF-8")
                    + "&callbackUrl=" + java.net.URLEncoder.encode(BASE_URL + "/", "UTF-8")
                    + "&json=true";

            HttpRequest loginRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/callback/credentials"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Cookie", csrfCookieHeader)
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> loginResponse = httpClient.send(
                    loginRequest, HttpResponse.BodyHandlers.ofString()
            );

            if (loginResponse.statusCode() != 200) {
                return new LoginResult(false, "Invalid credentials", null, null);
            }

            List<String> sessionCookies = loginResponse.headers().allValues("set-cookie");
            String sessionCookieHeader = sessionCookies.stream()
                    .map(c -> c.split(";")[0])
                    .collect(Collectors.joining("; "));

            String fullCookieHeader = csrfCookieHeader + "; " + sessionCookieHeader;

            // ── Step 3: fetch session ───────────────────────────
            HttpRequest sessionRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/session"))
                    .header("Cookie", fullCookieHeader)
                    .header("Accept", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> sessionResponse = httpClient.send(
                    sessionRequest, HttpResponse.BodyHandlers.ofString()
            );

            JsonNode session = objectMapper.readTree(sessionResponse.body());
            JsonNode userNode = session.path("user");

            if (userNode.isMissingNode() || userNode.isNull()) {
                return new LoginResult(false, "Invalid credentials", null, null);
            }

            User user = new User();
            user.setEmail(userNode.path("email").asText());

            // ── Step 4: full user profile ───────────────────────
            String userId = userNode.path("id").asText();
            if (!userId.isEmpty()) {
                HttpRequest userRequest = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/api/user/" + userId))
                        .header("Cookie", fullCookieHeader)
                        .header("Accept", "application/json")
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> userResponse = httpClient.send(
                        userRequest, HttpResponse.BodyHandlers.ofString()
                );

                if (userResponse.statusCode() == 200) {
                    JsonNode u = objectMapper.readTree(userResponse.body());
                    user.setId(u.path("_id").asText());
                    user.setFirstname(u.path("firstname").asText());
                    user.setLastname(u.path("lastname").asText());
                    user.setEmail(u.path("email").asText());
                    user.setUsername(u.path("username").asText());
                    user.setNickname(u.path("nickname").asText());
                    user.setCompanyId(u.path("companyId").asText());
                }
            }

            authToken.set(fullCookieHeader);
            currentUser.set(user);

            // ── Persist session to OS keychain ──────────────────
            sessionPersistence.saveSession(fullCookieHeader);

            return new LoginResult(true, "Login successful", fullCookieHeader, user);

        } catch (Exception e) {
            return new LoginResult(false, "Connection error: " + e.getMessage(), null, null);
        }
    }

    // ─── Register ──────────────────────────────────────────────────

    public RegisterResult register(String firstname, String lastname,
                                   String email, String username, String password) {
        try {
            RegisterRequest regBody = new RegisterRequest(
                    firstname, lastname, email, username, password
            );

            String body = objectMapper.writeValueAsString(regBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            JsonNode json = objectMapper.readTree(response.body());

            if (response.statusCode() == 201) {
                return new RegisterResult(true,
                        json.path("message").asText("Registered successfully"));
            } else {
                String error = json.path("error").asText("");
                if (error.isEmpty()) error = "Registration failed";

                JsonNode details = json.path("details");
                if (!details.isMissingNode() && !details.isNull()) {
                    JsonNode fieldErrors = details.path("fieldErrors");
                    if (!fieldErrors.isMissingNode()) {
                        StringBuilder sb = new StringBuilder(error).append(": ");
                        fieldErrors.fields().forEachRemaining(entry ->
                                sb.append(entry.getKey())
                                        .append(" — ")
                                        .append(entry.getValue())
                                        .append("  "));
                        error = sb.toString().trim();
                    }
                }
                return new RegisterResult(false, error);
            }

        } catch (Exception e) {
            return new RegisterResult(false, "Connection error: " + e.getMessage());
        }
    }

    // ─── Logout ────────────────────────────────────────────────────

    /**
     * Clears the in-memory session AND removes the stored cookie from the
     * OS keychain so the next app start shows the login screen.
     */
    public void logout() {
        authToken.set(null);
        currentUser.set(null);
        sessionPersistence.clearSession();
    }

    // ─── Accessors ─────────────────────────────────────────────────

    public StringProperty authTokenProperty()          { return authToken; }
    public ObjectProperty<User> currentUserProperty()  { return currentUser; }
    public String getAuthToken()                       { return authToken.get(); }
    public User getCurrentUser()                       { return currentUser.get(); }
    public boolean isLoggedIn()                        { return authToken.get() != null; }

    // ─── DTOs ──────────────────────────────────────────────────────

    public record RegisterRequest(
            String firstname, String lastname,
            String email, String username, String password,
            String role, List<String> modules,
            boolean deleted, boolean archived
    ) {
        public RegisterRequest(String firstname, String lastname,
                               String email, String username, String password) {
            this(firstname, lastname, email, username, password,
                    "student", List.of(), false, false);
        }
    }

    public record LoginResult(boolean success, String message, String token, User user) {}
    public record RegisterResult(boolean success, String message) {}
}