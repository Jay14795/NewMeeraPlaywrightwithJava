package com.meera.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Text-to-speech helper that speaks test questions through the Windows SAPI
 * voice, routed to the VB-CABLE loopback so the browser mic picks it up.
 *
 * <p>Direct port of the TypeScript {@code utils/voice-agent.js}. It shells out
 * to PowerShell with a base64 (UTF-16LE) {@code -EncodedCommand}, exactly like
 * the original {@code execSync} calls. Windows-only, same as the source.</p>
 */
public class VoiceAgent {

    /** One logged Q/A exchange. */
    public static class QuestionRecord {
        public final String question;
        public final String response;
        public final long durationMs;
        public final String timestamp;

        QuestionRecord(String question, String response, long durationMs) {
            this.question = question;
            this.response = response;
            this.durationMs = durationMs;
            this.timestamp = Instant.now().toString();
        }
    }

    /** Report state, mirrors the JS {@code reportData} object. */
    public static class ReportData {
        public String startTime = null;
        public String endTime = null;
        public String status = "Not Started";
        public final List<QuestionRecord> questions = new ArrayList<>();
    }

    private final int speechRate;
    private final String voice;
    private final ReportData reportData = new ReportData();
    private boolean isSpeaking = false;

    public VoiceAgent() {
        this(0, "Microsoft Zira Desktop");
    }

    public VoiceAgent(int speechRate, String voice) {
        this.speechRate = speechRate;
        this.voice = voice;
        init();
    }

    /* ---------------------------------------------------------------
     * PowerShell plumbing
     * --------------------------------------------------------------- */

    /** Wraps a PowerShell script as an encoded-command argument list. */
    private List<String> psCommand(String script) {
        String encoded = Base64.getEncoder()
                .encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
        List<String> cmd = new ArrayList<>();
        cmd.add("powershell");
        cmd.add("-NoProfile");
        cmd.add("-EncodedCommand");
        cmd.add(encoded);
        return cmd;
    }

