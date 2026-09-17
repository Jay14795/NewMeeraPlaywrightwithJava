package com.meera.tests;

import com.meera.config.Config;
import com.meera.pages.LoginPage;
import com.meera.utils.ExcelDataReader;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.testng.annotations.BeforeMethod;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Base class for tests that require the stored login session
 * ({@code playwright/.auth/auth.json}), which is produced by
 * {@link AuthSetupTest}. Equivalent of the TS projects that declared
 * {@code storageState: "playwright/.auth/auth.json"} and
 * {@code dependencies: ["setup"]}.
 */
public abstract class AuthenticatedTest extends BaseTest {

    @Override
    protected boolean useStorageState() {
        return true;
    }

    @BeforeMethod(alwaysRun = true)
    public void ensureAuthenticatedSession() {
        Path authPath = Paths.get(Config.AUTH_STATE_PATH);
        if (authPath.toFile().exists() && hasValidStoredSession(authPath, Config.CAMPAIGN_CREATE_URL)) {
            System.out.println("Stored token is valid.");
            return;
        }

        System.out.println("Stored token is missing or expired. Performing fresh login...");
        List<Map<String, String>> loginData = ExcelDataReader.getTestData(Config.LOGIN_DATA, "LoginData");
        Map<String, String> authUser = loginData.stream()
                .filter(d -> isTruthy(d.get("expectedURL")) && !isTruthy(d.get("expectedError")))
                .findFirst()
                .orElseGet(() -> loginData.stream()
                        .filter(d -> isTruthy(d.get("expectedURL")))
                        .findFirst()
                        .orElse(loginData.get(0)));

        clearCurrentSession();
        LoginPage loginPage = new LoginPage(page);
        loginPage.goTo(Config.BASE_URL);
        if (!loginPage.isLoginFormVisible()) {
            throw new IllegalStateException(
                    "Fresh login was required, but the login form was not visible. Current URL: " + page.url());
        }
        loginPage.login(authUser.get("email"), authUser.get("password"));
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(30_000));
        } catch (RuntimeException e) {
            System.out.println("Network did not reach idle after login; continuing.");
        }
        context.storageState(new BrowserContext.StorageStateOptions().setPath(authPath));
        System.out.println("Auth state saved to: " + authPath);
    }

    private void clearCurrentSession() {
        context.clearCookies();
        page.navigate(Config.BASE_URL,
                new Page.NavigateOptions()
                        .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(30_000));
        page.evaluate("() => { localStorage.clear(); sessionStorage.clear(); }");
    }

    private static boolean isTruthy(String value) {
        return value != null && !value.isEmpty();
    }
}
