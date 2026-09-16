package com.meera.tests;

import com.meera.pages.StateCompliancePage;
import org.testng.annotations.Test;

/**
 * State-compliance verification. Port of {@code tests/stateComplianceTests.spec.js}.
 */
public class StateComplianceTests extends AuthenticatedTest {

    @Test(timeOut = 300_000)
    public void verifyStateComplianceForNY() {
        StateCompliancePage stateCompliancePage = new StateCompliancePage(page);

        stateCompliancePage.gotoHome();
        stateCompliancePage.openStateCompliancePage();
        stateCompliancePage.searchState("Ny");
        stateCompliancePage.verifyStateComplianceData();
        stateCompliancePage.clickStartTimeCell();
        stateCompliancePage.clickEndTimeCell();
    }
}
