package com.meera.tests;

import com.meera.config.Config;
import com.meera.utils.ExcelDataReader;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Generates a leads CSV and uploads it to a campaign.
 * Port of {@code tests/uploadCampaignLeads.spec.js}.
 */
public class UploadCampaignLeadsTests extends AuthenticatedTest {

    private static String generatePhoneNumber() {
        int area = ThreadLocalRandom.current().nextInt(800) + 200;
        int prefix = ThreadLocalRandom.current().nextInt(900) + 100;
        int line = ThreadLocalRandom.current().nextInt(9000) + 1000;
        return "+1" + area + prefix + line;
    }

    private static String randomHex(int bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes; i++) {
            sb.append(String.format("%02x", ThreadLocalRandom.current().nextInt(256)));
        }
        return sb.toString();
    }

    private static String generateCsv(int rowCount) {
        String campaignId = "1915";
        String source = "Google";
        String header =
                "\"campaign_id\",\"first_name\",\"last_name\",\"country_code\","
                + "\"state_code\",\"mobile_number\",\"external_system_id\",\"source\"";
        StringBuilder rows = new StringBuilder(header);
        for (int i = 0; i < rowCount; i++) {
            String externalId = "EXT-" + System.currentTimeMillis() + "-" + i + "-" + randomHex(4);
            String phone = generatePhoneNumber();
            rows.append("\r\n")
                .append("\"").append(campaignId).append("\",")
                .append("\"Mark\",\"Denial\",\"IN\",\"GJ\",")
                .append("\"").append(phone).append("\",")
                .append("\"").append(externalId).append("\",")
                .append("\"").append(source).append("\"");
        }
        return rows.toString();
    }

    @DataProvider(name = "editCampaignData")
    public Object[][] editCampaignData() {
        return ExcelDataReader.getTestDataAsProvider(Config.CAMPAIGN_DATA, "EditCampaign");
    }

    @Test(dataProvider = "editCampaignData", timeOut = 300_000)
    public void uploadCampaignLeads(Map<String, String> data) throws Exception {
        System.out.println("Running test case: " + data.get("testCase") + " - Upload Campaign Leads");
        attachPageEventLoggers(page);

        // ---- Build the CSV -------------------------------------------------
        String csvContent = generateCsv(30);
        Path tempDir = Paths.get("test-results", "temp");
        Files.createDirectories(tempDir);
        Path csvPath = tempDir.resolve("upload-leads-" + System.currentTimeMillis() + ".csv");
        Files.write(csvPath, csvContent.getBytes(StandardCharsets.UTF_8));
        System.out.println("CSV written to: " + csvPath.toAbsolutePath());

        // ---- Step 1: campaign reports -------------------------------------
        System.out.println("Step 1: Navigating to campaign reports...");
        page.navigate(Config.CAMPAIGN_REPORTS_URL, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60000));
        System.out.println("Step 1 done. URL: " + page.url());

        // ---- Step 2: search -----------------------------------------------
        System.out.println("Step 2: Searching for campaign...");
        page.locator("input[type='search']").fill(data.get("campaignName"));
        page.waitForTimeout(2000);
        page.keyboard().press("Enter");
        page.waitForTimeout(2000);
        System.out.println("Step 2 done.");

        // ---- Step 3: open campaign ----------------------------------------
        System.out.println("Step 3: Clicking campaign name...");
        page.locator("text=\"" + data.get("campaignName") + "\"").first().click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(2000);
        System.out.println("Step 3 done. URL: " + page.url());

        // ---- Step 4: go to upload_leads page ------------------------------
        System.out.println("Step 4: Navigating directly to upload_leads page...");
        String campaignId = "1915";
        page.navigate(Config.BASE_URL + "campaign/upload_leads/" + campaignId,
                new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60000));
        System.out.println("Upload leads page loaded. URL: " + page.url());

        // ---- Attach the file ----------------------------------------------
        // The real <input type="file"> is almost always hidden by CSS and triggered
        // by a styled button/label on top of it. setInputFiles works on hidden
        // inputs, so DO NOT gate on visibility.
        Locator fileInput = page.locator("input[type='file']").first();
        fileInput.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.ATTACHED).setTimeout(15000));
        fileInput.setInputFiles(csvPath);
        System.out.println("File attached to the first file input (hidden or not).");

        page.waitForTimeout(1000);

        // ---- Click the Upload / Submit button -----------------------------
        Locator uploadBtn = page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                        new Page.GetByRoleOptions()
                                .setName(Pattern.compile("upload|submit", Pattern.CASE_INSENSITIVE)))
                .or(page.locator("input[type='submit']"))
                .first();

        if (uploadBtn.count() > 0) {
            uploadBtn.click();
            System.out.println("Clicked the Upload/Submit button.");
        } else {
            // Fallback: scan everything and log it so you can see the real labels.
            Locator upButtons = page.locator("button, input[type='submit']");
            int upBtnCount = upButtons.count();
            System.out.println("No upload button matched by role; scanning all buttons: " + upBtnCount);
            for (int i = 0; i < upBtnCount; i++) {
                String text = upButtons.nth(i).textContent();
                text = text == null ? "" : text.trim();
                boolean vis = upButtons.nth(i).isVisible();
                System.out.println("  Button[" + i + "]: visible=" + vis + " text=\""
                        + text.substring(0, Math.min(60, text.length())) + "\"");
                if (vis && Pattern.compile("upload|submit", Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                    upButtons.nth(i).click();
                    System.out.println("Clicked button[" + i + "]: \""
                            + text.substring(0, Math.min(60, text.length())) + "\"");
                    break;
                }
            }
        }

        // ---- Step 5: verify result ----------------------------------------
        System.out.println("Step 5: Checking result...");
        System.out.println("URL: " + page.url());

        Locator successLocator = page.locator("text=Successfully Uploaded")
                .or(page.locator("text=Uploaded Successfully"))
                .or(page.locator("text=/upload(ed)?/i"))
                .or(page.locator(".swal2-success"))
                .or(page.locator(".swal2-popup"));

        try {
            assertThat(successLocator.first()).isVisible(
                    new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions()
                            .setTimeout(20000));
            System.out.println("Step 5 done. Upload confirmed.");
        } catch (AssertionError err) {
            // Capture what the page actually shows so the real selector is obvious.
            Path shotPath = tempDir.resolve("upload-fail-" + System.currentTimeMillis() + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(shotPath).setFullPage(true));
            System.out.println("FAILURE SCREENSHOT: " + shotPath.toAbsolutePath());
            String bodyText = page.locator("body").innerText();
            bodyText = bodyText.substring(0, Math.min(2000, bodyText.length()));
            System.out.println("VISIBLE PAGE TEXT (first 2000 chars):\n" + bodyText);
            throw err;
        }
    }
}
