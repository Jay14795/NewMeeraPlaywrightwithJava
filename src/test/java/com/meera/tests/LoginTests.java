package com.meera.tests;

import com.meera.config.Config;
import com.meera.pages.LoginPage;
import com.meera.utils.ExcelDataReader;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Data-driven login tests. Port of {@code tests/LoginTests.spec.js}.
 * Each row of the {@code LoginData} sheet becomes one test invocation.
 */
public class LoginTests extends BaseTest {

    @DataProvider(name = "loginData")
    public Object[][] loginData() {
        return ExcelDataReader.getTestDataAsProvider(Config.LOGIN_DATA, "LoginData");
    }

    @Test(dataProvider = "loginData")
    public void loginTest(Map<String, String> data) {
        System.out.println("Running test case: " + data.get("testCase"));
        LoginPage loginPage = new LoginPage(page);

        loginPage.goTo(Config.BASE_URL);
        System.out.println(page.title());
        loginPage.verifyTitle("Sign In");
        loginPage.login(data.get("email"), data.get("password"));

        if (isTruthy(data.get("expectedURL"))) {
            loginPage.verifyUrl(data.get("expectedURL"));
        }

        if (isTruthy(data.get("expectedError"))) {
            System.out.println(loginPage.getAlertText());
            loginPage.verifyAlertContains(data.get("expectedError"));
        }
    }

    private static boolean isTruthy(String s) {
        return s != null && !s.isEmpty();
    }
}
    