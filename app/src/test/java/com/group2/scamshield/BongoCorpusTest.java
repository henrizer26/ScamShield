package com.group2.scamshield;

import com.group2.scamshield.utils.ScamDetector;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Measures the detector against {@code bongo_scam.csv}, a labelled corpus of
 * Tanzanian SMS collected independently of the rule table.
 *
 * <p>This is the companion {@link ScamDetectorCorpusTest} asks for in its own
 * doc comment, and the two answer different questions. That corpus was written
 * alongside the rules, so it scores near 1.0 by construction and is a regression
 * guard. This one was not, so its numbers are the honest ones - and lower.
 *
 * <p>The corpus carries message bodies only, with no originating address, so
 * every sender-dependent rule sits idle here: {@code S03_TRUSTED_SENDER_ID},
 * {@code C04_BRAND_MISMATCH} and the sender half of {@code R01_KNOWN_NUMBER}.
 * Recall on a handset, where the sender is known, should be better than the
 * figure this test prints. See {@code app/src/test/resources/README.md} for the
 * rest of the caveats and for the scam families still missed.
 *
 * <p>The corpus is a test resource rather than an app resource, so it never
 * reaches the APK: the app has no use for it at runtime, only this test does.
 * That also keeps the test plain JVM, with no Android framework to emulate for
 * what is only a CSV read - the detector under test has no Android dependency
 * of its own.
 */
public class BongoCorpusTest {

    private static final String CORPUS = "/bongo_scam.csv";

    /**
     * Floors, not targets. Set just below the measured precision 1.000 and
     * recall 0.674 so an unrelated rule edit that quietly costs field accuracy
     * fails here instead of shipping. Raise them when the rules improve; never
     * lower them to make a change pass.
     */
    private static final double MIN_PRECISION = 0.97;
    private static final double MIN_RECALL = 0.65;

    /** How many missed and falsely flagged messages to print for diagnosis. */
    private static final int SAMPLE_LIMIT = 10;

    @Test
    public void corpusLoadsIntact() throws IOException {
        List<String[]> rows = loadCorpus();
        assertEquals("corpus row count changed", 1508, rows.size());

        int scam = 0;
        for (String[] row : rows) {
            assertTrue("unexpected label: " + row[0],
                    "scam".equals(row[0]) || "trust".equals(row[0]));
            assertTrue("empty message body", !row[1].trim().isEmpty());
            if ("scam".equals(row[0])) {
                scam++;
            }
        }
        assertEquals("scam row count changed", 1000, scam);
    }

    @Test
    public void detectorMeetsFieldAccuracyFloor() throws IOException {
        List<String[]> rows = loadCorpus();
        List<String> missed = new ArrayList<>();
        List<String> falseAlarms = new ArrayList<>();
        int truePositives = 0;
        int trueNegatives = 0;
        int confident = 0;

        for (String[] row : rows) {
            boolean labelledScam = "scam".equals(row[0]);
            // No sender travels with this corpus, so sender rules cannot fire.
            ScamDetector.ScamResult result = ScamDetector.detect(row[1], "");

            if (result.shouldAlert()) {
                if (labelledScam) {
                    truePositives++;
                    if (result.score >= ScamDetector.SCAM_THRESHOLD) {
                        confident++;
                    }
                } else {
                    record(falseAlarms, result.score, row[1]);
                }
            } else if (labelledScam) {
                record(missed, result.score, row[1]);
            } else {
                trueNegatives++;
            }
        }

        // The sample lists are capped for readability, so counts are derived from
        // the corpus totals rather than from their size.
        int labelledScams = countScams(rows);
        int labelledGenuine = rows.size() - labelledScams;
        int falsePositives = labelledGenuine - trueNegatives;
        int falseNegatives = labelledScams - truePositives;

        double precision = truePositives / (double) (truePositives + falsePositives);
        double recall = truePositives / (double) (truePositives + falseNegatives);
        double f1 = 2 * precision * recall / (precision + recall);
        double accuracy = (truePositives + trueNegatives) / (double) rows.size();

        String report = String.format(
                "bongo corpus: %d rows (%d scam, %d genuine), no sender%n"
                        + "TP=%d FP=%d TN=%d FN=%d%n"
                        + "precision=%.3f recall=%.3f f1=%.3f accuracy=%.3f%n"
                        + "reached SCAM threshold: %d of %d%n"
                        + "sample missed: %s%nsample false alarms: %s",
                rows.size(), labelledScams, labelledGenuine,
                truePositives, falsePositives, trueNegatives, falseNegatives,
                precision, recall, f1, accuracy,
                confident, labelledScams, missed, falseAlarms);
        System.out.println(report);

        assertTrue("precision fell below " + MIN_PRECISION + "\n" + report,
                precision >= MIN_PRECISION);
        assertTrue("recall fell below " + MIN_RECALL + "\n" + report,
                recall >= MIN_RECALL);
    }

    private static void record(List<String> sink, int score, String message) {
        if (sink.size() < SAMPLE_LIMIT) {
            sink.add(score + " | " + message);
        }
    }

    private static int countScams(List<String[]> rows) {
        int count = 0;
        for (String[] row : rows) {
            if ("scam".equals(row[0])) {
                count++;
            }
        }
        return count;
    }

    private static List<String[]> loadCorpus() throws IOException {
        String text = readResource();
        List<String[]> rows = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c != '"') {
                    field.append(c);
                } else if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = false;
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (c == '\n') {
                addRow(rows, fields, field);
            } else if (c != '\r') {
                field.append(c);
            }
        }
        addRow(rows, fields, field);

        if (!rows.isEmpty() && "Category".equalsIgnoreCase(rows.get(0)[0])) {
            rows.remove(0);
        }
        return Collections.unmodifiableList(rows);
    }

    private static void addRow(List<String[]> rows, List<String> fields,
                               StringBuilder field) {
        if (field.length() == 0 && fields.isEmpty()) {
            return;
        }
        fields.add(field.toString());
        field.setLength(0);
        if (fields.size() >= 2) {
            rows.add(new String[]{fields.get(0).trim(), fields.get(1)});
        }
        fields.clear();
    }

    private static String readResource() throws IOException {
        try (InputStream in = BongoCorpusTest.class.getResourceAsStream(CORPUS)) {
            if (in == null) {
                throw new IOException("missing test resource " + CORPUS);
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
