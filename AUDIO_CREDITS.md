# Batch 7 audio sources and licenses

The full source patch intentionally does **not** silently substitute a synthetic recording for a real cat. Run `install-cozy-audio.ps1` to download verified real-world animal sounds from Wikimedia Commons before committing.

- `real_cat_meow.ogg` — **Meow of a pleading cat.oga**, Heismark (2010), public domain. https://commons.wikimedia.org/wiki/File:Meow_of_a_pleading_cat.oga
- `cute_cat_v7.ogg` — **Maullido de gata hembra joven.ogg**, George Miquilena (2012), CC0. https://commons.wikimedia.org/wiki/File:Maullido_de_gata_hembra_joven.ogg
- `purr_real.ogg` — **Purring cat bertie.ogg**, jim_mowatt (2007), public domain. https://commons.wikimedia.org/wiki/File:Purring_cat_bertie.ogg

- `sigh_real.ogg` — **Hm and sigh.ogg**, ezwa (2008), public domain. https://commons.wikimedia.org/wiki/File:Hm_and_sigh.ogg
- `yawn_real.ogg` — **Yawning 1.ogg**, ezwa, public domain. https://commons.wikimedia.org/wiki/File:Yawning_1.ogg

No Pixabay recording is copied into this patch. The user may still replace sounds with licensed files of their choice.

`purr_fallback.wav`, `bag.wav`, `kibble.wav`, `toy.wav`, and `sigh.wav` are small synthesized temporary/offline fallback effects, not recordings of actual pets/humans. Prefer the downloaded real purr. `cute_cat_meow.wav` remains as the preexisting compatibility fallback if the new cute recording is unavailable.

Android audio is embedded in the APK to allow offline alarms; meow service playback is independent from the WebView interaction audio.
