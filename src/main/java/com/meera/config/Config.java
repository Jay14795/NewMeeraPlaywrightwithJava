package com.meera.config;

/**
 * Central configuration constants for the Meera Playwright suite.
 *
 * <p>Mirrors the values that were scattered across the TypeScript
 * {@code playwright.config.ts} and the individual spec files.</p>
 */
public final class Config {

    private Config() {
        // constants holder
    }

    /* ---------------------------------------------------------------
     * Application under test
     * --------------------------------------------------------------- */
    public static final String BASE_URL = "https://web-uat04.services.meera.ai/";
    public static final String CAMPAIGN_CREATE_URL = BASE_URL + "campaign/create7";
    public static final String CAMPAIGN_REPORTS_URL = BASE_URL + "campaign/reports";
    public static final String STATE_COMPLIANCE_URL = BASE_URL + "state-compliance";
    public static final String ASSISTANTS_URL = BASE_URL + "list/open-ai-assistants";
    /** Campaign used by the Test &amp; Train (voice) module. */
    public static final String TEST_AND_TRAIN_CAMPAIGN_URL = BASE_URL + "campaign/edit7/2027";

    /* ---------------------------------------------------------------
     * Storage-state (authenticated session) — equivalent of the TS
     * "setup" project writing playwright/.auth/auth.json
     * --------------------------------------------------------------- */
    public static final String AUTH_STATE_PATH = "playwright/.auth/auth.json";

    /* ---------------------------------------------------------------
     * Test data (Excel) locations, relative to the project root
     * --------------------------------------------------------------- */
    public static final String LOGIN_DATA = "test-data/login-data.xlsx";
    public static final String CAMPAIGN_DATA = "test-data/campaign-data.xlsx";
    public static final String ASSISTANT_DATA = "test-data/assistant-data.xlsx";

    /* ---------------------------------------------------------------
     * Timeouts (milliseconds) — from playwright.config.ts
     * --------------------------------------------------------------- */
    /** expect{ timeout } in the TS config. */
    public static final double ASSERTION_TIMEOUT = 40_000;
    /** timeout in the TS config (per-test). */
    public static final double TEST_TIMEOUT = 300_000;

    /* ---------------------------------------------------------------
     * Browser launch — headless:false, --start-maximized, viewport:null
     * --------------------------------------------------------------- */
    public static final boolean HEADLESS = false;
}