    /** Runs PowerShell, returns stdout, throws on timeout/non-zero exit. */
    private String runPowerShell(String script, long timeoutMs) {
        try {
            ProcessBuilder pb = new ProcessBuilder(psCommand(script));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("PowerShell command timed out after " + timeoutMs + "ms");
            }
            return output.toString();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("PowerShell execution failed: " + e.getMessage(), e);
        }
    }

    /* ---------------------------------------------------------------
     * Init check (audio outputs / CABLE / voices)
     * --------------------------------------------------------------- */

    private void init() {
        String checkScript = String.join("\n",
                "try {",
                "  $voice = New-Object -ComObject SAPI.SpVoice;",
                "  $devices = $voice.GetAudioOutputs();",
                "  \"Available audio outputs: \" + $devices.Count;",
                "  $devices | ForEach-Object { $_.GetDescription() + \" | \" + $_.Id };",
                "  $cable = $devices | Where-Object { $_.Id -like '*59eff3a3*' };",
                "  if ($cable) { \"CABLE Input device FOUND\" } else { \"CABLE Input device NOT FOUND\" };",
                "  $voice.GetVoices() | ForEach-Object { $_.GetDescription() }",
                "} catch { write-host \"ERROR: $_\" }");
        try {
            String result = runPowerShell(checkScript, 10_000);
            if (result.contains("ERROR")) {
                System.out.println("Voice synthesis may not be available: " + result.trim());
            } else {
                List<String> lines = new ArrayList<>();
                for (String l : result.trim().split("\n")) {
                    if (!l.trim().isEmpty()) {
                        lines.add(l);
                    }
                }
                for (String l : lines) {
                    if (l.contains("|") || l.contains("CABLE")) {
                        System.out.println("  " + l);
                    }
                }
                long voiceLines = lines.stream()
                        .filter(l -> !l.contains("|") && !l.contains("CABLE") && !l.startsWith("Available"))
                        .count();
                if (voiceLines > 0) {
                    System.out.println("Voices available: " + voiceLines);
                }
                boolean hasCable = lines.stream().anyMatch(l -> l.contains("CABLE Input device FOUND"));
                if (hasCable) {
                    System.out.println("Audio loopback: CABLE Input ready for TTS output");
                } else {
                    System.out.println("CABLE Input device not found - TTS will play through speakers");
                }
            }
        } catch (RuntimeException e) {
            System.out.println("Voice synthesis init warning: " + e.getMessage());
        }
    }

    /* ---------------------------------------------------------------
     * Speaking
     * --------------------------------------------------------------- */

    private void speakViaPowerShell(String text) {
        String safeText = text.replace("'", "''");
        String psScript = String.join("\n",
                "$voice = New-Object -ComObject SAPI.SpVoice;",
                "$devices = $voice.GetAudioOutputs();",
                "if ($devices.Count -gt 1) { $voice.AudioOutput = $devices.Item(1); }",
                "try { $voice.Rate = " + speechRate + "; } catch {}",
                "$voice.Speak('" + safeText + "');");
        runPowerShell(psScript, 60_000);
    }

    /**
     * Speaks a question aloud and returns how long it took, in milliseconds.
     * Retries once with a minimal fallback script, matching the JS behaviour.
     */
    public long speakQuestion(String question) {
        isSpeaking = true;
        long startTime = System.currentTimeMillis();
        try {
            speakViaPowerShell(question);
        } catch (RuntimeException e) {
            try {
                String safeText = question.replace("'", "''");
                String fallbackScript = String.join("\n",
                        "$v = New-Object -ComObject SAPI.SpVoice;",
                        "$d = $v.GetAudioOutputs();",
                        "if ($d.Count -gt 1) { $v.AudioOutput = $d.Item(1); }",
                        "$v.Speak('" + safeText + "');");
                runPowerShell(fallbackScript, 60_000);
            } catch (RuntimeException e2) {
                System.err.println("Speech failed: " + e2.getMessage());
                isSpeaking = false;
                throw e2;
            }
        }
        long endTime = System.currentTimeMillis();
        isSpeaking = false;
        return endTime - startTime;
    }

    /** Alias, same as JS {@code speak()}. */
    public long speak(String question) {
        return speakQuestion(question);
    }

    public boolean isSpeaking() {
        return isSpeaking;
    }

    /* ---------------------------------------------------------------
     * Reporting
     * --------------------------------------------------------------- */

    public void logQuestion(String question, String response, long durationMs) {
        reportData.questions.add(new QuestionRecord(question, response, durationMs));
    }

    public ReportData generateReport() {
        reportData.endTime = Instant.now().toString();
        int qCount = reportData.questions.size();
        String bar = "=".repeat(60);
        System.out.println("\n" + bar);
        System.out.println("VOICE INTERACTION REPORT");
        System.out.println(bar);
        System.out.println("Status: " + reportData.status);
        System.out.println("Start Time: " + reportData.startTime);
        System.out.println("End Time: " + reportData.endTime);
        System.out.println("Total Questions: " + qCount);
        System.out.println("-".repeat(60));
        for (int i = 0; i < reportData.questions.size(); i++) {
            QuestionRecord q = reportData.questions.get(i);
            System.out.println("Q" + (i + 1) + ": " + q.question);
            System.out.println("   Response: " + (q.response == null ? "N/A" : q.response));
            System.out.println("   Duration: " + q.durationMs + "ms");
        }
        System.out.println(bar);
        return reportData;
    }

    public String saveReportToFile(String filePath) {
        ReportData report = generateReport();
        List<String> lines = new ArrayList<>();
        String bar = "=".repeat(60);
        lines.add(bar);
        lines.add("VOICE INTERACTION REPORT");
        lines.add(bar);
        lines.add("Status: " + report.status);
        lines.add("Start Time: " + (report.startTime == null ? "N/A" : report.startTime));
        lines.add("End Time: " + report.endTime);
        lines.add("Total Questions: " + report.questions.size());
        lines.add("-".repeat(60));
        for (int i = 0; i < report.questions.size(); i++) {
            QuestionRecord q = report.questions.get(i);
            lines.add("Q" + (i + 1) + ": " + q.question);
            lines.add("   Response: " + (q.response == null ? "N/A" : q.response));
            lines.add("   Duration: " + q.durationMs + "ms");
        }
        lines.add(bar);

        try {
            Path path = Paths.get(filePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write voice report to " + filePath, e);
        }
        return filePath;
    }

    public void cleanup() {
        reportData.startTime = null;
        reportData.endTime = null;
        reportData.questions.clear();
        reportData.status = "Cleaned Up";
        isSpeaking = false;
    }

    public ReportData getReportData() {
        return reportData;
    }
}
