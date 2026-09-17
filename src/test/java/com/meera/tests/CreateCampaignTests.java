package com.meera.tests;

import com.meera.config.Config;
import com.meera.pages.CampaignPage;
import com.meera.utils.ExcelDataReader;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Create-campaign end-to-end tests. Port of
 * {@code tests/createCampaignTests.spec.js}.
 * Iterates the {@code GeneralSettings} sheet, joining the per-tab sheets by
 * testCase.
 *
 * <p>
 * Before running, validates the stored auth token. If the token is still valid,
 * skips re-login. Otherwise performs a fresh login and proceeds.
 * </p>
 */
public class CreateCampaignTests extends AuthenticatedTest {

    @DataProvider(name = "campaignData")
    public Object[][] campaignData() {
        return ExcelDataReader.getTestDataAsProvider(Config.CAMPAIGN_DATA, "GeneralSettings");
    }

    @Test(dataProvider = "campaignData", timeOut = 300_000)
    public void createCampaign(Map<String, String> data) {
        String testCase = data.get("testCase");
        System.out.println("Running test case: " + testCase);

        List<Map<String, String>> numbersData = ExcelDataReader.getTestData(Config.CAMPAIGN_DATA, "Numbers");
        List<Map<String, String>> businessHrsData = ExcelDataReader.getTestData(Config.CAMPAIGN_DATA, "BusinessHrs");
        List<Map<String, String>> sysInitMessagesData = ExcelDataReader.getTestData(Config.CAMPAIGN_DATA,
                "SystemInitiatedMessages");
        List<Map<String, String>> callSettingsData = ExcelDataReader.getTestData(Config.CAMPAIGN_DATA, "CallSettings");
        List<Map<String, String>> transferSettingsData = ExcelDataReader.getTestData(Config.CAMPAIGN_DATA,
                "TransferSettings");

        CampaignPage campaignPage = new CampaignPage(page);
        attachPageEventLoggers(page);

        campaignPage.navigateToCampaignCreate(Config.CAMPAIGN_CREATE_URL);
        campaignPage.fillGeneralSettings(data);

        Map<String, String> numbersInfo = findByTestCase(numbersData, testCase);
        if (numbersInfo != null) {
            campaignPage.fillNumbersTab(numbersInfo);
        }

        Map<String, String> bizHrsInfo = findByTestCase(businessHrsData, testCase);
        if (bizHrsInfo != null) {
            campaignPage.fillBusinessHrs(bizHrsInfo);
        }

        Map<String, String> sysMsgInfo = findByTestCase(sysInitMessagesData, testCase);
        if (sysMsgInfo != null) {
            campaignPage.fillSystemInitiatedMessages(sysMsgInfo);
        }

        Map<String, String> callInfo = findByTestCase(callSettingsData, testCase);
        if (callInfo != null) {
            campaignPage.fillCallSettings(callInfo);
        }

        Map<String, String> transferInfo = findByTestCase(transferSettingsData, testCase);
        if (transferInfo != null) {
            campaignPage.fillTransferSettings(transferInfo);
        }

        campaignPage.clickAddCampaign();
        campaignPage.handleValidationPopup();
        campaignPage.verifyCampaignCreated();
    }

    private static Map<String, String> findByTestCase(List<Map<String, String>> rows, String testCase) {
        return rows.stream()
                .filter(r -> testCase != null && testCase.equals(r.get("testCase")))
                .findFirst()
                .orElse(null);
    }
}
