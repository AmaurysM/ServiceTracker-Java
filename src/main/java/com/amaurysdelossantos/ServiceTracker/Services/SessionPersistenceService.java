package com.amaurysdelossantos.ServiceTracker.Services;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists the authenticated session cookie string in the OS native
 * credential store (Windows Credential Manager, macOS Keychain, or
 * libsecret on Linux) so the user stays logged in across restarts.
 *
 * Nothing is written to disk in plaintext — the OS keychain is the
 * only storage mechanism used.
 */
@Component
public class SessionPersistenceService {

    private static final Logger LOG = Logger.getLogger(SessionPersistenceService.class.getName());

    // Keychain service/account identifiers — change the service name to
    // match your app if you ever publish to an OS app store.
    private static final String KEYCHAIN_SERVICE = "LineTrainer";
    private static final String KEYCHAIN_ACCOUNT = "session-cookies";

    private Keyring keyring;

    public SessionPersistenceService() {
        try {
            keyring = Keyring.create();
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                    "OS keychain unavailable — session will not persist across restarts.", e);
            keyring = null;
        }
    }

    /**
     * Saves the full cookie string to the OS keychain.
     * Call this immediately after a successful login.
     */
    public void saveSession(String cookieHeader) {
        if (keyring == null || cookieHeader == null || cookieHeader.isBlank()) return;
        try {
            keyring.setPassword(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT, cookieHeader);
        } catch (PasswordAccessException e) {
            LOG.log(Level.WARNING, "Failed to save session to keychain.", e);
        }
    }

    /**
     * Loads the stored cookie string from the OS keychain.
     * Returns null if nothing is stored or the keychain is unavailable.
     */
    public String loadSession() {
        if (keyring == null) return null;
        try {
            String value = keyring.getPassword(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT);
            return (value == null || value.isBlank()) ? null : value;
        } catch (PasswordAccessException e) {
            // Entry doesn't exist yet — not an error, just no saved session
            return null;
        }
    }

    /**
     * Removes the stored session from the OS keychain.
     * Call this on logout so the next app start shows the login screen.
     */
    public void clearSession() {
        if (keyring == null) return;
        try {
            keyring.deletePassword(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT);
        } catch (PasswordAccessException e) {
            // Entry may not exist — safe to ignore
        }
    }

    public boolean isAvailable() {
        return keyring != null;
    }
}