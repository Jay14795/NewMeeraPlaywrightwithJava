package com.meera.tests;

import com.meera.config.Config;
import com.meera.utils.ExcelDataReader;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Edit-campaign (grounded RAG + business hours + update) flow.
 * Port of {@code tests/editCampaignTests.spec.js}.
 */
public class EditCampaignTests extends AuthenticatedTest {

        @DataProvider(name = "editCampaignData")
        public Object[][] editCampaignData() {
                return ExcelDataReader.getTestDataAsProvider(Config.CAMPAIGN_DATA, "EditCampaign");
        }

        @Test(dataProvider = "editCampaignData", timeOut = 300_000)
        public void editCampaign(Map<String, String> data) {
                System.out.println("Running test case: " + data.get("testCase"));
                attachPageEventLoggers(page);

                String campaignName = data.get("campaignName");

                // Step 2: Navigate to campaign reports page
                page.navigate(Config.CAMPAIGN_REPORTS_URL,
                                new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

                // Step 3: Search the Campaign name
                page.locator("input[type='search']").fill(campaignName);
                page.waitForTimeout(2000);
                page.keyboard().press("Enter");
                page.waitForTimeout(2000);

                // Step 4: Click on the campaign and open the grounded rag tab
                page.locator("text=\"" + campaignName + "\"").first().click();
                page.waitForLoadState(LoadState.NETWORKIDLE);

                page.locator("a[href=\"#groundedTab\"]").click();
                page.waitForTimeout(1500);

                // Click on "1. Create Knowledge Base" sub-tab if not already active
                page.locator("a[href=\"#kt_tab_pane_5_1\"]").click();
                page.waitForTimeout(1000);

                // Step 5: Enter URL in the Website/URL Knowledge Source section
                page.locator("text=Website/URL Knowledge Source")
                                .waitFor(new Locator.WaitForOptions()
                                                .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
                page.getByPlaceholder("Enter URL (e.g., https://example.com/page)")
                                .fill("https://cameobeautyacademy.com");
                page.waitForTimeout(500);

                // Step 6: Hit the Get URLs button and wait for results to load
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Get URLs")).click();
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                                .setName(Pattern.compile("select all", Pattern.CASE_INSENSITIVE)))
                                .waitFor(new Locator.WaitForOptions()
                                                .setState(WaitForSelectorState.VISIBLE).setTimeout(60000));
                page.waitForTimeout(1000);

                // Step 7: Click on select all button
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                                .setName(Pattern.compile("select all", Pattern.CASE_INSENSITIVE))).click();
                page.waitForTimeout(500);

                // Step 8: Click on the submit button
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                                .setName(Pattern.compile("submit", Pattern.CASE_INSENSITIVE))).click();
                page.waitForTimeout(2000);

                // Step 9: Click on the business hours tab and select mode to On
                page.locator("a[href=\"#tab_6_6\"]").click();
                page.waitForTimeout(1500);

                // Select mode to On (click label wrapping radio to avoid interception)
                page.locator("label:has(input[name=\"mode\"][value=\"1\"])").click();
                page.waitForTimeout(500);

                // Handle confirmation dialog "Are you sure you want to turn On the campaign?"
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Yes")).first().click();
                page.waitForTimeout(1000);

                // Step 10: Click on the update button
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Update")).first().click();
                waitForUpdateResult();

                // Step 11: Assert success message
                assertThat(page.locator("text=Successfully Updated the detail")).isVisible(
                                new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions()
                                                .setTimeout(60000));
        }

        private void waitForUpdateResult() {
                try {
                        page.waitForFunction(
                                        "() => {"
                                                        + " const success = Array.from(document.querySelectorAll('body *')).some(el =>"
                                                        + " el.offsetParent !== null && /Successfully Updated the detail/i.test(el.textContent || ''));"
                                                        + " const error = document.querySelector("
                                                        + "'.error, .invalid, .text-danger, .help-block, "
                                                        + ".field-validation-error, [aria-invalid=\"true\"]');"
                                                        + " return success || (error && error.offsetParent !== null);"
                                                        + "}",
                                        null,
                                        new Page.WaitForFunctionOptions().setTimeout(60_000));
                } catch (RuntimeException e) {
                        System.out.println("Campaign update is still processing after 60 seconds.");
                }
        }
}
