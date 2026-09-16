package com.meera.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Login page object. Port of {@code pages/LoginPage.js}.
 */
public class LoginPage {

    private final Page page;
    private final Locator emailInput;
    private final Locator passwordInput;
    private final Locator signInButton;
    private final Locator alertMessage;

    public LoginPage(Page page) {
        this.page = page;
        this.emailInput = page.locator("input[placeholder='Email']");
        this.passwordInput = page.locator("input[placeholder='Password']");
        this.signInButton = page.locator("#kt_login_singin_form_submit_button");
        this.alertMessage = page.locator("span[role='alert']");
    }

    public void goTo(String url) {
        page.navigate(url);
        page.waitForLoadState(LoadState.LOAD);
    }

    public void verifyTitle(String title) {
        assertThat(page).hasTitle(title);
    }

    public void enterEmail(String email) {
        emailInput.fill(email);
    }

    public void enterPassword(String password) {
        passwordInput.fill(password);
    }

    public void clickSignIn() {
        signInButton.click();
    }

    public void verifyUrl(String expectedUrl) {
        assertThat(page).hasURL(expectedUrl);
    }

    public String getAlertText() {
        return alertMessage.textContent();
    }

    public void verifyAlertContains(String text) {
        assertThat(alertMessage).containsText(text);
    }

    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickSignIn();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }
}
