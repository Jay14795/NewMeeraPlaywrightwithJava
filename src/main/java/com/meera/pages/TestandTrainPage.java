package com.meera.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Test &amp; Train (voice) page object. Port of {@code pages/TestandTrainPage.js}.
 */
public class TestandTrainPage {

    private final Page page;

    private final Locator callTypeDropdownToggle;
    private final Locator callTypeOptions;
    private final Locator startPromptBtn;
    private final Locator hangupButton;
    private final Locator muteButton;
    private final Locator voiceStatusLabel;
    private final Locator startPromptTextarea;
    private final Locator startPromptSendBtn;
    private final Locator voiceGreetingTextarea;
    private final Locator voiceGreetingCallBtn;
    private final Locator messageInput;
    private final Locator sendBtn;
    private final Locator messageList;
    private final Locator callTypeSelect;

    public TestandTrainPage(Page page) {
        this.page = page;
        this.callTypeDropdownToggle = page.locator("#voice-start-btn");
        this.callTypeOptions = page.locator("a.call-type-option");
        this.startPromptBtn = page.locator("#empty-start-prompt-btn");
        this.hangupButton = page.locator("#voice-hangup-btn");
        this.muteButton = page.locator("#voice-mute-btn");
        this.voiceStatusLabel = page.locator("#voice-status");
        this.startPromptTextarea = page.locator("#start-prompt-textarea");
        this.startPromptSendBtn = page.locator("#save-prompt-btn");
        this.voiceGreetingTextarea = page.locator("#voice-greeting-textarea");
        this.voiceGreetingCallBtn = page.locator("#voice-greeting-call-btn");
        this.messageInput = page.locator("#message-input");
        this.sendBtn = page.locator("#send-btn");
        this.messageList = page.locator("#message-list");
        this.callTypeSelect = page.locator("#call-type-select");
    }

    private static boolean isTruthy(String s) {
        return s != null && !s.isEmpty();
    }

    /**
     * Chromium ignores {@code --audio-input-device-id} for
     * {@code getUserMedia("default")}; it keeps using the OS default mic instead
     * of the VB-Cable loopback the TTS agent speaks into. Patching getUserMedia
     * to pin the CABLE Output deviceId is what actually routes it.
     */
    public void useVirtualCableMicrophone(String labelFragment) {
        String script = "(() => {"
                + "const labelFragment = '" + labelFragment + "';"
                + "const originalGetUserMedia = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);"
                + "navigator.mediaDevices.getUserMedia = async (constraints) => {"
                + "  try {"
                + "    const devices = await navigator.mediaDevices.enumerateDevices();"
                + "    const cableDevice = devices.find(d => d.kind === 'audioinput'"
                + "        && d.label.includes(labelFragment)"
                + "        && d.deviceId !== 'communications'"
                + "        && d.deviceId !== 'default');"
                + "    if (cableDevice && constraints && constraints.audio !== false) {"
                + "      const audioConstraints = typeof constraints.audio === 'object' ? constraints.audio : {};"
                + "      constraints = { ...constraints, audio: { ...audioConstraints, deviceId: { exact: cableDevice.deviceId } } };"
                + "    }"
                + "  } catch (e) { console.warn('Could not pin CABLE Output as mic input:', e); }"
                + "  return originalGetUserMedia(constraints);"
                + "};"
                + "})();";
        page.addInitScript(script);
    }

    public void useVirtualCableMicrophone() {
        useVirtualCableMicrophone("CABLE Output");
    }

    public void selectCallType(String callType) {
        String type = callType.toLowerCase();
        callTypeDropdownToggle.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        callTypeDropdownToggle.click();
        page.waitForTimeout(500);
        Locator option = callTypeOptions.filter(new Locator.FilterOptions()
                .setHasText(Pattern.compile(type, Pattern.CASE_INSENSITIVE)));
        option.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        option.click();
        page.waitForTimeout(500);
        // Close dropdown if still open
        page.keyboard().press("Escape");
        page.waitForTimeout(300);
    }

