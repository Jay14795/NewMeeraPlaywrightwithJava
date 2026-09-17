package com.meera.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;

import java.nio.file.Paths;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Campaign create/edit page object. Port of {@code pages/CampaignPage.js}.
 */
public class CampaignPage {

    private final Page page;

    private final Locator accountNameInput;
    private final Locator accountNameDropdown;
    private final Locator brandNameInput;
    private final Locator campaignNameInput;
    private final Locator avatarNameInput;
    private final Locator campaignSkillType;
    private final Locator industrySelect;
    private final Locator subIndustrySelect;
    private final Locator useCaseSelect;
    private final Locator skillSelect;
    private final Locator campaignGoalInput;

    public CampaignPage(Page page) {
        this.page = page;
        this.accountNameInput = page.locator("#client_name");
        this.accountNameDropdown = page.locator(".typeahead.dropdown-menu li a").first();
        this.brandNameInput = page.locator("#university");
        this.campaignNameInput = page.locator("#campaign_n");
        this.avatarNameInput = page.locator("#avatarName");
        this.campaignSkillType = page.locator("#service_type");
        this.industrySelect = page.locator("#industryCampaign");
        this.subIndustrySelect = page.locator("#sub_industry_campaign");
        this.useCaseSelect = page.locator("#use_case_campaign");
        this.skillSelect = page.locator("#skillCampaign");
        this.campaignGoalInput = page.locator("#campaign_goal_description");
    }

    /** Exposed for the duplicate-campaign flow (waits for the form to load). */
    public Locator getCampaignNameInput() {
        return campaignNameInput;
    }

    private static boolean isTruthy(String s) {
        return s != null && !s.isEmpty();
    }

