package com.meera.tests.nlp.assistant;

import com.meera.config.Config;
import com.meera.pages.AssistantPage;
import com.meera.tests.AuthenticatedTest;
import com.meera.utils.ExcelDataReader;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Delete-assistant tests. Port of {@code tests/NLP/Assistant/deleteAssistantTests.spec.js}.
 */
public class DeleteAssistantTests extends AuthenticatedTest {

    @DataProvider(name = "deleteAssistantData")
    public Object[][] deleteAssistantData() {
        return ExcelDataReader.getTestDataAsProvider(Config.ASSISTANT_DATA, "DeleteAssistant");
    }

    @Test(dataProvider = "deleteAssistantData", timeOut = 300_000)
    public void deleteAssistant(Map<String, String> data) {
        System.out.println("Running test case: " + data.get("testCase"));
        AssistantPage assistantPage = new AssistantPage(page);
        attachPageEventLoggers(page);

        assistantPage.goTo(Config.ASSISTANTS_URL);
        assistantPage.searchAssistant(data.get("assistantName"));
        assistantPage.clickDeleteAssistant(data.get("assistantName"));
        assistantPage.handleDeleteConfirmation();
        assistantPage.verifyAssistantDeleted();
    }
}
