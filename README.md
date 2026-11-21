# Offline Translator

A free translator for Android that works on the phone, without an internet connection once a language is downloaded.
No ads, no purchases, no account. Type or speak a sentence, or hold a conversation with someone across the phone.

- **54 languages**, both ways with English (other pairs go through English), with Mozilla's Bergamot translation models.
- **Speech** through the phone's own on-device recognizer and voices; nothing is recorded or sent.
- **The only network use** is Google Play downloading the language packs you choose.

Free software under the [GNU General Public License 3.0](LICENSE). Built with Kotlin Multiplatform and Compose
Multiplatform; it began as the Translator tool of PolyTools (specs 012 and 080 in `specs/`).

## Building

```bash
. ~/Android/env.sh                    # JAVA_HOME (Temurin 21) and ANDROID_HOME
scripts/fetch-language-packs.sh       # Portuguese and Spanish models; --all for every language
./gradlew check                       # tests, coverage, detekt, ktlint, network guard, architecture tests
./gradlew :androidApp:installDebug
```
