package com.meera.pages;

import com.meera.config.Config;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * State-compliance page object. Port of {@code pages/StateCompliancePage.js}.
 */
public class StateCompliancePage {

    private final Page page;
    private final Locator searchBox;
    private final Locator tableBody;
    private final Locator startTimeCell;
    private final Locator endTimeCell;

    public StateCompliancePage(Page page) {
        this.page = page;

        this.searchBox = page.getByRole(AriaRole.SEARCHBOX,
                new Page.GetByRoleOptions().setName("Search:"));
        this.tableBody = page.locator("tbody");

        this.startTimeCell = page.getByRole(AriaRole.GRIDCELL,
                new Page.GetByRoleOptions().setName(":00 AM 01:00 AM 01:00 AM"));
        this.endTimeCell = page.getByRole(AriaRole.GRIDCELL,
                new Page.GetByRoleOptions().setName(":00 PM 09:00 PM 09:00 PM"));
    }

    public void gotoHome() {
        page.navigate(Config.BASE_URL);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForTimeout(3000);

        // Check if we need to log in
        Locator emailInput = page.locator("input[placeholder='Email']");
        boolean isLoginPage = emailInput.isVisible();
        if (isLoginPage) {
            emailInput.click();
            emailInput.fill("admin@chatbot.com");
            Locator passwordInput = page.locator("input[placeholder='Password']");
            passwordInput.click();
            passwordInput.fill("Newmeera@1234");
            Locator signInButton = page.locator("#kt_login_singin_form_submit_button");
            signInButton.click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(2000);
        }
    }

    public void openStateCompliancePage() {
        page.navigate(Config.STATE_COMPLIANCE_URL);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertThat(searchBox).isVisible(
                new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions()
                        .setTimeout(30000));
    }

    public void searchState(String stateName) {
        searchBox.fill(stateName);
        searchBox.press("Enter");

        assertThat(tableBody).isVisible();
        System.out.println("Searched for state: \"" + stateName + "\" - table is visible");
    }

    public void verifyStateComplianceData() {
        // 1. Read the actual content first
        String tableText = tableBody.innerText();
        System.out.println("----- State Compliance Table Content -----");
        System.out.println(tableText);
        System.out.println("------------------------------------------");

        // 2. Compute the conditions so you can log pass/fail
        boolean hasSchedule = tableText.contains("Monday-FridaySaturdaySunday");
        boolean isActive = tableText.contains("Active");

        System.out.println("Contains schedule (Mon-Fri/Sat/Sun): " + hasSchedule);
        System.out.println("Status is Active: " + isActive);

        // 3. Run the assertions
        assertThat(tableBody).containsText("Monday-FridaySaturdaySunday");
        assertThat(tableBody).containsText("Active");

        System.out.println("All state compliance assertions passed");
    }

    public void clickStartTimeCell() {
        assertThat(startTimeCell).isVisible();
        String value = startTimeCell.innerText();
        System.out.println("Start time cell value: " + value);
        startTimeCell.click();
        System.out.println("Clicked start time cell");
    }

    public void clickEndTimeCell() {
        assertThat(endTimeCell).isVisible();
        String value = endTimeCell.innerText();
        System.out.println("End time cell value: " + value);
        endTimeCell.click();
        System.out.println("Clicked end time cell");
    }
}
