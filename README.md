# RETUR MATCHER — Android

Aplikasi Android awal untuk mencocokkan retur dengan data master Excel (.xlsx).

## Fitur MVP
- Impor data master Excel melalui pemilih file Android.
- Kolom wajib: nomor pesanan dan nomor resi.
- Alias header: `nomor pesanan`, `no pesanan`, `order`, `order number`, `nomor order`; serta `nomor resi`, `no resi`, `resi`, `awb`, `tracking number`, `kode resi`.
- Penyimpanan lokal menggunakan Room.
- Input manual nomor pesanan atau resi.
- Status cocok, tidak ditemukan, dan duplikat.
- Scan barcode dengan Google Code Scanner.
- Ekspor laporan hasil pemeriksaan ke Excel.
- Riwayat scan dan hitungan ringkas.

## Format Excel
Baris pertama berisi judul kolom. Contoh:

| Nomor Pesanan | Nomor Resi | Marketplace | Produk |
|---|---|---|---|
| 240001 | JX123456789 | Shopee | Kaos |
| 240002 | SPX987654321 | Shopee | Sepatu |

Simpan sebagai `.xlsx`.

## Build lokal
- Android Studio yang mendukung Android Gradle Plugin 8.7.3.
- JDK 17 dan Android SDK 35.
- Jalankan `gradle assembleDebug` dari folder proyek (Gradle 8.9).

## Build otomatis
Setiap push ke branch `main` menjalankan GitHub Actions untuk membangun APK debug. Hasilnya diunggah sebagai artifact `ReturMatcher-debug-apk` selama 14 hari.

## Catatan
Ini adalah fondasi MVP dan belum diverifikasi melalui build pada saat source diunggah. Pencocokan menggunakan kesamaan persis (exact match), bukan pencocokan sebagian. Pastikan nomor di Excel konsisten dan tidak berubah menjadi format angka ilmiah.