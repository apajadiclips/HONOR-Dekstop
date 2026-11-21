# HONOR Hybrid Desktop

Projek untuk HONOR Pad X9 (ELN-L09, MagicOS 7.1, Android 13, Snapdragon 685).
Modul pertama: **HONOR Debloat Manager** (`debloat/`) — debloat tahap *balanced*
dan sekatan iklan, **tanpa root, tanpa komputer, tanpa kehilangan data**.

## Cara ia berfungsi

Tool `hdm` berjalan dalam **Termux** pada tablet dan menghantar arahan `pm` /
`settings` melalui **Shizuku (`rish`)**, yang memberi akses tahap ADB shell
melalui Wireless Debugging. Tiada root diperlukan.

```
Termux (hdm)  ──rish──▶  Shizuku (uid shell)  ──▶  pm / settings pada tablet
```

## Persediaan (sekali sahaja)

1. Pasang **Shizuku** dan **Termux** (dari F-Droid / GitHub, bukan versi Play Store lama).
2. Settings > About tablet > ketik *Build number* 7 kali > Developer options >
   hidupkan **Wireless debugging**.
3. Buka Shizuku > *Start via Wireless debugging* > pair menggunakan kod.
   (Selepas reboot, Shizuku perlu dimulakan semula.)
4. Shizuku > *Use Shizuku in terminal apps* > *Export files* ke satu folder,
   kemudian dalam Termux:
   ```sh
   termux-setup-storage
   cp ~/storage/shared/<folder>/rish* $PREFIX/bin/ && chmod +x $PREFIX/bin/rish
   pkg install python git
   git clone https://github.com/apajadiclips/HONOR-Dekstop && cd HONOR-Dekstop
   ./hdm check        # mesti tunjuk uid=2000(shell)
   ```

## Aliran kerja selamat (ikut susunan)

| Langkah | Arahan | Perubahan? |
|---|---|---|
| 0. Semak kesihatan | `./hdm doctor` | Tiada |
| 1. Audit | `./hdm scan` | Tiada |
| 2. Backup asal | `./hdm backup --label asal` | Tiada |
| 3. Ukur prestasi | `./hdm measure sebelum` | Tiada |
| 4. Sekat iklan | `./hdm dns on` | Private DNS → `dns.adguard-dns.com` |
| 5. Debloat ringan | `./hdm disable <pakej>` | Satu pakej, boleh diundur |
| 6. Uji tablet beberapa hari | — | — |
| 7. (Pilihan) Buang | `./hdm uninstall <pakej>` | Hanya selepas disable |
| 8. Ukur semula | `./hdm measure selepas` | Tiada |

Undur: `./hdm restore <pakej>`, `./hdm restore --all`, `./hdm dns off`.
Jika tool ini sendiri rosak: `./hdm restore-manifest` memberi arahan yang boleh
ditaip terus dalam `rish`.

## Peraturan keselamatan yang dikuatkuasakan dalam kod

- **Tiada bulk uninstall, tiada auto-debloat.** Satu pakej setiap arahan.
- Setiap perubahan memerlukan anda **menaip semula nama pakej**.
- **Backup automatik** (snapshot penuh) sebelum setiap perubahan, dan setiap
  perubahan direkod dalam `journal.jsonl` beserta status sebelum/selepas.
- **Disable dahulu**: `uninstall` ditolak jika pakej belum di-disable.
- Uninstall hanya untuk pengguna 0 (`pm uninstall -k --user 0`); APK asal kekal
  dalam partition sistem, jadi `cmd package install-existing` boleh memulihkannya.
- **Kategori C ditolak terus**: System UI, Settings, launcher HONOR, Play Services,
  WebView, Package Installer, Permission Controller, Wi-Fi/Bluetooth/telephony,
  System Update, komponen Snapdragon, semua overlay & modul APEX, serta
  **papan kekunci dan launcher yang sedang digunakan** (dikesan secara langsung).
- **Kategori B** (semua aplikasi HONOR/Google/AOSP lain, dan *semua* pakej sistem
  yang tidak dikenali) memerlukan `--reviewed`.
- Aplikasi pengguna tidak disentuh.

## Kategori

| Fail | Kategori | Maksud |
|---|---|---|
| `debloat/policies/safe-candidates.json` | A | Calon selamat (masih perlu sahkan) |
| `debloat/policies/review-required.json` | B | Perlu semakan manual |
| `debloat/policies/protected-packages.json` | C | Jangan buang |

> **Penting:** senarai Kategori A *belum disahkan* pada ELN-L09. Langkah pertama
> ialah jalankan `./hdm scan --json > scan.json` dan kongsi hasilnya supaya
> package ID sebenar boleh dikelaskan dengan betul.

## Had yang perlu diketahui

- Backup ini merekod **status pakej**, bukan data dalaman aplikasi. Data
  aplikasi yang di-uninstall mungkin tidak pulih sepenuhnya — sebab itu
  `disable` diutamakan.
- Menyingkirkan pakej penting boleh menyebabkan bootloop. Senarai C dan peraturan
  di atas mengurangkan risiko itu, tetapi tidak menghapuskannya.
- DNS tidak dapat menyekat semua iklan (cth. YouTube, iklan dari domain yang sama
  dengan kandungan). Jika aplikasi gagal memuatkan: `./hdm dns off`.
- Data backup disimpan dalam `~/honor-debloat` (dalam Termux). Salin ke storan
  dalaman sebelum menyahpasang Termux:
  `cp -r ~/honor-debloat ~/storage/shared/Download/`

## Ujian

```sh
python3 -m unittest discover -s tests -t .
```
Ujian menggunakan peranti palsu (`tests/fake_device.py`) — tiada arahan sebenar dihantar.
