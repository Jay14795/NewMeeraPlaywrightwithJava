package com.meera.tests;

import com.meera.config.Config;
import com.meera.pages.CampaignPage;
import com.meera.pages.LoginPage;
import com.meera.utils.ExcelDataReader;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Create-campaign end-to-end tests. Port of
 * {@code tests/createCampaignTests.spec.js}.
 * Iterates the {@code GeneralSettings} sheet, joining the per-tab sheets by
 * testCase.
 *
 * <p>
 * Before running, validates the stored auth token. If the token is still valid,
 * skips re-login. Otherwise performs a fresh login and proceeds.
 * </p>
 */
public class CreateCampaignTests extends AuthenticatedTest {

    @DataProvider(name = "campaignData")
    public Object[][] campaignData() {
        return ExcelDataReader.getTestDataAsProvider(Config.CAMPAIGN_DATA, "GeneralSettings");
    }

    @Test(dataProvider = "campaignData", timeOut = 300_000)
    public void createCampaign(Map<String, String> data) {
        String testCase = data.get("testCase");
        System.out.println("Running test case: " + testCase);

        // Validate stored session; re-login if token expired
        ensureValidSession();

        List<Map<String, String>> numbersData = ExcelDataReader.getTestData(Config.CAMPAIGN_DATA, "Numbers");
        List<Map<String, String>> businessHrsData = ExcelDataReader.getTestData(Config.CAMPAIGN_DATA, "BusinessHrs");
        List<Map<String, String>> sysInitMessagesData = ExcelDataReader.getTestData(Config.CAMPAIGN_DATA,
                "SystemInitiatedMessages");
        List<Map<String, String>> callSettingsData = ExcelDataReader.getTestData(Config.CAMPAIGN_DATA, "CallSettings");
        List<Map<String, String>> transferSettingsData = ExcelDataReader.getTestData(Config.CAMPAIGN_DATA,
                "TransferSettings");

        CampaignPage campaignPage = new CampaignPage(page);
        attachPageEventLoggers(page);

        campaignPage.navigateToCampaignCreate(Config.CAMPAIGN_CREATE_URL);
        campaignPage.fillGeneralSettings(data);

        Map<String, String> numbersInfo = findByTestCase(numbersData, testCase);
        if (numbersInfo != null) {
            campaignPage.fillNumbersTab(numbersInfo);
        }

        Map<String, String> bizHrsInfo = findByTestCase(businessHrsData, testCase);
        if (bizHrsInfo != null) {
            campaignPage.fillBusinessHrs(bizHrsInfo);
        }

        Map<String, String> sysMsgInfo = findByTestCase(sysInitMessagesData, testCase);
        if (sysMsgInfo != null) {
            campaignPage.fillSystemInitiatedMessages(sysMsgInfo);
        }

        Map<String, String> callInfo = findByTestCase(callSettingsData, testCase);
        if (callInfo != null) {
            campaignPage.fillCallSettings(callInfo);
        }

        Map<String, String> transferInfo = findByTestCase(transferSettingsData, testCase);
        if (transferInfo != null) {
            campaignPage.fillTransferSettings(transferInfo);
        }

        campaignPage.clickAddCampaign();
        campaignPage.handleValidationPopup();
        campaignPage.verifyCampaignCreated();
    }

    /**
     * Checks if the stored auth session is still valid by navigating to
     * the campaign create URL. If redirected to login, performs a fresh
     * login and re-saves the session.
     */
    private void ensureValidSession() {
        Path authPath = Paths.get(Config.AUTH_STATE_PATH);
        if (!authPath.toFile().exists()) {
            System.out.println("No auth state found. Logging in...");
            performLogin(false);
            return;
        }

        System.out.println("Auth state found. Checking if token is still valid...");
        if (hasValidStoredSession(authPath, Config.CAMPAIGN_CREATE_URL)) {
            System.out.println("Token is still valid! Skipping login.");
            return;
        }
        System.out.println("Stored token is expired or rejected. Re-logging in...");

        performLogin(true);
    }

    private void performLogin(boolean forceLogin) {
        List<Map<String, String>> loginData = ExcelDataReader.getTestData(Config.LOGIN_DATA, "LoginData");
        Map<String, String> authUser = loginData.stream()
                .filter(d -> isTruthy(d.get("expectedURL")) && !isTruthy(d.get("expectedError")))
                .findFirst()
                .orElseGet(() -> loginData.stream()
                        .filter(d -> isTruthy(d.get("expectedURL")))
                        .findFirst()
                        .orElse(loginData.get(0)));

        System.out.println("Logging in as: " + authUser.get("email"));
        LoginPage loginPage = new LoginPage(page);
        if (forceLogin) {
            clearStoredSession();
        }
        loginPage.goTo(Config.BASE_URL);

        if (page.url().contains("/login") || page.url().contains("/signin")) {
            loginPage.verifyTitle("Sign In");
            loginPage.login(authUser.get("email"), authUser.get("password"));
            try {
                page.waitForLoadState(LoadState.NETWORKIDLE,
                        new Page.WaitForLoadStateOptions().setTimeout(30_000));
            } catch (RuntimeException e) {
                System.out.println("Network did not reach idle after login; continuing anyway.");
            }
        } else {
            throw new IllegalStateException(
                    "Fresh login was required, but the application did not show the login page. "
                            + "Current URL: " + page.url());
        }
        page.waitForTimeout(2000);

        context.storageState(new BrowserContext.StorageStateOptions().setPath(Paths.get(Config.AUTH_STATE_PATH)));
        System.out.println("Auth state saved to: " + Config.AUTH_STATE_PATH);
    }

    private void clearStoredSession() {
        System.out.println("Clearing expired browser session before login.");
        context.clearCookies();
        page.navigate(Config.BASE_URL,
                new Page.NavigateOptions()
                        .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(30_000));
        page.evaluate("() => { localStorage.clear(); sessionStorage.clear(); }");
    }

    private static boolean isTruthy(String s) {
        return s != null && !s.isEmpty();
    }

    private static Map<String, String> findByTestCase(List<Map<String, String>> rows, String testCase) {
        return rows.stream()
                .filter(r -> testCase != null && testCase.equals(r.get("testCase")))
                .findFirst()
                .orElse(null);
    }
}
