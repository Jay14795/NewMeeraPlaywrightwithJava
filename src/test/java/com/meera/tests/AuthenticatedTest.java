package com.meera.tests;

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
}