    /**
     * Dismisses a SweetAlert2 modal if one is currently shown (blocks pointer
     * events otherwise).
     */
    private void dismissSwal2IfPresent() {
        Locator swal = page.locator(".swal2-container.swal2-backdrop-show");
        if (swal.count() == 0) {
            return;
        }
        System.out.println("Dismissing SweetAlert2 modal...");
        try {
            if (swal.locator(".swal2-confirm").count() > 0) {
                swal.locator(".swal2-confirm").first().click();
            } else if (swal.locator(".swal2-close").count() > 0) {
                swal.locator(".swal2-close").first().click();
            } else if (swal.locator(".swal2-cancel").count() > 0) {
                swal.locator(".swal2-cancel").first().click();
            } else {
                page.keyboard().press("Escape");
            }
            swal.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN).setTimeout(5000));
        } catch (RuntimeException e) {
            System.out.println("Could not dismiss SweetAlert2 modal: " + e.getMessage());
        }
        page.waitForTimeout(300);
    }

    public void navigateToCampaignCreate(String url) {
        page.navigate(url,
                new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        try {
            page.waitForLoadState(LoadState.LOAD,
                    new Page.WaitForLoadStateOptions().setTimeout(60_000));
        } catch (RuntimeException e) {
            System.out.println("Page did not reach LOAD state; continuing with current DOM.");
        }
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(20_000));
        } catch (RuntimeException e) {
            System.out.println("Network did not reach idle on campaign create; continuing.");
        }
    }

    public void fillAccountName(String name) {
        accountNameInput.fill(name);
        page.waitForTimeout(1000);
        if (accountNameDropdown.isVisible()) {
            accountNameDropdown.click();
            page.waitForTimeout(500);
        }
    }

    public void fillBrandName(String name) {
        brandNameInput.fill(name);
    }

    public void fillCampaignName(String name) {
        campaignNameInput.fill(name);
    }

    public void fillAvatarName(String name) {
        avatarNameInput.fill(name);
    }

    public void selectCampaignSkillType(String label) {
        campaignSkillType.selectOption(new SelectOption().setLabel(label));
    }

    public void selectLeadProcessType(String text) {
        String value = text.toLowerCase().contains("real time") ? "live" : "aged";
        page.locator("label:has(input[name=\"type\"][value=\"" + value + "\"])").click();
    }

    public void selectInboundTextCampaign(String text) {
        String value = text.toLowerCase().contains("yes") ? "1" : "0";
        page.locator("label:has(input[name=\"inbound_type\"][value=\"" + value + "\"])").click();
    }

    public void selectMeeraAiBot(String text) {
        String value = text.toLowerCase().contains("on") ? "1" : "0";
        page.locator("label:has(input[name=\"bot_reply\"][value=\"" + value + "\"])").click();
    }

    public void selectCarrierLookup(String text) {
        String value = text.toLowerCase().contains("on") ? "1" : "0";
        page.locator("label:has(input[name=\"number_lookup\"][value=\"" + value + "\"])").click();
    }

    public void selectThrottleLeadPost(String text) {
        String value = text.toLowerCase().contains("on") ? "1" : "0";
        page.locator("label:has(input[name=\"throttle_post\"][value=\"" + value + "\"])").click();
    }

    public void selectResponseType(String text) {
        String value = text.toLowerCase().contains("grounded rag") ? "grounded_rag" : "intent_matcher";
        page.locator("label:has(input[name=\"gen_ai_response_type\"][value=\"" + value + "\"])").click();
    }

    private void selectOptionByTextRobust(Locator selectLocator, String idSelector, String label) {
        System.out.println("Selecting option in " + idSelector + " with label: '" + label + "'");
        Locator options = page.locator(idSelector + " option");
        try {
            options.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.ATTACHED).setTimeout(15000));
        } catch (RuntimeException e) {
            System.err.println("Options did not load under " + idSelector + ": " + e.getMessage());
        }

        int count = options.count();
        String matchedValue = null;
        for (int i = 0; i < count; i++) {
            Locator opt = options.nth(i);
            String optText = opt.textContent();
            String optVal = opt.getAttribute("value");
            if (optText != null && optText.trim().equalsIgnoreCase(label.trim())) {
                matchedValue = optVal;
                break;
            }
        }

        if (matchedValue != null) {
            selectLocator.selectOption(matchedValue);
        } else {
            // fallback to soft-match
            Locator option = page.locator(idSelector + " option")
                    .filter(new Locator.FilterOptions().setHasText(label))
                    .first();
            String val = option.getAttribute("value");
            selectLocator.selectOption(val);
        }
    }

    public void selectIndustry(String label) {
        selectOptionByTextRobust(industrySelect, "#industryCampaign", label);
    }

    public void selectSubIndustry(String label) {
        selectOptionByTextRobust(subIndustrySelect, "#sub_industry_campaign", label);
    }

    public void selectUseCase(String label) {
        selectOptionByTextRobust(useCaseSelect, "#use_case_campaign", label);
    }

    public void selectSkill(String label) {
        selectOptionByTextRobust(skillSelect, "#skillCampaign", label);
    }

    public void fillCampaignGoal(String text) {
        campaignGoalInput.fill(text);
    }

    /*
     * ---------------------------------------------------------------
     * Numbers tab
     * ---------------------------------------------------------------
     */

    public void clickNumbersTab() {
        dismissSwal2IfPresent();
        page.locator("a[href='#tab_6_2']").click();
        page.waitForTimeout(1500);
    }

    public void selectMessagingChannel(String channel) {
        String value = channel.equalsIgnoreCase("sms") ? "0" : "1";
        System.out.println("Selecting messaging channel: " + channel + " (value: " + value + ")");
        Locator label = page.locator(
                "label:has(input[name=\"twilio_whatsapp_service\"][value=\"" + value + "\"])");
        boolean isVisible = label.isVisible(new Locator.IsVisibleOptions().setTimeout(5000));
        System.out.println("Label visible for messaging channel: " + isVisible);
        label.click();
        page.waitForTimeout(500);
        System.out.println("Messaging channel selected");
    }

    public void selectMessageServiceIntegration(String mode) {
        String value;
        if (mode.toLowerCase().contains("existing")) {
            value = "2";
        } else if (mode.toLowerCase().contains("yes")) {
            value = "1";
        } else {
            value = "0";
        }
        page.locator("label:has(input[name=\"twilio_message_service\"][value=\"" + value + "\"])").click();
    }

    public void selectExistingMessagingService(String serviceName) {
        Locator row = page.locator(
                "#messagingServices tr:has(td p:has-text(\"" + serviceName + "\"))");
        row.locator("button:not([disabled]):text-is('Select')").click();
    }

    public void fillNumbersTab(Map<String, String> data) {
        System.out.println("Starting Numbers Tab fill with data: " + data);
        clickNumbersTab();
        System.out.println("Numbers tab clicked");

        if (isTruthy(data.get("messagingChannel"))) {
            System.out.println("Setting messaging channel to: " + data.get("messagingChannel"));
            selectMessagingChannel(data.get("messagingChannel"));
        }

        if (isTruthy(data.get("messageServiceIntegration"))) {
            System.out.println("Setting message service integration to: "
                    + data.get("messageServiceIntegration"));
            selectMessageServiceIntegration(data.get("messageServiceIntegration"));
        }

        if (isTruthy(data.get("existingServiceName"))) {
            System.out.println("Selecting existing service: " + data.get("existingServiceName"));
            String serviceName = data.get("existingServiceName");

            Locator anyRow = page.locator("#messagingServices tbody tr").first();
            boolean rowsAppeared = false;
            try {
                anyRow.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE).setTimeout(60_000));
                rowsAppeared = true;
            } catch (RuntimeException e) {
                System.out.println("No rows appeared under #messagingServices within 60s.");
            }

            if (rowsAppeared) {
                selectExistingMessagingService(serviceName);
            } else {
                int rowCount = page.locator("#messagingServices tbody tr").count();
                if (rowCount == 0) {
                    System.out.println("WARNING: Messaging services table is empty (0 rows). "
                            + "Service '" + serviceName + "' is not available in this environment. "
                            + "Skipping service selection.");
                } else {
                    throw new RuntimeException(
                            "Existing messaging service '" + serviceName + "' was not found on the page.");
                }
            }
        }

        System.out.println("Numbers tab filled completely");
    }

    /*
     * ---------------------------------------------------------------
     * Business hours tab
     * ---------------------------------------------------------------
     */

    public void clickBusinessHrsTab() {
        dismissSwal2IfPresent();
        page.locator("a[href='#tab_6_6']").click();
        page.waitForTimeout(1500);
    }

    public void selectTimezone(String timezone) {
        dismissSwal2IfPresent();
        page.locator("select[name='timezone_campaign']").selectOption(timezone);
        page.waitForTimeout(500);
    }

    public void setDaySchedule(String day, String openTime, String closeTime) {
        dismissSwal2IfPresent();
        String dayLower = day.toLowerCase();
        Locator dayToggle = page.locator("input." + dayLower + "_open_close_checked");
        try {
            dayToggle.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.ATTACHED).setTimeout(10_000));
            if (!dayToggle.isChecked()) {
                dayToggle.evaluate(
                        "(el) => { if (!el.checked) { el.checked = true; "
                                + "el.dispatchEvent(new Event('click', { bubbles: true })); "
                                + "el.dispatchEvent(new Event('change', { bubbles: true })); } }");
            }
        } catch (RuntimeException e) {
            System.out.println("Failed to toggle day '"
                    + day + "' via JS: " + e.getMessage());
            dayToggle.click(new Locator.ClickOptions().setForce(true));
        }
        page.waitForTimeout(200);
        if (isTruthy(openTime)) {
            page.locator("input." + dayLower + "_open_time")
                    .fill(openTime, new Locator.FillOptions().setForce(true));
        }
        if (isTruthy(closeTime)) {
            page.locator("input." + dayLower + "_close_time")
                    .fill(closeTime, new Locator.FillOptions().setForce(true));
        }
        page.waitForTimeout(200);
    }

    public void fillBusinessHrs(Map<String, String> data) {
        clickBusinessHrsTab();
        if (isTruthy(data.get("timezone"))) {
            selectTimezone(data.get("timezone"));
        }
        if (isTruthy(data.get("businessDays")) && isTruthy(data.get("openTime"))
                && isTruthy(data.get("closeTime"))) {
            String[] days = data.get("businessDays").split(",");
            for (String day : days) {
                setDaySchedule(day.trim(), data.get("openTime"), data.get("closeTime"));
            }
        }
    }

    /*
     * ---------------------------------------------------------------
     * System-initiated messages tab
     * ---------------------------------------------------------------
     */

    public void clickSystemInitiatedTab() {
        dismissSwal2IfPresent();
        Locator tab = page.locator("a[href='#tab_6_4']");
        tab.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        // Tab is initially disabled — wait for it to become enabled after filling
        // previous tabs
        try {
            page.waitForFunction(
                    "() => !document.querySelector(\"a[href='#tab_6_4']\").classList.contains('disabled')",
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(30000));
        } catch (RuntimeException ignored) {
        }
        tab.click();
        page.waitForTimeout(1500);
    }

    public void clickFollowUpSubTab() {
        dismissSwal2IfPresent();
        page.locator("a.nav-link[href='#followUpSection']").click();
        page.waitForTimeout(500);
    }

    public void setRespondedUserFollowUp(String sentDays, String hours) {
        page.locator("select[name='user_follow_up[0][f_day]']").selectOption(sentDays);
        if (isTruthy(hours)) {
            page.locator("select[name='user_follow_up[0][f_hours]']").selectOption(hours);
        }
        page.waitForTimeout(300);
    }

    public void setNonRespondedUserFollowUp(String sentDays, String hours) {
        page.locator("select[name='follow_up[0][f_day]']").selectOption(sentDays);
        if (isTruthy(hours)) {
            page.locator("select[name='follow_up[0][f_hours]']").selectOption(hours);
        }
        page.waitForTimeout(300);
    }

    public void enableCTA() {
        dismissSwal2IfPresent();
        Locator ctaCheckbox = page.locator("input[type='checkbox'][name='enableCTA']").first();
        try {
            if (!ctaCheckbox.isChecked()) {
                ctaCheckbox.evaluate(
                        "(el) => { el.checked = true; el.dispatchEvent(new Event('click', { bubbles: true })); "
                                + "el.dispatchEvent(new Event('change', { bubbles: true })); }");
            }
        } catch (RuntimeException e) {
            System.out.println("Failed to enable CTA via JS: " + e.getMessage());
            ctaCheckbox.check(new Locator.CheckOptions().setForce(true));
        }
        page.waitForTimeout(500);
    }

    public void fillCTAText(String inWorkingHour, String afterWorkingHour, String afterCallSchedule) {
        if (isTruthy(inWorkingHour)) {
            page.locator("textarea[name='field[0][cta_in_working_hour]']").fill(inWorkingHour);
        }
        if (isTruthy(afterWorkingHour)) {
            page.locator("textarea[name='field[0][cta_after_working_hour]']").fill(afterWorkingHour);
        }
        if (isTruthy(afterCallSchedule)) {
            page.locator("textarea[name='field[0][call_scheduled_cta]']").fill(afterCallSchedule);
        }
        page.waitForTimeout(300);
    }

    public void fillSystemInitiatedMessages(Map<String, String> data) {
        clickSystemInitiatedTab();
        clickFollowUpSubTab();
        if (isTruthy(data.get("respondedSentDays")) || isTruthy(data.get("respondedHours"))) {
            setRespondedUserFollowUp(data.get("respondedSentDays"), data.get("respondedHours"));
        }
        if (isTruthy(data.get("nonRespondedSentDays")) || isTruthy(data.get("nonRespondedHours"))) {
            setNonRespondedUserFollowUp(data.get("nonRespondedSentDays"), data.get("nonRespondedHours"));
        }
        String enableCta = data.get("enableCTA");
        if ("Yes".equals(enableCta) || "yes".equals(enableCta)) {
            enableCTA();
            if (isTruthy(data.get("ctaInWorkingHour")) || isTruthy(data.get("ctaAfterWorkingHour"))
                    || isTruthy(data.get("ctaAfterCallSchedule"))) {
                fillCTAText(data.get("ctaInWorkingHour"), data.get("ctaAfterWorkingHour"),
                        data.get("ctaAfterCallSchedule"));
            }
        }
    }

    /*
     * ---------------------------------------------------------------
     * Sweet-alert helper
     * ---------------------------------------------------------------
     */

    public void dismissSweetAlert() {
        page.evaluate("() => {"
                + "  const swal = document.querySelector('.swal2-container');"
                + "  if (swal) {"
                + "    const confirmBtn = swal.querySelector('.swal2-confirm');"
                + "    if (confirmBtn) confirmBtn.click();"
                + "    swal.style.display = 'none';"
                + "    document.body.classList.remove('swal2-shown', 'swal2-height-auto');"
                + "  }"
                + "}");
        page.waitForTimeout(500);
    }

    /*
     * ---------------------------------------------------------------
     * Call settings tab
     * ---------------------------------------------------------------
     */

    public void clickCallSettingsTab() {
        Locator tab = page.locator("a[href='#tab_6_5']");
        tab.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        try {
            page.waitForFunction(
                    "() => !document.querySelector(\"a[href='#tab_6_5']\").classList.contains('disabled')",
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(30000));
        } catch (RuntimeException ignored) {
        }
        tab.click(new Locator.ClickOptions().setForce(true));
        page.waitForTimeout(1500);
        dismissSweetAlert();
    }

    public void setMaxCallScheduleDuration(String days) {
        page.locator("select[name='maximum_call_schedule_duration']").selectOption(String.valueOf(days));
        page.waitForTimeout(300);
    }

    public void setRedialAttemptInterval(String minutes) {
        page.locator("select[name='redial_gap']").selectOption(String.valueOf(minutes));
        page.waitForTimeout(300);
    }

    public void enableAIVoiceCall() {
        Locator outboundYes = page.locator("input[name='enable_ai_voice_outbound'][value='yes']");
        Locator inboundYes = page.locator("input[name='enable_ai_voice_inbound'][value='yes']");
        Locator group = page.locator("input[name='enable_ai_voice_outbound'], input[name='enable_ai_voice_inbound']");
        group.first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.ATTACHED).setTimeout(15_000));

        Locator target = outboundYes.count() > 0 ? outboundYes.first() : inboundYes.first();
        try {
            if (!target.isChecked()) {
                target.evaluate(
                        "(el) => { if (!el.checked) { el.checked = true; "
                                + "el.dispatchEvent(new Event('click', { bubbles: true })); "
                                + "el.dispatchEvent(new Event('change', { bubbles: true })); } }");
            }
        } catch (RuntimeException e) {
            System.out.println("Failed to enable AI voice via JS: " + e.getMessage());
            target.check(new Locator.CheckOptions().setForce(true));
        }
        page.waitForTimeout(600);
    }

    public void fillRepPhoneNumber(String phone) {
        page.locator("input[name='add_rep_phone[]']").fill(phone);
        page.waitForTimeout(300);
    }

    public void selectUnknownIncomingCall(String value) {
        Locator radio = page.locator("input[name='unknown_status'][value='" + value + "']");
        selectRadioByValue(radio, value);
        page.waitForTimeout(300);
    }

    public void selectRepUnavailableAction(String value) {
        Locator radio = page.locator("input[name='rep_unavailable_action'][value='" + value + "']");
        selectRadioByValue(radio, value);
        page.waitForTimeout(300);
    }

    private void selectRadioByValue(Locator radio, String value) {
        radio.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.ATTACHED).setTimeout(15_000));
        try {
            if (!radio.isChecked()) {
                radio.evaluate(
                        "(el) => { el.checked = true; "
                                + "el.dispatchEvent(new Event('click', { bubbles: true })); "
                                + "el.dispatchEvent(new Event('change', { bubbles: true })); }");
            }
        } catch (RuntimeException e) {
            System.out.println("Failed to select radio '" + value + "' via JS: " + e.getMessage());
            radio.check(new Locator.CheckOptions().setForce(true));
        }
    }

    public void fillCallSettings(Map<String, String> data) {
        clickCallSettingsTab();
        dismissSweetAlert();

        System.out.println("CallSettings data: " + data);
        System.out.println("AI Voice Enabled: " + data.get("aiVoiceEnabled"));

        if (isTruthy(data.get("maxCallScheduleDuration"))) {
            setMaxCallScheduleDuration(data.get("maxCallScheduleDuration"));
        }

        if (isTruthy(data.get("redialGap"))) {
            setRedialAttemptInterval(data.get("redialGap"));
        }

        String aiVoiceEnabled = (data.get("aiVoiceEnabled") == null ? "" : data.get("aiVoiceEnabled"))
                .trim().toLowerCase();

        if ("yes".equals(aiVoiceEnabled)) {
            dismissSweetAlert();
            enableAIVoiceCall();
        }

        if (isTruthy(data.get("repPhoneNumber"))) {
            fillRepPhoneNumber(data.get("repPhoneNumber"));
        }

        if (isTruthy(data.get("unknownIncomingCall"))) {
            selectUnknownIncomingCall(data.get("unknownIncomingCall"));
        }

        if (isTruthy(data.get("repUnavailableAction"))) {
            selectRepUnavailableAction(data.get("repUnavailableAction"));
        }
    }

    /*
     * ---------------------------------------------------------------
     * Transfer settings tab
     * ---------------------------------------------------------------
     */

    public void clickTransferSettingsTab() {
        Locator tab = page.locator("a[href='#tab_6_7']");
        tab.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        try {
            page.waitForFunction(
                    "() => !document.querySelector(\"a[href='#tab_6_7']\").classList.contains('disabled')",
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(30000));
        } catch (RuntimeException ignored) {
        }
        tab.click(new Locator.ClickOptions().setForce(true));
        page.waitForTimeout(1500);
        dismissSweetAlert();
    }

    public void selectConnectingWhisperOption(String option) {
        page.locator("label:has-text(\"" + option + "\")").click();
        page.waitForTimeout(500);
    }

    public void selectPressAnyKeyWhisper(String option) {
        // Prefer accessible role selection, fall back to a visible label when needed.
        Locator radio = page.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName(option));
        if (radio.count() > 0) {
            try {
                radio.click(new Locator.ClickOptions().setTimeout(5000));
            } catch (RuntimeException e) {
                Locator fallback = page.locator("label:has-text(\"" + option + "\")");
                if (fallback.isVisible()) {
                    fallback.click(new Locator.ClickOptions().setTimeout(5000));
                }
            }
            page.waitForTimeout(300);
            return;
        }

        Locator fallback = page.locator("label:has-text(\"" + option + "\")");
        if (fallback.isVisible()) {
            fallback.click(new Locator.ClickOptions().setTimeout(5000));
            page.waitForTimeout(300);
        }
    }

    public void fillTransferSettings(Map<String, String> data) {
        clickTransferSettingsTab();

        Object whisperRadios = page.evaluate(
                "() => Array.from(document.querySelectorAll('input[type=radio]'))"
                        + ".filter(r => /whisper|press_any_key|user_end|userend|campaign/i.test(r.name))"
                        + ".map(r => { const l = r.closest('label'); "
                        + "const card = l ? (l.closest('.card') || l.closest('.col') || null) : null; "
                        + "return (r.name || '') + '=' + (r.value || '') + ' checked=' + r.checked"
                        + " + (l ? ' label=[' + (l.innerText || '').replace(/\\s+/g, ' ').trim() + ']' : '')"
                        + " + (card ? ' card=[' + (card.innerText || '').replace(/\\s+/g, ' ').trim().slice(0, 60) + ']' : ''); })");
        System.out.println("Whisper radios: " + whisperRadios);

        if (isTruthy(data.get("connectingWhisper"))) {
            selectConnectingWhisperOption(data.get("connectingWhisper"));
        }

        selectWhisperSectionOption("User End Outbound Call Whisper", "Campaign Specific");

        // Business rule: Press Any Key Whisper may need special handling.
        try {
            Locator userRepLocator = page.getByRole(AriaRole.RADIO,
                    new Page.GetByRoleOptions().setName("User - Rep"));
            Locator fullRecordingLocator = page.getByRole(AriaRole.RADIO,
                    new Page.GetByRoleOptions().setName("Full recording"));
            Locator defaultRecordingLocator = page.getByRole(AriaRole.RADIO,
                    new Page.GetByRoleOptions().setName("Default"));

            boolean isUserRep = userRepLocator.count() > 0 && safeIsChecked(userRepLocator);
            boolean isFullRecording = fullRecordingLocator.count() > 0 && safeIsChecked(fullRecordingLocator);
            boolean isDefaultRecording = defaultRecordingLocator.count() > 0
                    && safeIsChecked(defaultRecordingLocator);

            if (isUserRep && (isFullRecording || isDefaultRecording)) {
                selectPressAnyKeyWhisper("Campaign Specific");
            } else if (isTruthy(data.get("pressAnyKeyWhisper"))) {
                selectPressAnyKeyWhisper(data.get("pressAnyKeyWhisper"));
            }
        } catch (RuntimeException e) {
            // ignore — best-effort enforcement for test stability
        }
    }

    /**
     * Selects a radio option inside the section headed by {@code section}
     * (e.g. "User End Outbound Call Whisper"). Uses JS to reliably toggle the
     * hidden custom-styled radios this app renders.
     */
    public void selectWhisperSectionOption(String section, String option) {
        Object result = page.evaluate(
                "([section, option]) => {"
                        + "  const key = s => (s || '').toLowerCase().replace(/[^a-z0-9]+/g, '');"
                        + "  const norm = t => (t || '').replace(/[\\s\\u00a0]+/g, ' ').trim();"
                        + "  const heads = Array.from(document.querySelectorAll("
                        + "    'h1,h2,h3,h4,h5,h6,legend,label,span,p,strong,b,div'));"
                        + "  const head = heads.find(e => "
                        + "    norm(e.innerText || e.textContent).toLowerCase().startsWith(section.toLowerCase())"
                        + "    && (e.innerText || e.textContent).length < 200);"
                        + "  if (!head) return { ok: false, reason: 'section heading not found' };"
                        + "  let node = head;"
                        + "  for (let i = 0; i < 8; i++) {"
                        + "    node = node.parentElement;"
                        + "    if (!node || node.tagName === 'BODY') break;"
                        + "    const radios = Array.from(node.querySelectorAll('input[type=radio]'));"
                        + "    for (const r of radios) {"
                        + "      if (key(r.value) === key(option)) {"
                        + "        r.checked = true;"
                        + "        r.dispatchEvent(new MouseEvent('click', { bubbles: true }));"
                        + "        r.dispatchEvent(new Event('change', { bubbles: true }));"
                        + "        return { ok: true, name: r.name, value: r.value, via: 'radio' };"
                        + "      }"
                        + "    }"
                        + "    const labels = Array.from(node.querySelectorAll('label'));"
                        + "    for (const l of labels) {"
                        + "      if (key(norm(l.innerText || l.textContent)) === key(option)) {"
                        + "        const inside = l.querySelector('input[type=radio]');"
                        + "        const forId = l.getAttribute('for');"
                        + "        const ref = inside || (forId ? document.getElementById(forId) : null);"
                        + "        if (ref && ref.type === 'radio') {"
                        + "          ref.checked = true;"
                        + "          ref.dispatchEvent(new MouseEvent('click', { bubbles: true }));"
                        + "          ref.dispatchEvent(new Event('change', { bubbles: true }));"
                        + "          return { ok: true, name: ref.name, value: ref.value, via: 'label' };"
                        + "        }"
                        + "        l.click();"
                        + "        return { ok: true, via: 'label-click' };"
                        + "      }"
                        + "    }"
                        + "  }"
                        + "  return { ok: false, reason: 'option not found in section scope' };"
                        + "}",
                java.util.Arrays.asList(section, option));
        System.out.println("Whisper section ['" + section + "' -> '" + option + "']: " + result);
        page.waitForTimeout(300);

        Object after = page.evaluate(
                "() => Array.from(document.querySelectorAll('input[type=radio]'))"
                        + ".filter(r => /whisper|press_any_key|user_end|userend/i.test(r.name))"
                        + ".map(r => (r.name || '') + '=' + (r.value || '') + ' checked=' + r.checked)");
        System.out.println("Whisper radios AFTER selection: " + after);
    }

    private boolean safeIsChecked(Locator locator) {
        try {
            return locator.isChecked();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /*
     * ---------------------------------------------------------------
     * Submit + verify
     * ---------------------------------------------------------------
     */

    public void clickAddCampaign() {
        Locator addBtn = page.locator("input#add_campaign");
        assertThat(addBtn).isVisible(
                new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions()
                        .setTimeout(15000));
        addBtn.scrollIntoViewIfNeeded();

        page.evaluate("() => {"
                + "  const btn = document.querySelector('input#add_campaign');"
                + "  if (btn) { btn.removeAttribute('disabled'); }"
                + "}");

        Object btnInfo = page.evaluate(
                "() => { const b = document.querySelector('input#add_campaign');"
                        + " return b ? {disabled: b.disabled, type: b.type, value: b.value, form: b.form?.id ?? 'none'} : null; }");
        System.out.println("add_campaign button state: " + btnInfo);

        try {
            addBtn.click(new Locator.ClickOptions().setTimeout(15000));
            waitForCampaignSubmissionResult();
            System.out.println("Add campaign clicked. Current URL: " + page.url());
        } catch (RuntimeException error) {
            try {
                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(Paths.get("test-results/debug-clickAddCampaign.png")));
            } catch (RuntimeException ignored) {
            }
            throw error;
        }
    }

    private void waitForCampaignSubmissionResult() {
        try {
            page.waitForFunction(
                    "() => {"
                            + " const success = document.querySelector('.swal2-popup.swal2-icon-success');"
                            + " const validation = document.querySelector("
                            + "'.error, .invalid, .text-danger, .help-block, "
                            + ".field-validation-error, [aria-invalid=\"true\"]');"
                            + " return (success && success.offsetParent !== null)"
                            + " || (validation && validation.offsetParent !== null);"
                            + "}",
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(60_000));
        } catch (RuntimeException e) {
            System.out.println("Campaign submission is still processing after 60 seconds.");
        }
    }

    public void clickAddCampaignDuplicate() {
        System.out.println("Starting clickAddCampaign_duplicate");

        // Clear any overlay that could intercept the click
        dismissSweetAlert();

        String[] possibleSelectors = {
                "input#add_campaign_intent_wise",
                "input#add_campaign",
                "button:has-text('Create Campaign')",
                "button:has-text('Submit')",
                "input[type='submit']",
        };

        Locator addBtn = null;
        String matchedSelector = null;

        for (String selector : possibleSelectors) {
            Locator candidate = page.locator(selector).first();

            if (candidate.count() == 0) {
                System.out.println("Not in DOM: " + selector);
                continue;
            }

            try {
                candidate.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE).setTimeout(8000));
                addBtn = candidate;
                matchedSelector = selector;
                System.out.println("Found visible button with selector: " + selector);
                break;
            } catch (RuntimeException e) {
                System.out.println("In DOM but not visible: " + selector);
            }
        }

        if (addBtn == null) {
            try {
                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(Paths.get("test-results/debug-clickAddCampaign_duplicate-nobtn.png"))
                        .setFullPage(true));
            } catch (RuntimeException ignored) {
            }
            throw new RuntimeException(
                    "Could not find a visible add/submit campaign button on the duplicate page.");
        }

        addBtn.scrollIntoViewIfNeeded();

        // Re-enable the SAME button we matched (not a hardcoded id)
        page.evaluate("(selector) => {"
                + "  const btn = document.querySelector(selector);"
                + "  if (btn) {"
                + "    btn.removeAttribute('disabled');"
                + "    btn.classList.remove('disabled');"
                + "    btn.style.pointerEvents = 'auto';"
                + "    btn.style.opacity = '1';"
                + "  }"
                + "}", matchedSelector);

        // Dismiss overlays again right before clicking
        dismissSweetAlert();

        try {
            System.out.println("Attempting standard click...");
            addBtn.click(new Locator.ClickOptions().setTimeout(15000));
            System.out.println("Standard click succeeded");
        } catch (RuntimeException error) {
            System.out.println("Standard click failed, trying force click...");
            try {
                addBtn.click(new Locator.ClickOptions().setTimeout(5000).setForce(true));
                System.out.println("Force click succeeded");
            } catch (RuntimeException error2) {
                System.out.println("Force click failed, trying JS click...");
                page.evaluate("(selector) => {"
                        + "  const btn = document.querySelector(selector);"
                        + "  if (btn) btn.click();"
                        + "}", matchedSelector);
                System.out.println("JS click dispatched");
            }
        }

        try {
            page.waitForLoadState(LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(10000));
        } catch (RuntimeException e) {
            System.out.println("Network idle timeout - form may still be processing");
        }

        System.out.println("Form submission processed");
    }

    public void handleValidationPopup() {
        for (int i = 0; i < 15; i++) {
            Locator proceedBtn = page.locator("button.groundedRagReviewProceed");
            if (proceedBtn.isVisible(new Locator.IsVisibleOptions().setTimeout(3000))) {
                System.out.println("groundedRagReviewProceed popup found - clicking proceed");
                proceedBtn.click();
                page.waitForTimeout(1000);
                continue;
            }
            Locator successPopup = page.locator(".swal2-popup.swal2-icon-success");
            if (successPopup.count() > 0 && successPopup.first().isVisible()) {
                System.out.println("Campaign success popup found; leaving it open for assertion.");
                break;
            }
            Locator confirmBtn = page.locator(".swal2-container .swal2-confirm");
            boolean dismissed = false;
            if (confirmBtn.count() > 0 && confirmBtn.first().isVisible()) {
                String label = confirmBtn.first().textContent() == null
                        ? ""
                        : confirmBtn.first().textContent().trim();
                System.out.println("Dismissing confirmation popup: '" + label + "'");
                confirmBtn.first().click();
                page.waitForTimeout(1000);
                dismissed = true;
            }
            if (!dismissed) {
                break;
            }
        }
    }

    public void verifyCampaignCreated() {
        Locator swalSuccess = page.locator(".swal2-popup.swal2-icon-success");
        Locator swalTitle = page.locator("#swal2-title",
                new Page.LocatorOptions().setHasText("Campaign Created Successfully"));
        Locator okButton = page.locator(".swal2-actions button.swal2-confirm");

        boolean shown = false;
        for (int i = 0; i < 12; i++) {
            try {
                if (swalSuccess.isVisible(new Locator.IsVisibleOptions().setTimeout(5000))
                        && swalTitle.isVisible(new Locator.IsVisibleOptions().setTimeout(1000))) {
                    shown = true;
                    break;
                }
            } catch (RuntimeException ignored) {
            }
            // A confirmation popup may be blocking submission — accept it and keep waiting.
            Locator confirmBtn = page.locator(
                    ".swal2-container:not(:has(.swal2-icon-success)) .swal2-confirm");
            if (confirmBtn.count() > 0 && confirmBtn.first().isVisible()) {
                String label = confirmBtn.first().textContent() == null
                        ? ""
                        : confirmBtn.first().textContent().trim();
                System.out.println("Dismissing popup while waiting for success: '" + label + "'");
                confirmBtn.first().click();
                page.waitForTimeout(1000);
            }
        }

        if (!shown) {
            // Diagnostic: dump whatever SWAL is currently visible
            Locator anySwal = page.locator(".swal2-popup");
            if (anySwal.count() > 0 && anySwal.first().isVisible()) {
                String title = page.locator("#swal2-title").textContent() == null
                        ? ""
                        : page.locator("#swal2-title").textContent();
                String html = page.locator(".swal2-html-container").textContent() == null
                        ? ""
                        : page.locator(".swal2-html-container").textContent();
                System.out.println("SWAL title: '" + title.trim() + "' html: '" + html.trim() + "'");
            } else {
                System.out.println("No SWAL popup visible. Current URL: " + page.url());
            }

            Object validation = page.evaluate(
                    "() => Array.from(document.querySelectorAll('.error, .invalid, .text-danger, "
                            + ".help-block, .field-validation-error, [aria-invalid=\"true\"]'))"
                            + ".filter(el => el.offsetParent !== null)"
                            + ".map(el => {"
                            + "  const fg = el.closest('.form-group, .form-row, .form-item, .col, td, tr, .nav-item, .tab-pane');"
                            + "  const html = fg ? fg.outerHTML.replace(/\\s+/g, ' ').slice(0, 400) : '';"
                            + "  const inputs = fg ? Array.from(fg.querySelectorAll('input,select,textarea'))"
                            + "      .map(i => (i.name || i.id || '?') + '/' + (i.type || '') + '=' + (i.value || '')).join(' , ') : '';"
                            + "  const lab = el.closest('label');"
                            + "  const txt = lab ? (lab.innerText || '').replace(/\\s+/g, ' ').trim().slice(0, 60) : '';"
                            + "  return (txt || el.textContent.trim()) + ' ||| inputs: ' + inputs + ' ||| html: ' + html;"
                            + "}).slice(0, 15)");
            System.out.println("DETAILED validation messages: " + validation);

            Object requiredFields = page.evaluate(
                    "() => Array.from(document.querySelectorAll('#add_admin_user_form input, "
                            + "#add_admin_user_form select, #add_admin_user_form textarea'))"
                            + ".filter(el => el.required || el.getAttribute('aria-required') === 'true'"
                            + " || /required/i.test(el.className))"
                            + ".map(el => {"
                            + "  const t = el.type || '';"
                            + "  let v = (t === 'checkbox' || t === 'radio') ? String(el.checked) : (el.value || '');"
                            + "  if (v.length > 30) v = v.slice(0, 30) + '...';"
                            + "  const lab = (el.id ? document.querySelector('label[for=\"' + el.id + '\"]') : null)"
                            + "        || el.closest('label');"
                            + "  const labText = lab ? (lab.innerText || '').replace(/\\s+/g, ' ').trim().slice(0, 60) : '';"
                            + "  return (el.name || el.id || '?') + '[' + labText + '] = ' + v;"
                            + "})");
            System.out.println("Required fields state: " + requiredFields);

            try {
                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(Paths.get("test-results/debug-verify-fail-state.png"))
                        .setFullPage(true));
                System.out.println("Saved full-page screenshot: test-results/debug-verify-fail-state.png");
            } catch (RuntimeException e) {
                System.out.println("Screenshot failed: " + e.getMessage());
            }
        }

        assertThat(swalSuccess).isVisible(
                new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions()
                        .setTimeout(60000));
        assertThat(swalTitle).isVisible(
                new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions()
                        .setTimeout(15000));
        System.out.println("'Campaign Created Successfully.' popup is visible");

        okButton.click();
        System.out.println("Clicked OK — campaign verified");

        try {
            page.waitForLoadState(LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(10000));
        } catch (RuntimeException ignored) {
        }
    }

    public void verifyDuplicateCampaignCreated() {
        Locator successPopup = page.locator(".swal2-popup.swal2-icon-success");
        Locator successTitle = page.locator("#swal2-title",
                new Page.LocatorOptions().setHasText("Campaign Created Successfully."));
        Locator okButton = page.locator(".swal2-actions button.swal2-confirm");

        // Assert the success popup appears (auto-waits up to the timeout; fails the
        // test if it never shows)
        assertThat(successPopup).isVisible(
                new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions()
                        .setTimeout(45000));
        assertThat(successTitle).isVisible(
                new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions()
                        .setTimeout(10000));
        System.out.println("'Campaign Created Successfully.' popup is visible");

        // Click OK to dismiss
        assertThat(okButton).isVisible(
                new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions()
                        .setTimeout(10000));
        okButton.click();
        System.out.println("Clicked OK — duplicate campaign verified");

        // Wait for the modal to close so trailing teardown is clean
        try {
            assertThat(successPopup).isHidden(
                    new com.microsoft.playwright.assertions.LocatorAssertions.IsHiddenOptions()
                            .setTimeout(10000));
        } catch (AssertionError ignored) {
        }
    }

    public void fillGeneralSettings(Map<String, String> data) {
        fillAccountName(data.get("accountName"));
        if (isTruthy(data.get("brandName"))) {
            fillBrandName(data.get("brandName"));
        }
        if (isTruthy(data.get("campaignName"))) {
            fillCampaignName(data.get("campaignName"));
        }
        if (isTruthy(data.get("avatarName"))) {
            fillAvatarName(data.get("avatarName"));
        }
        selectCampaignSkillType(data.get("campaignSkillType"));
        selectLeadProcessType(data.get("leadProcessType"));
        if (isTruthy(data.get("inboundTextCampaign"))) {
            selectInboundTextCampaign(data.get("inboundTextCampaign"));
        }
        if (isTruthy(data.get("meeraAiBot"))) {
            selectMeeraAiBot(data.get("meeraAiBot"));
        }
        if (isTruthy(data.get("carrierLookup"))) {
            selectCarrierLookup(data.get("carrierLookup"));
        }
        if (isTruthy(data.get("throttleLeadPost"))) {
            selectThrottleLeadPost(data.get("throttleLeadPost"));
        }
        selectResponseType(data.get("responseType"));
        selectIndustry(data.get("industry"));
        selectSubIndustry(data.get("subIndustry"));
        selectUseCase(data.get("useCase"));
        selectSkill(data.get("skills"));
        // Campaign Goal & Description is auto-populated based on selections above
        page.waitForTimeout(1000);
    }
}
