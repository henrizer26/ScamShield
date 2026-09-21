package com.group2.scamshield.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, weighted and explainable on-device SMS classifier.
 *
 * <p>The pipeline is fixed and has no learned parameters:
 *
 * <ol>
 *   <li><b>Trust the sender</b> - a message that arrived on a short code or on a
 *       registered operator, bank or authority sender ID is returned safe before
 *       any rule runs. Those senders are reserved by the networks and cannot be
 *       originated from a handset, so scoring their wording only ever produced
 *       false alarms on the OTPs and balance alerts they exist to send. See
 *       {@link WhitelistUtils#trustReason(String)} for what counts and why.</li>
 *   <li><b>Normalise</b> - Unicode folding, accent and invisible-character removal,
 *       lower-casing and whitespace collapsing, so one spelling of a word is enough.</li>
 *   <li><b>Canonicalise</b> - undo the two evasion tricks that break plain keyword
 *       matching: characters that look like letters ("p1n", "0TP") and separators
 *       pushed between letters ("p.i.n", "t u m a").</li>
 *   <li><b>Match</b> - every rule in the table runs its compiled pattern over the view
 *       of the message it was written for and reports the text that fired it.</li>
 *   <li><b>Score</b> - strong rules add their full weight, supporting rules share a
 *       capped budget so noise alone cannot convict, rules that describe genuine
 *       traffic subtract, and co-occurrence rules add the combinations that only
 *       appear in real scams.</li>
 *   <li><b>Classify</b> - the bounded score is compared with two fixed thresholds and
 *       returned together with the evidence that produced it.</li>
 * </ol>
 */
public final class ScamDetector {

    public static final String SAFE = "SAFE";
    public static final String SUSPICIOUS = "SUSPICIOUS";
    public static final String SCAM = "SCAM";

    /**
     * Audit identifier recorded when the trusted-sender gate, rather than the
     * rule table, produced the verdict. Named in the same space as the rules so
     * that {@link ScamResult#matchedRules} still answers "what decided this?"
     * with a single lookup.
     */
    public static final String TRUSTED_SENDER_RULE = "W01_TRUSTED_SENDER";

    public static final int SUSPICIOUS_THRESHOLD = 30;
    public static final int SCAM_THRESHOLD = 60;
    public static final int MAX_SCORE = 100;

    /** Highest weight that still counts as a supporting signal rather than a verdict. */
    public static final int WEAK_RULE_WEIGHT = 15;

    /** Supporting signals together may never contribute more than this. */
    public static final int WEAK_SIGNAL_LIMIT = 30;

    /** Weight added when two rules that are innocent apart fire together. */
    private static final int CONTEXT_WEIGHT = 20;

    private static final String SAFE_TYPE = "Safe Message";

    private static final String GENERAL = "Suspicious Message";
    private static final String MONEY = "Money Request";
    private static final String CREDENTIAL = "Credential Theft";
    private static final String PRIZE = "Prize / Fake Promotion";
    private static final String ACCOUNT = "Account Threat";
    private static final String HEALER = "Fake Traditional Healer";
    private static final String LOAN = "Fake Loan";
    private static final String JOB = "Fake Job Offer";
    private static final String PHISHING = "Phishing Link";
    private static final String IMPERSONATION = "Brand Impersonation";
    private static final String KNOWN_NUMBER = "Known Scam Number";
    private static final String GENUINE = "Genuine Traffic";

    private static final List<DetectionRule> RULES;

    static {
        List<DetectionRule> rules = new ArrayList<>();

        // -----------------------------------------------------------------
        // Structure: read the message as written, because links, amounts and
        // phone numbers lose their shape once characters are folded.
        // -----------------------------------------------------------------

        rules.add(rule("R01_KNOWN_NUMBER",
                "(?:^|\\D)(?:0614159000|0623638092|0784062546)(?:\\D|$)",
                70, "Sent from or points to a number already reported as a scam",
                KNOWN_NUMBER, DetectionRule.Field.SENDER_OR_MESSAGE));

        rules.add(rule("R11_UNSAFE_LINK",
                "\\b(?:bit\\.ly|tinyurl\\.com|goo\\.gl|t\\.co|is\\.gd|cutt\\.ly|rb\\.gy"
                        + "|ow\\.ly|buff\\.ly|shorturl\\.at|rebrand\\.ly|s\\.id|wa\\.me"
                        + "|api\\.whatsapp\\.com)\\b"
                        + "|https?://\\d{1,3}(?:\\.\\d{1,3}){3}"
                        + "|\\bxn--[a-z0-9-]{2,}"
                        + "|\\b[a-z0-9-]{2,}\\.(?:tk|ml|ga|cf|gq|xyz|top|buzz|click|link"
                        + "|icu|rest|zip|mov|fun|live)\\b",
                30, "Hides its destination behind a shortener, raw IP address or throwaway domain",
                PHISHING, DetectionRule.Field.MESSAGE));

        rules.add(rule("R12_BRAND_LOOKALIKE",
                "\\b[a-z0-9-]*(?:mpesa|m-pesa|vodacom|airtel|tigopesa|halopesa|halotel"
                        + "|selcom|azampesa|nmb|crdb|nbc|ttcl|tcra)[a-z0-9-]*"
                        + "\\.(?:com|net|org|info|online|site|shop|store|xyz|top|click"
                        + "|link|tk|ml|ga|cf|help|support)\\b",
                35, "Uses a look-alike web address built from a trusted brand name",
                IMPERSONATION, DetectionRule.Field.MESSAGE));

        rules.add(rule("R05_LINK",
                "\\b(?:https?://|www\\.)\\S+",
                12, "Contains a web link", GENERAL, DetectionRule.Field.MESSAGE));

        rules.add(rule("R13_PERSONAL_NUMBER",
                "(?:^|[^0-9+])(?:\\+?255|0)\\s?[67]\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{3}(?:[^0-9]|$)",
                15, "Points to a personal mobile number instead of an official service line",
                GENERAL, DetectionRule.Field.MESSAGE));

        rules.add(rule("R14_MONEY_AMOUNT",
                "\\b(?:tsh|tzs|ksh|kes|usd|sh|shilingi)\\b\\.?\\s*\\d[\\d,. ]{2,}"
                        + "|\\b\\d{3,9}\\s*/="
                        + "|\\b(?:elfu|laki|milioni)\\b"
                        + "|\\b\\d{1,3}(?:,\\d{3})+\\b",
                12, "States a specific amount of money", MONEY, DetectionRule.Field.MESSAGE));

        rules.add(rule("R15_SHOUTING",
                "\\b[A-Z]{4,}\\b(?:[^A-Za-z]{1,4}\\b[A-Z]{4,}\\b){2,}",
                8, "Written in shouted capitals to force attention",
                GENERAL, DetectionRule.Field.RAW));

        // -----------------------------------------------------------------
        // Wording: read the canonical view, so "p.i.n", "P1N" and "p i n" all
        // reach the same written rule.
        // -----------------------------------------------------------------

        rules.add(rule("R02_CREDENTIAL_REQUEST",
                // A bank warning you never to share a code uses the same verbs as
                // the thief asking for it, so negated instructions are excluded.
                "(?<!\\bdo not )(?<!\\bdon't )(?<!\\bdont )(?<!\\bnever )(?<!\\bcannot )"
                        + "\\b(?:tuma|nitumie|niambie|nipe|weka|ingiza|jaza|thibitisha|hakiki"
                        + "|share|send|enter|provide|confirm|verify|reply\\s+with|jibu\\s+na)\\b"
                        + "[^.!?]{0,45}"
                        + "\\b(?:pin|otp|password|passcode|nenosiri|namba\\s+ya\\s+siri"
                        + "|kitambulisho|cvv|username)\\b"
                        + "|\\b(?:pin|otp|password|nenosiri|namba\\s+ya\\s+siri|cvv)\\b"
                        + "[^.!?]{0,45}"
                        + "\\b(?:tuma|nitumie|niambie|nipe|weka|ingiza|jaza|thibitisha"
                        + "|share|send|enter|confirm)\\b",
                35, "Asks the recipient to hand over a PIN, OTP or password",
                CREDENTIAL, DetectionRule.Field.CANONICAL));

        rules.add(rule("R02B_SECRET_MENTION",
                "\\b(?:pin|otp|nenosiri|password|namba\\s+ya\\s+siri|cvv)\\b",
                12, "Mentions a secret code", CREDENTIAL, DetectionRule.Field.CANONICAL));

        rules.add(rule("R03_URGENCY",
                "\\b(?:haraka|mara\\s+moja|sasa\\s+hivi|hivi\\s+sasa|leo\\s+hii"
                        + "|dakika\\s+\\d+|saa\\s+\\d+\\s+zijazo|kabla\\s+ya\\s+(?:saa|leo|kesho)"
                        + "|muda\\s+(?:umeisha|unaisha|wako\\s+unaisha)"
                        + "|urgent(?:ly)?|immediately|right\\s+now|last\\s+chance|deadline"
                        + "|within\\s+\\d+\\s+(?:min|mins|minutes|hours)"
                        + "|expires?\\s+(?:today|soon|now))\\b",
                15, "Pressures the recipient to act before they can check",
                GENERAL, DetectionRule.Field.CANONICAL));

        rules.add(rule("R04_MONEY_REQUEST",
                "\\b(?:nitumie|unitumie|utanitumia|itume|niletee|tuma|peleka|hamisha"
                        + "|lipia|lipa|malipo|weka|deposit|send|transfer|pay)\\b"
                        + "[^.!?]{0,45}"
                        + "\\b(?:pesa|fedha|hela|shilingi|tsh|tzs|m-?pesa|tigopesa|mixx"
                        + "|airtel\\s?money|halopesa|ezypesa|lipa\\s+namba"
                        + "|namba\\s+ya\\s+wakala|agent\\s+number)\\b"
                        + "|\\b(?:pesa|fedha|hela|shilingi|tsh|tzs)\\b"
                        + "[^.!?]{0,45}"
                        + "\\b(?:nitumie|unitumie|utanitumia|itume|niletee|tuma|peleka"
                        + "|hamisha|lipia|lipa|deposit|send|transfer|pay)\\b",
                30, "Requests a money transfer or payment", MONEY, DetectionRule.Field.CANONICAL));

        rules.add(rule("R18_PAYMENT_REDIRECT",
                // "Send it to this Airtel number instead": the transfer instruction is
                // pointed at a mobile-money line named in the message itself, which is
                // how the wrong-number scam moves a payment it was never owed.
                "\\b(?:itume|utanitumia|unitumie|kunitumia|nitumie|niletee|tuma|tumia"
                        + "|peleka|weka)\\b"
                        + "[^.!?]{0,60}"
                        + "(?:\\b(?:vodacom|voda|tigo|tigopesa|airtel|halotel|halopesa"
                        + "|m-?pesa|mixx|yas|azampesa|selcom|ezypesa|wakala)\\b"
                        + "|\\bnamba\\s+(?:hii|hiyo)\\b)"
                        // The number is what makes this a redirect rather than a
                        // plain request for money, which R04 already scores.
                        + "[^.!?]{0,30}"
                        + "(?:\\+?255|0)\\s?[67]\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{3}",
                30, "Redirects a payment to a mobile-money number given in the message",
                // Read as written: canonicalising digits beside letters would
                // damage the very phone number this rule is looking for.
                MONEY, DetectionRule.Field.MESSAGE));

        rules.add(rule("R19_NAME_TO_EXPECT",
                "\\bjina\\b[^.!?]{0,12}"
                        + "\\b(?:litakuja|lije|litaleta|italeta|linakuja|litafika"
                        + "|litaonyesha|litaonesha|itaonyesha|ni|la)\\b"
                        + "|\\bjina\\b\\s*[(:]",
                15, "States the name the transfer should appear to come from",
                MONEY, DetectionRule.Field.CANONICAL));

        rules.add(rule("R06_PRIZE",
                // Only wording that asserts a win. "zawadi" and "hongera" are the
                // ordinary words for a gift and for congratulations, so they are
                // scored separately below instead of convicting on their own.
                "\\b(?:umeshinda|mmeshinda|imeshinda|umejishindia|imejishindia|mshindi"
                        + "|umechaguliwa|droo|bahati\\s+nasibu|promosheni|promotion|prize"
                        + "|winner|lottery|jackpot|bonanza|congratulations)\\b",
                30, "Claims a prize, draw or unexpected reward", PRIZE,
                DetectionRule.Field.CANONICAL));

        rules.add(rule("R06B_PRIZE_WORD",
                "\\b(?:zawadi|tuzo|tunzo|hongera)\\b",
                12, "Mentions a gift, award or congratulation", PRIZE,
                DetectionRule.Field.CANONICAL));

        rules.add(rule("R07_ACCOUNT_THREAT",
                "\\b(?:akaunti|account|laini|line|sim|simu\\s+yako|namba\\s+yako)\\b"
                        + "[^.!?]{0,50}"
                        + "\\b(?:imefungwa|itafungwa|imesimamishwa|itasimamishwa|imezuiwa"
                        + "|fungwa|blocked|suspended|locked|deactivated|disabled|expired?"
                        + "|verify|thibitisha|hakiki|sasisha|update)\\b",
                30, "Threatens the account or demands that it be verified", ACCOUNT,
                DetectionRule.Field.CANONICAL));

        rules.add(rule("R08_HEALER_PROMISE",
                "\\b(?:mganga|mtaalamu?\\s+wa\\s+(?:tiba|dawa|mizimu|mapenzi)"
                        + "|tiba\\s+ya\\s+asili|dawa\\s+ya\\s+(?:mapenzi|bahati|kufungua|kutajirika)"
                        + "|pete\\s+ya\\s+bahati|hirizi|mzimu|ramli|kafala"
                        + "|ku(?:ru|lu)disha\\s+(?:mke|mume|mpenzi)"
                        + "|kufunga\\s+(?:mke|mume)|uzazi\\s+wa\\s+kuchelewa)\\b",
                45, "Makes a supernatural or traditional-healer promise", HEALER,
                DetectionRule.Field.CANONICAL));

        rules.add(rule("R09_LOAN",
                "\\b(?:mkopo|mikopo|kopa|loan)\\b"
                        + "[^.!?]{0,50}"
                        + "\\b(?:haraka|bila\\s+dhamana|bila\\s+riba|papo\\s+hapo|instant"
                        + "|approved|guaranteed|ada|fee|malipo|usajili|register)\\b",
                25, "Advertises a loan that needs no checks", LOAN,
                DetectionRule.Field.CANONICAL));

        rules.add(rule("R16_ADVANCE_FEE",
                "\\b(?:ada|gharama|fee|charge|kiasi\\s+kidogo|processing)\\b"
                        + "[^.!?]{0,50}"
                        + "\\b(?:kupokea|kutoa|kuchukua|kufungua|release|receive"
                        + "|claim|process|unlock|withdraw)\\b"
                        + "|\\b(?:kupokea|kuchukua|claim|receive)\\b"
                        + "[^.!?]{0,50}"
                        + "\\b(?:ada|gharama|fee|lipa|lipia|pay)\\b",
                30, "Demands a fee before something is released", MONEY,
                DetectionRule.Field.CANONICAL));

        rules.add(rule("R17_JOB_OFFER",
                "\\b(?:ajira|nafasi\\s+za\\s+kazi|kazi\\s+(?:mpya|nzuri|ya\\s+mkataba)"
                        + "|job\\s+offer|vacanc(?:y|ies)|recruitment)\\b"
                        + "[^.!?]{0,50}"
                        + "\\b(?:bila\\s+usaili|haraka|tuma|lipa|ada|usajili|register"
                        + "|whatsapp|apply\\s+now)\\b",
                20, "Offers work that needs no interview or asks for money up front", JOB,
                DetectionRule.Field.CANONICAL));

        rules.add(rule("R10_INSTITUTION",
                "\\b(?:benki|bank|vodacom|airtel|tigo|yas|halotel|ttcl|tcra|nmb|crdb"
                        + "|nbc|exim|azania|equity|stanbic|serikali|government|mamlaka)\\b",
                10, "Speaks in the name of a bank, operator or authority", GENERAL,
                DetectionRule.Field.CANONICAL));

        rules.add(rule("R20_OFF_CHANNEL",
                "\\b(?:whatsapp|wasap|telegram|nipigie|pigia\\s+namba|call\\s+(?:me|now)"
                        + "|dm\\s+me)\\b",
                10, "Moves the conversation to a channel nobody can trace", GENERAL,
                DetectionRule.Field.CANONICAL));

        // -----------------------------------------------------------------
        // Negative rules: wording that genuine traffic uses and scams rarely
        // bother to copy. They pull the score back down.
        // -----------------------------------------------------------------

        rules.add(rule("S01_SECURITY_ADVICE",
                "\\b(?:usimpe|usitoe|usishirikishe|usimwambie|hatutakuomba|hatuombi"
                        + "|hatuwezi\\s+kuomba|never\\s+share|do\\s+not\\s+share|dont\\s+share"
                        + "|we\\s+will\\s+never\\s+ask)\\b"
                        + "[^.!?]{0,60}"
                        + "\\b(?:pin|otp|nenosiri|password|namba\\s+ya\\s+siri)\\b",
                -12, "Warns the recipient never to share a secret, the way real banks do",
                GENUINE, DetectionRule.Field.CANONICAL));

        rules.add(rule("S02_TRANSACTION_RECEIPT",
                "\\b(?:muamala\\s+namba|kumbukumbu\\s+namba|namba\\s+ya\\s+muamala"
                        + "|transaction\\s+(?:id|ref|reference|code)|receipt\\s+no"
                        + "|salio\\s+lako|new\\s+balance|umepokea\\s+(?:tsh|tzs)"
                        + "|umelipa\\s+(?:tsh|tzs)|umetuma\\s+(?:tsh|tzs))\\b",
                -20, "Carries the reference numbers of a completed transaction receipt",
                GENUINE, DetectionRule.Field.MESSAGE));

        // S03_TRUSTED_SENDER_ID used to sit here and deduct 20 for a registered
        // sender ID. It is gone rather than retuned: the same senders are now
        // decided before the table runs, by WhitelistUtils, so the rule could
        // never fire again, and a dead rule in a table meant to be audited is
        // worse than no rule. The senders it listed were carried over verbatim.

        RULES = Collections.unmodifiableList(rules);
    }

    private ScamDetector() {
    }

    private static DetectionRule rule(String id, String regex, int weight,
                                      String explanation, String category,
                                      DetectionRule.Field field) {
        return new DetectionRule(id, regex, weight, explanation, category, field);
    }

    /** The compiled rule table, exposed so the rules can be listed and audited. */
    public static List<DetectionRule> rules() {
        return RULES;
    }

    public static ScamResult detect(String message, String sender) {
        // Before any rule runs: operators, banks and authorities reach a handset
        // on senders no subscriber can originate, so their traffic is taken at
        // face value rather than scored. The short-circuit sits here, in the one
        // method every screen calls, so the live SMS receiver, the paste-and-scan
        // screen and a re-read of a stored row cannot reach different verdicts
        // for the same message.
        String trustReason = WhitelistUtils.trustReason(sender);
        if (trustReason != null) {
            return trustedSenderResult(message, sender, trustReason);
        }

        String normalizedMessage = normalize(message);
        String normalizedSender = normalize(sender);
        String canonicalMessage = canonicalize(message);
        DetectionRule.Input input = new DetectionRule.Input(
                message == null ? "" : message,
                normalizedMessage, canonicalMessage, normalizedSender);

        int strongScore = 0;
        int weakScore = 0;
        int suppression = 0;
        List<String> indicators = new ArrayList<>();
        Map<String, Integer> categoryScores = new LinkedHashMap<>();
        List<String> matchedIds = new ArrayList<>();

        for (DetectionRule rule : RULES) {
            String evidence = rule.match(input);
            if (evidence == null) {
                continue;
            }
            matchedIds.add(rule.getId());
            indicators.add(describe(rule.getExplanation(), evidence));

            if (rule.isSuppressor()) {
                suppression += -rule.getWeight();
                continue;
            }
            if (rule.isWeak()) {
                weakScore += rule.getWeight();
            } else {
                strongScore += rule.getWeight();
            }
            addCategoryScore(categoryScores, rule.getCategory(), rule.getWeight());
        }

        // A pile of supporting signals is not a verdict: cap what they may add.
        weakScore = Math.min(weakScore, WEAK_SIGNAL_LIMIT);

        strongScore += contextBonuses(matchedIds, normalizedSender, indicators, categoryScores);

        // Genuine wording can cancel supporting noise, but a direct request for
        // money or credentials still stands on its own.
        int appliedSuppression = strongScore > 0
                ? Math.min(suppression, weakScore)
                : suppression;

        int score = Math.max(0, Math.min(MAX_SCORE,
                strongScore + weakScore - appliedSuppression));

        String classification;
        String dangerLevel;
        if (score >= SCAM_THRESHOLD) {
            classification = SCAM;
            dangerLevel = RiskUi.SCAM;
        } else if (score >= SUSPICIOUS_THRESHOLD) {
            classification = SUSPICIOUS;
            dangerLevel = RiskUi.SUSPICIOUS;
        } else {
            classification = SAFE;
            dangerLevel = RiskUi.SAFE;
        }

        String category = strongestCategory(categoryScores);
        String displayType;
        if (SAFE.equals(classification)) {
            displayType = SAFE_TYPE;
        } else if (SUSPICIOUS.equals(classification)) {
            displayType = "Suspicious: " + category;
        } else {
            displayType = category + (category.endsWith("Scam") ? "" : " Scam");
        }

        return new ScamResult(classification, score, displayType, dangerLevel,
                indicators, matchedIds, canonicalMessage);
    }

    /**
     * The verdict for traffic that arrived on a sender the networks reserve.
     *
     * <p>Scored zero rather than merely reduced, because a partial deduction is
     * what the old {@code S03_TRUSTED_SENDER_ID} rule applied and it was not
     * enough: a genuine bank OTP carries a credential word, a deadline and an
     * amount, which together out-weighed the deduction and warned the user about
     * their own bank. The screen still gets a reason to show, so a safe verdict
     * here reads as a decision the app made rather than as nothing having matched.
     *
     * <p>Note that the whitelist table this registry comes from asks for the
     * opposite: it says a whitelisted sender should be "a recognized
     * institutional source, not proof that the message is safe", and that
     * content should still be analysed. That was put to the project owner and
     * declined - the sender decides here, and the message is not read. Do not
     * "fix" this by letting rules run behind the gate without asking again;
     * what it costs is recorded in {@code TRUSTED_SENDER_BLIND_SPOT}.
     */
    private static ScamResult trustedSenderResult(String message, String sender,
                                                  String trustReason) {
        List<String> indicators = new ArrayList<>();
        indicators.add(describe(trustReason, normalize(sender)));
        List<String> matched = new ArrayList<>();
        matched.add(TRUSTED_SENDER_RULE);
        return new ScamResult(SAFE, 0, SAFE_TYPE, RiskUi.SAFE,
                indicators, matched, canonicalize(message));
    }

    /**
     * Co-occurrence rules. Each pair below is ordinary on its own and close to
     * conclusive together, which is the part a single keyword cannot express.
     */
    private static int contextBonuses(List<String> matchedIds, String normalizedSender,
                                      List<String> indicators,
                                      Map<String, Integer> categoryScores) {
        int bonus = 0;

        if (matchedIds.contains("R06_PRIZE")
                && containsAny(matchedIds, "R04_MONEY_REQUEST", "R16_ADVANCE_FEE")) {
            bonus += addContext(matchedIds, indicators, categoryScores, "C01_PRIZE_PAYMENT",
                    CONTEXT_WEIGHT, "A prize claim is paired with a payment request", PRIZE);
        }
        if (matchedIds.contains("R02_CREDENTIAL_REQUEST")
                && containsAny(matchedIds, "R05_LINK", "R11_UNSAFE_LINK", "R12_BRAND_LOOKALIKE")) {
            bonus += addContext(matchedIds, indicators, categoryScores, "C02_CREDENTIAL_LINK",
                    CONTEXT_WEIGHT, "A credential request is paired with a web link", CREDENTIAL);
        }
        if (matchedIds.contains("R07_ACCOUNT_THREAT")
                && matchedIds.contains("R02_CREDENTIAL_REQUEST")) {
            bonus += addContext(matchedIds, indicators, categoryScores, "C03_ACCOUNT_CREDENTIAL",
                    CONTEXT_WEIGHT, "An account threat is paired with a credential request",
                    ACCOUNT);
        }
        if (isPersonalNumber(normalizedSender) && matchedIds.contains("R10_INSTITUTION")
                && containsAny(matchedIds, "R02_CREDENTIAL_REQUEST", "R07_ACCOUNT_THREAT",
                "R04_MONEY_REQUEST", "R05_LINK", "R11_UNSAFE_LINK", "R12_BRAND_LOOKALIKE")) {
            bonus += addContext(matchedIds, indicators, categoryScores, "C04_BRAND_MISMATCH",
                    CONTEXT_WEIGHT,
                    "Speaks for an institution but was sent from an ordinary mobile number",
                    IMPERSONATION);
        }
        if (matchedIds.contains("R18_PAYMENT_REDIRECT")
                && matchedIds.contains("R19_NAME_TO_EXPECT")) {
            bonus += addContext(matchedIds, indicators, categoryScores, "C06_REDIRECT_WITH_NAME",
                    CONTEXT_WEIGHT,
                    "A payment is redirected to a new number under a stated name", MONEY);
        }
        // Only when the win wording itself is absent: otherwise R06_PRIZE has
        // already scored this claim and C01 has already paired it with payment.
        if (matchedIds.contains("R06B_PRIZE_WORD")
                && !matchedIds.contains("R06_PRIZE")
                && containsAny(matchedIds, "R14_MONEY_AMOUNT", "R04_MONEY_REQUEST",
                "R16_ADVANCE_FEE")) {
            bonus += addContext(matchedIds, indicators, categoryScores, "C07_GIFT_WITH_MONEY",
                    CONTEXT_WEIGHT,
                    "A gift or award is paired with a sum of money to move", PRIZE);
        }
        if (matchedIds.contains("R03_URGENCY")
                && containsAny(matchedIds, "R05_LINK", "R11_UNSAFE_LINK")) {
            bonus += addContext(matchedIds, indicators, categoryScores, "C05_URGENT_LINK",
                    10, "A deadline is paired with a link to follow", GENERAL);
        }
        return bonus;
    }

    /**
     * The sentence shown on screen: the reason in plain words, followed by the
     * text from the message that produced it.
     *
     * <p>Rule identifiers and weights are deliberately absent. Someone deciding
     * whether to trust a message needs the reason, not the bookkeeping; the
     * identifiers stay available for auditing in {@link ScamResult#matchedRules},
     * which records every rule that fired, combination rules included.
     */
    private static String describe(String explanation, String evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return explanation;
        }
        return explanation + " - \"" + evidence + "\"";
    }

    private static boolean containsAny(List<String> matchedIds, String... ids) {
        for (String id : ids) {
            if (matchedIds.contains(id)) {
                return true;
            }
        }
        return false;
    }

    /** True for an ordinary subscriber line, as opposed to a short code or sender ID. */
    private static boolean isPersonalNumber(String normalizedSender) {
        String digits = normalizedSender.replaceAll("[^0-9+]", "");
        return digits.matches("(?:\\+?255|0)[67]\\d{8}");
    }

    /**
     * Records a fired combination rule: its identifier joins the audit trail and
     * its reason joins the list shown on screen. Safe to append to {@code
     * matchedIds} while the combinations are still being checked, because every
     * condition above tests for a plain rule, never for another combination.
     */
    private static int addContext(List<String> matchedIds, List<String> indicators,
                                  Map<String, Integer> categoryScores,
                                  String id, int weight, String explanation,
                                  String category) {
        matchedIds.add(id);
        indicators.add(describe(explanation, null));
        addCategoryScore(categoryScores, category, weight);
        return weight;
    }

    private static void addCategoryScore(Map<String, Integer> scores,
                                         String category, int weight) {
        scores.put(category, scores.getOrDefault(category, 0) + weight);
    }

    /**
     * Names the verdict after the heaviest specific category. Signals filed under
     * the generic bucket support a verdict but never name it, so a message is only
     * called "suspicious" in general terms when nothing specific was found.
     */
    private static String strongestCategory(Map<String, Integer> scores) {
        String strongest = GENERAL;
        int highest = -1;
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (GENERAL.equals(entry.getKey()) || GENUINE.equals(entry.getKey())) {
                continue;
            }
            if (entry.getValue() > highest) {
                highest = entry.getValue();
                strongest = entry.getKey();
            }
        }
        return strongest;
    }

    /** Converts equivalent input into a stable form used by every rule. */
    public static String normalize(String value) {
        return collapseSpacing(fold(value));
    }

    /** Case, accent and punctuation folding. Spacing is left untouched. */
    private static String fold(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
                // Accents and zero-width or bidirectional marks hide keywords.
                .replaceAll("[\\p{Mn}\\p{Cf}]", "")
                .toLowerCase(Locale.ROOT)
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace('\u2013', '-')
                .replace('\u2014', '-');
    }

    private static String collapseSpacing(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    /** Single characters strung out with separators: "p.i.n", "t u m a", "o-t-p". */
    private static final Pattern SPACED_RUN = Pattern.compile(
            "\\b[\\p{L}0134579](?:[\\s._*+-][\\p{L}0134579]){2,}\\b");

    /** Digits and symbols standing in for the letter beside them: "p1n", "0tp", "m0ney". */
    private static final Pattern LOOKALIKE = Pattern.compile(
            "(?<=\\p{L})[0134579@$|]|[0134579@$|](?=\\p{L})");

    private static final Pattern RUN_SEPARATORS = Pattern.compile("[\\s._*+-]");

    /**
     * Undoes the obfuscation that defeats plain keyword matching, so one written
     * rule covers every spelling of the same word.
     *
     * <p>Folding runs before and after the separators are removed, because the two
     * tricks are combined in practice: "p 1 n" needs the join first, "0tp" needs
     * the fold first. The join happens while the original spacing is still intact,
     * so "t u m a  p e s a" rejoins as two words rather than one.
     */
    private static String canonicalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String spaced = fold(text).replaceAll("[\\t\\r\\n\\u00a0]+", " ");
        return collapseSpacing(foldLookalikes(joinSpacedRuns(foldLookalikes(spaced))));
    }

    private static String joinSpacedRuns(String value) {
        Matcher matcher = SPACED_RUN.matcher(value);
        StringBuffer out = new StringBuffer(value.length());
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(
                    RUN_SEPARATORS.matcher(matcher.group()).replaceAll("")));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String foldLookalikes(String value) {
        Matcher matcher = LOOKALIKE.matcher(value);
        StringBuffer out = new StringBuffer(value.length());
        while (matcher.find()) {
            matcher.appendReplacement(out,
                    Matcher.quoteReplacement(letterFor(matcher.group().charAt(0))));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String letterFor(char symbol) {
        switch (symbol) {
            case '0': return "o";
            case '1': case '|': return "i";
            case '3': return "e";
            case '4': case '@': return "a";
            case '5': case '$': return "s";
            case '7': return "t";
            case '9': return "g";
            default: return String.valueOf(symbol);
        }
    }

    /** Verdict bucket for current stored history rows. */
    public static String riskLevelFor(String scamType) {
        if (scamType == null) {
            return RiskUi.SAFE;
        }
        String type = scamType.toLowerCase(Locale.ROOT);
        if (type.startsWith("suspicious:")) {
            return RiskUi.SUSPICIOUS;
        }
        if (type.startsWith("safe")) {
            return RiskUi.SAFE;
        }
        if (type.contains("scam") || type.contains("money") || type.contains("prize")
                || type.contains("credential") || type.contains("account")
                || type.contains("healer") || type.contains("loan")
                || type.contains("impersonation") || type.contains("job")) {
            return RiskUi.SCAM;
        }
        return RiskUi.SUSPICIOUS;
    }

    public static final class ScamResult {
        public final String classification;
        public final int score;
        public final String scamType;
        public final String dangerLevel;
        /**
         * What fired, in plain words, ready to show: a reason and the text that
         * produced it, with no rule identifier or weight. Index-aligned with
         * {@link #matchedRules}, which names the rule behind each entry.
         */
        public final List<String> indicators;

        /**
         * The audit trail: the identifier of every rule that fired, plain rules
         * and combination rules alike, in the order they were applied. Kept out
         * of {@link #indicators} so that changing what a screen shows never
         * costs the ability to say which rule was responsible.
         */
        public final List<String> matchedRules;

        /** The de-obfuscated text the wording rules read, e.g. "p.1.n" as "pin". */
        public final String canonicalMessage;

        private ScamResult(String classification, int score, String scamType,
                           String dangerLevel, List<String> indicators,
                           List<String> matchedRules, String canonicalMessage) {
            this.classification = classification;
            this.score = score;
            this.scamType = scamType;
            this.dangerLevel = dangerLevel;
            this.indicators = Collections.unmodifiableList(new ArrayList<>(indicators));
            this.matchedRules = Collections.unmodifiableList(new ArrayList<>(matchedRules));
            this.canonicalMessage = canonicalMessage;
        }

        public boolean shouldAlert() {
            return !SAFE.equals(classification);
        }
    }
}
