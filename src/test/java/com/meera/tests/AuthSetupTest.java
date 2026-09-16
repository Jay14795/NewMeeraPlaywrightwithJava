package com.meera.tests;

import com.meera.config.Config;
import com.meera.pages.LoginPage;
import com.meera.utils.ExcelDataReader;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Produces the stored login session at {@code playwright/.auth/auth.json}.
 * Port of {@code tests/auth.setup.js}, the TS "setup" project.
 *
 * <p>Reuses an existing session if it is still valid; otherwise performs a
 * fresh login and saves the storage state.</p>
 */
public class AuthSetupTest extends BaseTest {

    @Test(groups = {"setup"})
    public void authenticate() {
        List<Map<String, String>> loginData =
                ExcelDataReader.getTestData(Config.LOGIN_DATA, "LoginData");

        // Select a valid login (one without expectedError) and with expectedURL
        Map<String, String> authUser = loginData.stream()
                .filter(d -> isTruthy(d.get("expectedURL")) && !isTruthy(d.get("expectedError")))
                .findFirst()
                .orElseGet(() -> loginData.stream()
                        .filter(d -> isTruthy(d.get("expectedURL")))
                        .findFirst()
                        .orElse(loginData.get(0)));

        Path authPath = Paths.get(Config.AUTH_STATE_PATH);
        boolean authExists = authPath.toFile().exists();

        // ----- Try to reuse an existing, still-valid session ------------
        if (authExists) {
            System.out.println("Auth state found. Checking if token is still valid...");
            BrowserContext probeContext = null;
            try {
                probeContext = browser.newContext(
                        new com.microsoft.playwright.Browser.NewContextOptions()
                                .setViewportSize(null)
                                .setStorageStatePath(authPath));
                Page probe = probeContext.newPage();
                probe.navigate(Config.CAMPAIGN_CREATE_URL,
                        new Page.NavigateOptions().setWaitUntil(
                                com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE));

                if (!probe.url().contains("/login") && !probe.url().contains("/signin")) {
                    System.out.println("Token is still valid! Skipping login.");
                    // Re-save to refresh the stored state.
                    probeContext.storageState(new BrowserContext.StorageStateOptions().setPath(authPath));
                    System.out.println("Auth state already saved. Continuing with tests...");
                    return;
                } else {
                    System.out.println("Token expired (redirected to login). Need to login again.");
                }
            } catch (RuntimeException e) {
                System.out.println("Could not validate token: " + e.getMessage());
                System.out.println("Will perform fresh login.");
            } finally {
                if (probeContext != null) {
                    probeContext.close();
                }
            }
        } else {
            System.out.println("No auth state found. Performing fresh login.");
        }

        // ----- Fresh login ---------------------------------------------
        System.out.println("Logging in as: " + authUser.get("email"));
        LoginPage loginPage = new LoginPage(page);
        loginPage.goTo(Config.BASE_URL);
        loginPage.verifyTitle("Sign In");
        loginPage.login(authUser.get("email"), authUser.get("password"));
        page.waitForTimeout(2000);

        if (isTruthy(authUser.get("expectedURL"))) {
            try {
                assertThat(page).hasURL(authUser.get("expectedURL"),
                        new com.microsoft.playwright.assertions.PageAssertions.HasURLOptions()
                                .setTimeout(5000));
            } catch (AssertionError error) {
                System.out.println("Login may have failed. Current URL: " + page.url());
                System.out.println("Expected URL: " + authUser.get("expectedURL"));
                System.out.println("Proceeding anyway to save current state...");
            }
        }
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Always save the current state
        context.storageState(new BrowserContext.StorageStateOptions().setPath(authPath));
        System.out.println("Auth state saved to: " + authPath);
    }

    private static boolean isTruthy(String s) {
        return s != null && !s.isEmpty();
    }
}
