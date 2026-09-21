# bongo_scam.csv — held-out evaluation corpus

A labelled corpus of Tanzanian SMS traffic, used by `BongoCorpusTest` to measure
the detector against messages **it was not written from**.

This is the corpus `ScamDetectorCorpusTest` asks for in its own doc comment. The
two are not interchangeable and both are kept on purpose:

| | `ScamDetectorCorpusTest` | `BongoCorpusTest` (this file) |
|---------|---------------------------|--------------------------------|
| Source | written alongside the rules | collected separately |
| Size | 20 scam + 20 genuine | 1000 scam + 508 genuine |
| Proves | no rule edit breaks a known shape | how the rules do on unseen wording |
| Score | ~1.0 by construction | see floors below |

## Shape

- 1508 rows, header `Category,Sms`, RFC-4180 quoting, UTF-8.
- Labels: `scam` (1000) and `trust` (508). `trust` is everyday traffic.
- 1377 distinct message bodies; some rows repeat because the same scam was
  received more than once. Duplicates are **kept** — they weight the metrics
  toward the shapes that actually circulate.

## Caveats — read before quoting any number from this

1. **No sender column.** The corpus carries message bodies only, so the test
   passes an empty sender. Every sender-dependent rule sits idle in this
   measurement: the trusted-sender gate, `C04_BRAND_MISMATCH`, and the sender
   half of `R01_KNOWN_NUMBER`. Real recall on a handset, where the sender is
   known, should be higher than the figure here.

   An empty sender is never a trusted one, so the gate cannot silence anything
   in this corpus. That is deliberate — it keeps these figures comparable with
   the ones measured before the gate existed — but it also means this corpus
   says nothing about what the gate costs. `TRUSTED_SENDER_BLIND_SPOT` in
   `ScamDetectorCorpusTest` is where that cost is recorded.
2. **Labels are the collector's, not a security analyst's.** `trust` means "not
   reported as a scam", which is not the same as "verified benign".
3. **Mojibake.** A few rows contain replacement characters where non-breaking
   spaces were lost in collection. They are left as-is; a detector that only
   works on clean input is not much use on real SMS.
4. Not a training set. The detector has no learned parameters — this corpus
   measures the hand-written rule table, it does not fit it.

## Floors

`BongoCorpusTest` asserts precision ≥ 0.97 and recall ≥ 0.65 against measured
values of **precision 1.000, recall 0.674, F1 0.805** (1508 rows, no sender).

The gap between the two corpora is the point of keeping this one. Measured
before the rules were revised against it: precision 0.982, **recall 0.384**.

## Known gaps this corpus exposes

Still missed, in rough order of volume — each is a scam family the rule table
does not describe yet, not a tuning problem:

- **Occult / Freemason recruitment** — `666 KARIBU FREEMASON UTIMIZE NDOTO...`.
  `R08_HEALER_PROMISE` covers traditional healers but not this vocabulary.
- **Healer ads written without `ya`** — `MZEE X WA TIBA ASILI. MALI. PETE.`
  `R08_HEALER_PROMISE` requires `tiba ya asili`.
- **`x`-for-`s` obfuscation** — `Xorry nipo xafari nina xhida... unixaidie`.
  `canonicalize()` folds digit look-alikes and inserted separators, but not
  letter-for-letter substitution. Same class of evasion as `j'ina` for `jina`,
  where a single apostrophe defeats the word boundary.
- **Landlord / new-number impersonation** — `mimi mwenye nyumba wako, hii namba
  yangu mpya ya Halotel, kodi yangu...`. No money verb, no link; the tell is a
  claimed relationship plus a new number.
