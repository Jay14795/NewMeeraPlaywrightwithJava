package com.meera.tests;

import com.meera.config.Config;
import com.meera.utils.ExcelDataReader;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Uploads the workspace leads CSV to the selected campaign and verifies it
 * from the campaign's uploads view.
 * Port of {@code tests/uploadCampaignLeads.spec.js}.
 */
public class UploadCampaignLeadsTests extends AuthenticatedTest {

    @DataProvider(name = "editCampaignData")
    public Object[][] editCampaignData() {
        return ExcelDataReader.getTestDataAsProvider(Config.CAMPAIGN_DATA, "EditCampaign");
    }

    @Test(dataProvider = "editCampaignData", timeOut = 300_000)
    public void uploadCampaignLeads(Map<String, String> data) throws Exception {
        System.out.println("Running test case: " + data.get("testCase") + " - Upload Campaign Leads");
        attachPageEventLoggers(page);

        Path tempDir = Paths.get("test-results", "temp");
        Files.createDirectories(tempDir);
        Path templatePath = Paths.get("test-data", "CampaignLeads.csv");
        if (!Files.exists(templatePath)) {
            throw new IllegalStateException("Leads template not found: " + templatePath.toAbsolutePath());
        }

        // The campaign id must match the campaign opened below.
        Path csvPath = tempDir.resolve("upload-leads-" + System.currentTimeMillis() + ".csv");
        String csvContent = limitCsvRows(
                Files.readString(templatePath, StandardCharsets.UTF_8), 50);
        csvContent = replacePhoneNumbers(csvContent);
        Files.writeString(csvPath, csvContent, StandardCharsets.UTF_8);
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
        String campaignUrl = page.url();
        String campaignId = extractCampaignId(campaignUrl);
        System.out.println("Step 3 done. Campaign URL: " + campaignUrl + ", campaign ID: " + campaignId);

        // ---- Step 4: fill the Leads Upload form on the campaign page -------
        System.out.println("Step 4: Preparing the Leads Upload form...");
        csvContent = replaceCampaignId(csvContent, campaignId);
        Files.writeString(csvPath, csvContent, StandardCharsets.UTF_8);
        Locator uploadForm = page.locator("#upload_lead_form2");
        uploadForm.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(30000));
        Locator fileInput = uploadForm.locator("input.uploadleads[name='lead_file']");
        fileInput.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.ATTACHED).setTimeout(60000));
        fileInput.setInputFiles(csvPath);
        System.out.println("Leads file selected: " + csvPath.toAbsolutePath());

        // ---- Step 5: submit and wait for the 2-3 minute upload -------------
        Locator uploadBtn = uploadForm.locator("#upload_lead2");
        uploadBtn.scrollIntoViewIfNeeded();
        uploadBtn.click(new Locator.ClickOptions().setTimeout(15000));
        System.out.println("Clicked Leads Upload.");

        // ---- Step 6: open uploads and verify the uploaded lead ------------
        Page uploadsPage = openUploadsView();
        refreshUploadsPageEveryThirtySeconds(uploadsPage);
        Locator latestUpload = uploadsPage.locator("table tbody tr").first();
        Locator completeStatus = latestUpload.locator("td").filter(
                new Locator.FilterOptions().setHasText(
                        Pattern.compile("^\\s*Complete\\s*$", Pattern.CASE_INSENSITIVE)));
        assertThat(completeStatus).isVisible(
                new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions()
                        .setTimeout(60_000));
        assertThat(latestUpload).containsText("50 (100%)",
                new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions()
                        .setTimeout(60_000));
        System.out.println("Latest upload is complete: 50 (100%)");
    }

    private void refreshUploadsPageEveryThirtySeconds(Page uploadsPage) {
        System.out.println("Waiting 2 minutes for lead processing and refreshing every 30 seconds...");
        for (int refreshNumber = 1; refreshNumber <= 4; refreshNumber++) {
            uploadsPage.waitForTimeout(30_000);
            uploadsPage.reload(new Page.ReloadOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(60_000));
            System.out.println("Uploads page refreshed (" + refreshNumber + "/4): "
                    + uploadsPage.url());
        }
    }

    private void waitForUploadResult() {
        try {
            page.waitForFunction(
                    "() => {"
                            + " const success = document.querySelector("
                            + "'.swal2-popup.swal2-icon-success, .swal2-success');"
                            + " const successText = /successfully uploaded|upload completed/i.test("
                            + "document.body.innerText || '');"
                            + " const error = document.querySelector("
                            + "'.error, .invalid, .text-danger, .help-block, "
                            + ".field-validation-error, [aria-invalid=\"true\"]');"
                            + " return (success && success.offsetParent !== null) || successText"
                            + " || (error && error.offsetParent !== null);"
                            + "}",
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(180_000));
        } catch (RuntimeException e) {
            System.out.println("Lead upload is still processing after 180 seconds.");
        }
    }

    private Page openUploadsView() {
        Locator uploadsControl = page.getByRole(com.microsoft.playwright.options.AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName(Pattern.compile("uploads?", Pattern.CASE_INSENSITIVE)))
                .or(page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                        new Page.GetByRoleOptions()
                                .setName(Pattern.compile("uploads?", Pattern.CASE_INSENSITIVE))))
                .first();
        uploadsControl.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(30000));
        Page uploadsPage = page.waitForPopup(uploadsControl::click);
        uploadsPage.waitForLoadState(LoadState.DOMCONTENTLOADED);
        System.out.println("Uploads view opened. URL: " + uploadsPage.url());
        return uploadsPage;
    }

    private static String limitCsvRows(String csvContent, int maximumRows) {
        String[] lines = csvContent.split("\\R");
        if (lines.length <= maximumRows + 1) {
            return csvContent;
        }
        StringBuilder limited = new StringBuilder(lines[0]);
        for (int i = 1; i <= maximumRows; i++) {
            limited.append(System.lineSeparator()).append(lines[i]);
        }
        return limited.toString();
    }

    private static String extractCampaignId(String campaignUrl) {
        Matcher matcher = Pattern.compile("/campaign/edit(?:7)?/(\\d+)(?:[/?#]|$)")
                .matcher(campaignUrl);
        if (!matcher.find()) {
            throw new IllegalStateException("Could not extract campaign ID from URL: " + campaignUrl);
        }
        return matcher.group(1);
    }

    private static String replaceCampaignId(String csvContent, String campaignId) {
        String[] lines = csvContent.split("\\R", -1);
        if (lines.length < 2 || !lines[0].toLowerCase().contains("campaign_id")) {
            throw new IllegalArgumentException("Leads CSV must contain a campaign_id header and at least one row.");
        }
        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].trim().isEmpty()) {
                int comma = lines[i].indexOf(',');
                if (comma < 1) {
                    throw new IllegalArgumentException("Invalid leads CSV row: " + lines[i]);
                }
                lines[i] = campaignId + lines[i].substring(comma);
            }
        }
        return String.join(System.lineSeparator(), lines);
    }

    private static String replacePhoneNumbers(String csvContent) {
        String[] lines = csvContent.split("\\R", -1);
        String[] headers = lines[0].split(",", -1);
        int phoneIndex = findColumnIndex(headers, "mobile_number");
        if (phoneIndex < 0) {
            throw new IllegalArgumentException("Leads CSV is missing mobile_number.");
        }

        SecureRandom random = new SecureRandom();
        Set<String> generatedNumbers = new HashSet<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) {
                continue;
            }
            String[] values = lines[i].split(",", -1);
            if (values.length <= phoneIndex) {
                throw new IllegalArgumentException("Invalid leads CSV row: " + lines[i]);
            }
            String phone;
            do {
                phone = "999" + String.format("%07d", random.nextInt(10_000_000));
            } while (!generatedNumbers.add(phone));
            values[phoneIndex] = phone;
            lines[i] = String.join(",", values);
        }
        return String.join(System.lineSeparator(), lines);
    }

    private static int findColumnIndex(String[] headers, String expectedHeader) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].replace("\"", "").trim().equalsIgnoreCase(expectedHeader)) {
                return i;
            }
        }
        return -1;
    }
}
