package com.meera.tests.nlp.assistant;

import com.meera.config.Config;
import com.meera.pages.AssistantPage;
import com.meera.tests.AuthenticatedTest;
import com.meera.utils.ExcelDataReader;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Edit-assistant tests. Port of {@code tests/NLP/Assistant/editAssistantTests.spec.js}.
 */
public class EditAssistantTests extends AuthenticatedTest {

    @DataProvider(name = "editAssistantData")
    public Object[][] editAssistantData() {
        return ExcelDataReader.getTestDataAsProvider(Config.ASSISTANT_DATA, "EditAssistant");
    }

    @Test(dataProvider = "editAssistantData", timeOut = 300_000)
    public void editAssistant(Map<String, String> data) {
        System.out.println("Running test case: " + data.get("testCase"));
        AssistantPage assistantPage = new AssistantPage(page);
        attachPageEventLoggers(page);

        assistantPage.goTo(Config.ASSISTANTS_URL);
        assistantPage.searchAssistant(data.get("searchName"));
        assistantPage.clickEditAssistant(data.get("searchName"));
        assistantPage.updateAssistantName(data.get("updatedName"));
        assistantPage.clickUpdateAssistant();
        assistantPage.verifyAssistantUpdated();
    }
}
