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

import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Edit-campaign (grounded RAG + business hours + update) flow.
 * Port of {@code tests/editCampaignTests.spec.js}.
 */
public class EditCampaignTests extends AuthenticatedTest {

        @DataProvider(name = "editCampaignData")
        public Object[][] editCampaignData() {
                return ExcelDataReader.getTestDataAsProvider(
                                Config.CAMPAIGN_DATA,
                                "EditCampaign");
        }

        @Test(dataProvider = "editCampaignData", timeOut = 600_000 // 10 minutes
        )
        public void editCampaign(Map<String, String> data) {

                System.out.println(
                                "Running test case: " + data.get("testCase"));

                attachPageEventLoggers(page);

                String campaignName = data.get("campaignName");

                // ---------------------------------------------------------
                // Step 2: Navigate to Campaign Reports page
                //
                // NOTE: We intentionally do NOT wait for NETWORKIDLE here.
                // The reports dashboard keeps background traffic alive
                // (polling / websockets / analytics), so the network never
                // truly idles and the navigation would time out at 30s.
                // Instead we wait for DOMCONTENTLOADED and then for a
                // concrete element that proves the page is ready.
                // ---------------------------------------------------------
                page.navigate(
                                Config.CAMPAIGN_REPORTS_URL,
                                new Page.NavigateOptions()
                                                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                                                .setTimeout(60_000));

                Locator searchBox = page.locator("input[type='search']");

                searchBox.waitFor(
                                new Locator.WaitForOptions()
                                                .setState(WaitForSelectorState.VISIBLE)
                                                .setTimeout(60_000));

                // ---------------------------------------------------------
                // Step 3: Search Campaign
                // ---------------------------------------------------------
                searchBox.fill(campaignName);

                page.waitForTimeout(2000);

                page.keyboard().press("Enter");

                page.waitForTimeout(2000);

                // ---------------------------------------------------------
                // Step 4: Open Campaign
                // ---------------------------------------------------------
                Locator campaignRow = page.locator(
                                "text=\"" + campaignName + "\"")
                                .first();

                campaignRow.waitFor(
                                new Locator.WaitForOptions()
                                                .setState(WaitForSelectorState.VISIBLE)
                                                .setTimeout(30_000));

                campaignRow.click();

                page.waitForLoadState(LoadState.DOMCONTENTLOADED);

                // Open Grounded RAG tab
                Locator groundedTab = page.locator("a[href=\"#groundedTab\"]");

                groundedTab.waitFor(
                                new Locator.WaitForOptions()
                                                .setState(WaitForSelectorState.VISIBLE)
                                                .setTimeout(30_000));

                groundedTab.click();

                page.waitForTimeout(1500);

                // ---------------------------------------------------------
                // Click "1. Create Knowledge Base" tab
                // ---------------------------------------------------------
                page.locator("a[href=\"#kt_tab_pane_5_1\"]")
                                .click();

                page.waitForTimeout(1000);

                // ---------------------------------------------------------
                // Step 5: Enter Website URL
                // ---------------------------------------------------------
                page.locator("text=Website/URL Knowledge Source")
                                .waitFor(
                                                new Locator.WaitForOptions()
                                                                .setState(WaitForSelectorState.VISIBLE)
                                                                .setTimeout(10_000));

                page.getByPlaceholder(
                                "Enter URL (e.g., https://example.com/page)")
                                .fill("https://cameobeautyacademy.com");

                page.waitForTimeout(500);

                // ---------------------------------------------------------
                // Step 6: Click Get URLs
                // ---------------------------------------------------------
                page.getByRole(
                                AriaRole.BUTTON,
                                new Page.GetByRoleOptions()
                                                .setName("Get URLs"))
                                .click();

                // Wait until Select All button becomes visible
                page.getByRole(
                                AriaRole.BUTTON,
                                new Page.GetByRoleOptions()
                                                .setName(
                                                                Pattern.compile(
                                                                                "select all",
                                                                                Pattern.CASE_INSENSITIVE)))
                                .waitFor(
                                                new Locator.WaitForOptions()
                                                                .setState(WaitForSelectorState.VISIBLE)
                                                                .setTimeout(60_000));

                page.waitForTimeout(1000);

                // ---------------------------------------------------------
                // Step 7: Select All URLs
                // ---------------------------------------------------------
                page.getByRole(
                                AriaRole.BUTTON,
                                new Page.GetByRoleOptions()
                                                .setName(
                                                                Pattern.compile(
                                                                                "select all",
                                                                                Pattern.CASE_INSENSITIVE)))
                                .click();

                page.waitForTimeout(500);

                // ---------------------------------------------------------
                // Step 8: Submit Knowledge Base
                // ---------------------------------------------------------
                page.getByRole(
                                AriaRole.BUTTON,
                                new Page.GetByRoleOptions()
                                                .setName(
                                                                Pattern.compile(
                                                                                "submit",
                                                                                Pattern.CASE_INSENSITIVE)))
                                .click();

                /*
                 * This method:
                 *
                 * 1. Waits for "View Uploaded Knowledge Base"
                 * 2. Clicks it
                 * 3. Waits EXACTLY 3 minutes
                 * 4. Verifies upload reached 100%
                 */
                waitForKnowledgeBaseUpload();

                // ---------------------------------------------------------
                // Step 9: Open tab after KB processing
                // ---------------------------------------------------------
                page.locator("a[href=\"#tab_6_6\"]")
                                .click();

                page.waitForTimeout(1000);

                // ---------------------------------------------------------
                // Turn Campaign Mode ON
                // ---------------------------------------------------------
                page.locator("label:has(input[name=\"mode\"][value=\"1\"])")
                                .click();

                page.waitForTimeout(500);

                // ---------------------------------------------------------
                // Confirmation Dialog
                // "Are you sure you want to turn On the campaign?"
                // ---------------------------------------------------------
                page.getByRole(
                                AriaRole.BUTTON,
                                new Page.GetByRoleOptions()
                                                .setName("Yes"))
                                .first()
                                .click();

                page.waitForTimeout(1000);

                // ---------------------------------------------------------
                // Step 10: Click Update
                // ---------------------------------------------------------
                page.getByRole(
                                AriaRole.BUTTON,
                                new Page.GetByRoleOptions()
                                                .setName("Update"))
                                .first()
                                .click();

                // ---------------------------------------------------------
                // Step 11: Wait until the "Successfully Updated" message
                // appears (fails early only on a *real* validation error).
                // ---------------------------------------------------------
                verifyUpdateSucceeded();

                System.out.println(
                                "Campaign successfully updated.");
        }