    public void startCall(String promptText) {
        // Handle any permission dialogs
        page.onDialog(dialog -> {
            System.out.println("Dialog detected: " + dialog.message());
            dialog.accept();
        });

        // Check if voice greeting modal is already visible
        boolean greetingVisible = voiceGreetingCallBtn.isVisible();
        if (greetingVisible) {
            System.out.println("Voice greeting modal already visible, filling greeting and clicking Call...");
            if (isTruthy(promptText)) {
                voiceGreetingTextarea.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
                voiceGreetingTextarea.fill(promptText);
                page.waitForTimeout(300);
            }
            voiceGreetingCallBtn.click();
            page.waitForTimeout(2000);
            return;
        }

        // Otherwise try clicking Send Start Prompt
        System.out.println("Looking for Send Start Prompt button...");
        boolean btnVisible = startPromptBtn.isVisible();
        if (btnVisible) {
            startPromptBtn.click();
            page.waitForTimeout(2000);

            // Fill and send start prompt modal
            boolean textareaVisible = startPromptTextarea.isVisible();
            if (textareaVisible) {
                if (isTruthy(promptText)) {
                    startPromptTextarea.fill(promptText);
                    page.waitForTimeout(300);
                }
                startPromptSendBtn.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
                startPromptSendBtn.click();
                page.waitForTimeout(2000);
            }

            // Handle voice greeting modal after start prompt
            boolean greetingAfter = voiceGreetingCallBtn.isVisible();
            if (greetingAfter) {
                System.out.println("Voice greeting modal appeared, filling greeting and clicking Call...");
                String current = "";
                try {
                    current = voiceGreetingTextarea.inputValue();
                } catch (RuntimeException ignored) {
                    current = "";
                }
                boolean textareaEmpty = current.isEmpty();
                if (textareaEmpty && isTruthy(promptText)) {
                    voiceGreetingTextarea.fill(promptText);
                    page.waitForTimeout(300);
                }
                voiceGreetingCallBtn.click();
                page.waitForTimeout(2000);
            }
        } else {
            System.out.println("Start Prompt button not visible, fallback: typing message directly...");
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("debug-fallback.png")));
            messageInput.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            messageInput.fill(isTruthy(promptText) ? promptText : "Hi");
            page.waitForTimeout(300);
            sendBtn.click();
            page.waitForTimeout(2000);
        }
    }

    public void clickVoiceGreetingCall() {
        boolean visible = voiceGreetingCallBtn.isVisible();
        if (visible) {
            voiceGreetingCallBtn.click();
            page.waitForTimeout(2000);
        }
    }

    public void clickHangupButton() {
        hangupButton.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        hangupButton.click();
        page.waitForTimeout(2000);
    }

    public String getVoiceStatus() {
        try {
            voiceStatusLabel.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        } catch (RuntimeException ignored) {
            // fall through to read whatever is there
        }
        try {
            String status = voiceStatusLabel.first().textContent();
            return status != null ? status.trim() : "Status not found";
        } catch (RuntimeException e) {
            return "Status not found";
        }
    }

    public void ensureUnmuted() {
        try {
            boolean visible = muteButton.isVisible();
            if (visible) {
                muteButton.click();
                page.waitForTimeout(500);
            }
        } catch (RuntimeException e) {
            System.out.println("Could not check mute state, continuing...");
        }
    }

    public void ensureMuted() {
        try {
            boolean visible = muteButton.isVisible();
            boolean unmutedClass = muteButton.locator("i.fa-microphone").isVisible();
            if (visible && unmutedClass) {
                muteButton.click();
                page.waitForTimeout(500);
            }
        } catch (RuntimeException e) {
            System.out.println("Could not check mute state, continuing...");
        }
    }

    public boolean waitForIncomingCall(int timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            String status = getVoiceStatus();
            if (status.toLowerCase().contains("in call")) {
                return true;
            }
            page.waitForTimeout(2000);
        }
        System.out.println("No incoming call detected within timeout");
        return false;
    }

    public boolean waitForIncomingCall() {
        return waitForIncomingCall(30000);
    }

    public void answerIncomingCall() {
        // For inbound calls, the call connects automatically when we start;
        // just wait for status to change.
        System.out.println("Waiting for inbound call connection...");
    }

    public String getCallLog() {
        try {
            return messageList.textContent();
        } catch (RuntimeException e) {
            return "";
        }
    }

    public String getTranscript() {
        return getCallLog();
    }

    public void sendTestQuestion(String question) {
        messageInput.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        messageInput.fill(question);
        page.waitForTimeout(300);
        sendBtn.click();
        page.waitForTimeout(2000);
    }

    public String getBotResponse() {
        try {
            List<String> messages = messageList.locator(".message").allTextContents();
            return messages.isEmpty() ? "" : messages.get(messages.size() - 1);
        } catch (RuntimeException e) {
            return "";
        }
    }

    public boolean waitForCallConnected(int timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            String status = getVoiceStatus();
            System.out.println("  Voice status: \"" + status + "\"");
            if (status.toLowerCase().contains("in call")) {
                return true;
            }
            if (status.toLowerCase().contains("error") || status.toLowerCase().contains("failed")) {
                System.out.println("Call failed: " + status);
                return false;
            }
            page.waitForTimeout(2000);
        }
        System.out.println("Call did not connect within timeout");
        return false;
    }

    public boolean waitForCallConnected() {
        return waitForCallConnected(30000);
    }

    public boolean waitForCallToEnd(int timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            String status = getVoiceStatus();
            if (!status.toLowerCase().contains("in call")) {
                return true;
            }
            page.waitForTimeout(1000);
        }
        return false;
    }

    public boolean waitForCallToEnd() {
        return waitForCallToEnd(30000);
    }

    public boolean isCallActive() {
        String status = getVoiceStatus();
        return status.toLowerCase().contains("in call");
    }
}
