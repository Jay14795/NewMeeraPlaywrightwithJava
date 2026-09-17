package com.meera.tests;

import com.meera.config.Config;
import com.meera.pages.CampaignPage;
import com.meera.utils.ExcelDataReader;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Duplicate-campaign flow. Port of
 * {@code tests/duplicateCampaignTests.spec.js}.
 */
public class DuplicateCampaignTests extends AuthenticatedTest {

        @DataProvider(name = "duplicateCampaignData")
        public Object[][] duplicateCampaignData() {
                return ExcelDataReader.getTestDataAsProvider(Config.CAMPAIGN_DATA, "DuplicateCampaign");
        }

        @Test(dataProvider = "duplicateCampaignData", timeOut = 300_000)
        public void duplicateCampaign(Map<String, String> data) {
                System.out.println("Running test case: " + data.get("testCase"));
                String testCase = data.get("testCase");
                String campaignName = data.get("campaignName");

                List<Map<String, String>> numbersData = ExcelDataReader.getTestData(Config.CAMPAIGN_DATA, "Numbers");

                CampaignPage campaignPage = new CampaignPage(page);
                attachPageEventLoggers(page);

                // Step 1: Navigate to campaign reports
                page.navigate(Config.CAMPAIGN_REPORTS_URL,
                                new Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD));

                // Step 2: Search for the campaign
                page.locator("input[type='search']").fill(campaignName);
                page.waitForTimeout(2000);
                page.keyboard().press("Enter");
                page.waitForTimeout(2000);

                // Step 3: Click the Duplicate icon on the matching ACTIVE campaign row.
                // Deleted rows (class "campaign-deleted-row") only have a Restore button and
                // no Duplicate icon, so we exclude them and require the duplicate link to
                // exist.
                Locator campaignRow = page.locator("table tbody tr:not(.campaign-deleted-row)")
                                .filter(new Locator.FilterOptions().setHasText(campaignName))
                                .filter(new Locator.FilterOptions().setHas(page.locator("i.flaticon2-copy")));

                assertThat(campaignRow.first()).isVisible(
                                new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions()
                                                .setTimeout(15000));

                Locator duplicateLink = campaignRow.first().locator("a:has(i.flaticon2-copy)");
                duplicateLink.scrollIntoViewIfNeeded();
                duplicateLink.click();

                // Step 4: Wait for the campaign form to load
                page.waitForTimeout(5000);
                campaignPage.getCampaignNameInput().waitFor(new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.VISIBLE).setTimeout(20000));

                // Step 5: Update campaign name on General Settings tab
                campaignPage.fillCampaignName(data.get("newCampaignName"));
                page.waitForTimeout(500);

                // Step 6: Handle Numbers tab (same pattern as createCampaignTests)
                Map<String, String> numbersInfo = numbersData.stream()
                                .filter(n -> testCase != null && testCase.equals(n.get("testCase")))
                                .findFirst()
                                .orElse(null);
                if (numbersInfo != null) {
                        campaignPage.fillNumbersTab(numbersInfo);
                }

                // Step 7: Click submit button on duplicate page (don't wait for navigation)
                campaignPage.clickAddCampaignDuplicate();
                // Handle grounded rag review popup if it appears
                campaignPage.handleValidationPopup();
                page.waitForTimeout(2000);
                System.out.println("URL after validation: " + page.url());

                // Step 8: Verify campaign duplication succeeded
                campaignPage.verifyDuplicateCampaignCreated();
        }
}