        /**
         * Wait for "View Uploaded Knowledge Base",
         * click it,
         * wait exactly 3 minutes,
         * and then verify upload is at 100%.
         */
        private void waitForKnowledgeBaseUpload() {

                Locator uploadedKnowledgeBase = page.locator(
                                "a, button, input[type='button'], input[type='submit']")
                                .filter(
                                                new Locator.FilterOptions()
                                                                .setHasText(
                                                                                Pattern.compile(
                                                                                                "view\\s+uploaded\\s+knowledge\\s*base",
                                                                                                Pattern.CASE_INSENSITIVE)))
                                .or(
                                                page.getByText(
                                                                Pattern.compile(
                                                                                "view\\s+uploaded\\s+knowledge\\s*base",
                                                                                Pattern.CASE_INSENSITIVE)))
                                .first();

                // Wait until button/link is visible
                uploadedKnowledgeBase.waitFor(
                                new Locator.WaitForOptions()
                                                .setState(WaitForSelectorState.VISIBLE)
                                                .setTimeout(60_000));

                // Scroll to element
                uploadedKnowledgeBase.scrollIntoViewIfNeeded();

                System.out.println(
                                "Clicking View Uploaded Knowledge Base...");

                // Click View Uploaded Knowledge Base
                uploadedKnowledgeBase.click();

                System.out.println(
                                "Waiting for Knowledge Base status to reach 100%...");

                Locator statusElements = page.locator(
                                "table tbody tr, [role='row'], .progress-bar, "
                                                + "[role='progressbar'], [aria-valuenow], [value]");
                long deadline = System.currentTimeMillis() + 180_000;
                boolean uploadCompleted = false;
                while (System.currentTimeMillis() < deadline) {
                        for (int i = 0; i < statusElements.count(); i++) {
                                Locator statusElement = statusElements.nth(i);
                                if (!statusElement.isVisible()) {
                                        continue;
                                }
                                String text = statusElement.textContent();
                                String ariaValue = statusElement.getAttribute("aria-valuenow");
                                String value = statusElement.getAttribute("value");
                                if (contains100Percent(text)
                                                || "100".equals(ariaValue)
                                                || "100".equals(value)) {
                                        uploadCompleted = true;
                                        break;
                                }
                        }
                        if (uploadCompleted) {
                                break;
                        }
                        page.waitForTimeout(2_000);
                }
                if (!uploadCompleted) {
                        throw new AssertionError(
                                        "Knowledge Base upload did not reach 100% within 3 minutes.");
                }

                System.out.println(
                                "Knowledge Base upload completed: 100%");
        }

