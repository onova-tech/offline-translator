# Quickstart: validating spec 080

## 1. On the computer

```bash
python3 scripts/update-language-catalog.py --check   # the catalog matches the registry file it was made from
scripts/fetch-language-packs.sh                       # Portuguese and Spanish, for the tests
scripts/ci.sh check                                   # domain, screens, fit, audit, rules
scripts/ci.sh device                                  # a11y and e2e, with only those two packs pushed
scripts/ci.sh engine --all                            # every language, five packs at a time (fetches them all)
```

Expected: all green; `engine --all` translates a sentence both ways with English for each of the 53 languages.

## 2. On the owner's phone (with consent)

1. Install through local testing with the packs for **pt, es and two new languages** (for example ja and fr), not all.
2. Translator → From → **Other languages…**: the page lists 54 languages; search "jap" finds Japanese; check Japanese
   and French, uncheck Spanish; with two left, they can't be unchecked and the note says why.
3. Back: both lists show the checked languages; Japanese shows its size; choose Portuguese → Japanese: the offer
   downloads it; in airplane mode the translation appears with "Translated through English".
4. Conversation: each half's list ends with "Other languages…"; the far half's is turned.
5. Uncheck Japanese: Settings → Offline languages still lists its pack; Delete frees it.
6. Arabic (if pushed): typed text runs right to left.
