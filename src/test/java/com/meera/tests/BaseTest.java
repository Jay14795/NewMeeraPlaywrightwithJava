package com.meera.tests;

import com.meera.config.Config;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.LoadState;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for every test. Owns the Playwright lifecycle (Playwright →
 * Browser → BrowserContext → Page) that the TypeScript Playwright test runner
 * used to provide automatically through the {@code page} fixture.
 *
 * <p>
 * Browser options reproduce {@code playwright.config.ts}: headed Chromium,
 * {@code --start-maximized}, and {@code viewport: null} so the maximized window
 * size is used.
 * </p>
 *
 * <p>
 * Subclasses that need the stored login session override
 * {@link #useStorageState()} (or extend {@link AuthenticatedTest}); subclasses
 * that need browser permissions (e.g. microphone) override
 * {@link #permissions()}.
 * </p>
 */
public abstract class BaseTest {

    static {
        // Equivalent of expect{ timeout: 40_000 } in the TS config.
        PlaywrightAssertions.setDefaultAssertionTimeout(Config.ASSERTION_TIMEOUT);
    }

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    private static final Object BROWSER_LOCK = new Object();
    private static Playwright suitePlaywright;
    private static Browser suiteBrowser;

    /** Override to load {@code playwright/.auth/auth.json} into the context. */
    protected boolean useStorageState() {
        return false;
    }

    /**
     * Override to grant context permissions, e.g. {@code List.of("microphone")}.
     */
    protected List<String> permissions() {
        return Collections.emptyList();
    }

    @BeforeSuite(alwaysRun = true)
    public void startBrowser() {
        synchronized (BROWSER_LOCK) {
            if (suiteBrowser != null) {
                return;
            }
            suitePlaywright = Playwright.create();
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(Config.HEADLESS)
                    .setArgs(List.of("--start-maximized"));
            suiteBrowser = suitePlaywright.chromium().launch(launchOptions);
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        if (suiteBrowser == null) {
            startBrowser();
        }
        playwright = suitePlaywright;
        browser = suiteBrowser;

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                // viewport: null — use the real (maximized) window size
                .setViewportSize(null);

        if (useStorageState()) {
            Path authPath = Paths.get(Config.AUTH_STATE_PATH);
            if (authPath.toFile().exists()) {
                contextOptions.setStorageStatePath(authPath);
            } else {
                System.out.println("Stored auth session not found. Authenticated test will perform login.");
            }
        }

        List<String> perms = permissions();
        if (perms != null && !perms.isEmpty()) {
            contextOptions.setPermissions(perms);
        }

        context = browser.newContext(contextOptions);
        page = context.newPage();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (context != null) {
            context.close();
        }
        context = null;
        page = null;
    }

    @AfterSuite(alwaysRun = true)
    public void stopBrowser() {
        synchronized (BROWSER_LOCK) {
            if (suiteBrowser != null) {
                suiteBrowser.close();
                suiteBrowser = null;
            }
            if (suitePlaywright != null) {
                suitePlaywright.close();
                suitePlaywright = null;
            }
        }
        browser = null;
        playwright = null;
    }

    /**
     * Attaches the diagnostic page-event loggers used throughout the original
     * specs ({@code page.on("dialog"/"close"/"crash"/"popup")}). Dialogs are
     * auto-accepted, matching the TS handlers.
     */
    protected void attachPageEventLoggers(Page page) {
        page.onDialog(dialog -> {
            System.out.println("PAGE EVENT: dialog " + dialog.message());
            dialog.accept();
        });
        page.onClose(p -> System.out.println("PAGE EVENT: close"));
        page.onCrash(p -> System.out.println("PAGE EVENT: crash"));
        page.onPopup(popup -> System.out.println("PAGE EVENT: popup " + popup.url()));
    }

    /**
     * Checks the stored session against a protected route. A SPA can leave the
     * browser on an application URL even when its API calls are unauthorized,
     * so the URL alone is not sufficient evidence that the session is valid.
     */
    protected boolean hasValidStoredSession(Path authPath, String protectedUrl) {
        BrowserContext probeContext = null;
        try {
            probeContext = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(null)
                    .setStorageStatePath(authPath));
            Page probe = probeContext.newPage();
            AtomicBoolean unauthorizedResponse = new AtomicBoolean(false);
            probe.onResponse(response -> {
                int status = response.status();
                if (status == 401 || status == 403) {
                    unauthorizedResponse.set(true);
                }
            });

            probe.navigate(protectedUrl, new Page.NavigateOptions()
                    .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(30_000));
            try {
                probe.waitForLoadState(LoadState.NETWORKIDLE,
                        new Page.WaitForLoadStateOptions().setTimeout(10_000));
            } catch (RuntimeException e) {
                System.out.println("Auth probe did not reach network idle; checking collected responses.");
            }

            String currentUrl = probe.url().toLowerCase(Locale.ROOT);
            boolean onLoginPage = currentUrl.contains("/login")
                    || currentUrl.contains("/signin")
                    || probe.locator("input[placeholder='Email']").isVisible();
            return !onLoginPage && !unauthorizedResponse.get();
        } catch (RuntimeException e) {
            System.out.println("Could not validate token: " + e.getMessage());
            return false;
        } finally {
            if (probeContext != null) {
                probeContext.close();
            }
        }
    }
}