        private boolean contains100Percent(String text) {
                return text != null && Pattern.compile(
                                "(?:^|\\s)100\\s*%(?:\\s|$)",
                                Pattern.CASE_INSENSITIVE).matcher(text.trim()).find();
        }

        /**
         * Waits until the campaign "Successfully Updated the detail" message
         * appears.
         *
         * <p>
         * It keeps polling for the success message and only fails early
         * if a <b>genuine</b> validation error is shown. Required-field
         * markers such as a lone red asterisk ("*") are ignored, because
         * they are always present in the form and are not real errors — a
         * candidate error must contain at least a few letters to count.
         *
         * <ul>
         * <li>Success message appears -> pass.</li>
         * <li>Real validation error -> fail with the error text.</li>
         * <li>Neither within timeout -> screenshot + fail.</li>
         * </ul>
         */
        private void verifyUpdateSucceeded() {

                Object outcomeObj;

                try {

                        outcomeObj = page.waitForFunction(
                                        "() => {"
                                                        + " const isVisible = el => el && el.offsetParent !== null;"

                                                        // 1) Success wins immediately.
                                                        + " const success = "
                                                        + "Array.from(document.querySelectorAll('body *'))"
                                                        + ".some(el => isVisible(el)"
                                                        + " && /Successfully\\s+Updated\\s+the\\s+detail/i"
                                                        + ".test(el.textContent || ''));"
                                                        + " if (success) return 'SUCCESS';"

                                                        // 2) Only a REAL error counts. Skip
                                                        // required-field markers like '*':
                                                        // the text must have >= 3 letters.
                                                        + " const errEl = Array.from(document.querySelectorAll("
                                                        + "'.error, "
                                                        + ".invalid, "
                                                        + ".text-danger, "
                                                        + ".field-validation-error, "
                                                        + ".toast-error, "
                                                        + ".alert-danger, "
                                                        + "[aria-invalid=\"true\"]'"
                                                        + ")).find(el => {"
                                                        + "   if (!isVisible(el)) return false;"
                                                        + "   const t = (el.textContent || '').trim();"
                                                        + "   const letters = (t.match(/[A-Za-z]/g) || []).length;"
                                                        + "   return letters >= 3;"
                                                        + " });"
                                                        + " if (errEl) return 'ERROR:' + errEl.textContent.trim();"

                                                        // 3) Nothing conclusive yet -> keep polling.
                                                        + " return false;"
                                                        + "}",

                                        null,

                                        new Page.WaitForFunctionOptions()
                                                        .setTimeout(120_000)); // wait up to 2 min

                } catch (RuntimeException timeout) {

                        String shot = captureScreenshot("update-timeout");

                        throw new AssertionError(
                                        "Timed out after 120s waiting for the "
                                                        + "'Successfully Updated the detail' message "
                                                        + "(and no real validation error appeared). "
                                                        + "Screenshot: " + shot,
                                        timeout);
                }

                String outcome = String.valueOf(
                                outcomeObj == null ? "" : outcomeObj);

                if ("SUCCESS".equals(outcome)) {
                        System.out.println(
                                        "Update confirmed: success message displayed.");
                        return;
                }

                if (outcome.startsWith("ERROR:")) {

                        String message = outcome.substring("ERROR:".length());
                        String shot = captureScreenshot("update-validation-error");

                        throw new AssertionError(
                                        "Campaign update failed with a validation error: \""
                                                        + message + "\". Screenshot: " + shot);
                }

                String shot = captureScreenshot("update-unknown-state");

                throw new AssertionError(
                                "Update outcome could not be determined. "
                                                + "Screenshot: " + shot);
        }

        /**
         * Saves a full-page screenshot under target/screenshots and returns
         * the path (or a note if the capture itself failed).
         */
        private String captureScreenshot(String label) {

                try {

                        String path = "target/screenshots/"
                                        + label + "-"
                                        + System.currentTimeMillis()
                                        + ".png";

                        page.screenshot(
                                        new Page.ScreenshotOptions()
                                                        .setPath(Paths.get(path))
                                                        .setFullPage(true));

                        return path;

                } catch (RuntimeException e) {
                        return "(screenshot capture failed: "
                                        + e.getMessage() + ")";
                }
        }
}