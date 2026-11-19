# Semantic Laptop Explorer — Spring Boot Web + Apache Jena

Aplikasi web modern berbasis **Spring Boot 3** dan **Apache Jena 5.5.0** untuk mengeksplorasi ontologi spesifikasi laptop (`src/main/resources/ontology/laptops.ttl` dalam format RDF Turtle). Proyek ini merupakan hasil migrasi dari antarmuka desktop Java Swing (`JTable`) ke arsitektur web lokal (`localhost`) dengan dukungan **alokasi port dinamis** (otomatis *fallback* dari port `8080` ke port kosong berikutnya jika terjadi konflik port).

## Fitur Utama
- **Mekanisme Port Dinamis (`DynamicPortConfig`)**: Memeriksa ketersediaan port mulai dari `8080`. Jika port `8080` sedang dipakai layanan lain, sistem otomatis beralih ke port kosong berikutnya (`8081`, `8082`, dst. hingga `8180`, atau *ephemeral port* OS) tanpa error `Address already in use`.
- **Informasi Konsol & Auto-Open Browser**: Menampilkan banner status server beserta URL lengkap (`http://localhost:<port>`) di terminal dan otomatis membuka web browser ketika server siap.
- **Pencarian & Filter SPARQL Utuh**: Mempertahankan seluruh filter dari aplikasi `JTable` sebelumnya (Brand, RAM Minimum, Harga Maksimum) menggunakan `ParameterizedSparqlString`, ditambah filter kata kunci (*live search*) dan pengurutan kolom (*column sorting*) secara interaktif di tabel HTML/JS.

## Menjalankan Aplikasi

### Prasyarat
- JDK 17 atau lebih baru
- Apache Maven 3.8+ (atau melalui bundel Maven pada Apache NetBeans / IDE)

### Eksekusi via Terminal
```bash
# 1. Kompilasi proyek
mvn clean compile

# 2. Jalankan server web Spring Boot
mvn spring-boot:run
# Atau menggunakan exec-maven-plugin (kompatibel dengan tombol Run di NetBeans):
mvn exec:java
```

### Konfigurasi Port Khusus (Opsional)
Secara default aplikasi mencoba port `8080` terlebih dahulu. Anda juga dapat menentukan port awal secara manual:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.server.preferred-port=9000"
```

## Struktur Endpoint REST API
- `GET /` — Antarmuka Web Utama (Tabel & Filter Interaktif)
- `GET /api/brands` — Daftar seluruh brand (`:Brand`) dari ontologi RDF
- `GET /api/laptops?brand=&minRam=0&maxPrice=40000000&keyword=` — Eksekusi query SPARQL terparameterisasi dan mengembalikan data spesifikasi laptop dalam format JSON.


## Pembaruan Antarmuka (Clean Cream & Blue)
- Hasil pencarian ditampilkan dalam satu area berbentuk grid kartu.
- Setiap kartu dapat ditekan untuk membuka detail laptop dalam modal.
- Detail menyediakan tab Ringkasan, Spesifikasi, Sinonim & Relasi, serta Triple RDF.
- Foto ilustrasi dimuat dari Unsplash melalui URL eksternal agar ukuran ZIP tetap ringan.
- Tautan **Lihat foto produk di Google Images** membuka pencarian gambar berdasarkan merek dan model laptop. Gambar hasil Google tidak di-hotlink langsung karena URL thumbnail hasil pencarian tidak stabil dan dapat berubah.

Jalankan seperti biasa melalui NetBeans (Run Project) atau:
```bash
mvn spring-boot:run
```
Buka URL localhost yang dicetak pada Output/terminal.


## Catatan pembaruan katalog (November 2026)

- Sinonim keluarga produk (ROG, TUF Gaming, Vivobook, Zenbook, ExpertBook, ProArt) tidak lagi ditempelkan pada individu brand ASUS. Alias merek ASUS hanya berisi variasi nama merek.
- Pencarian kategori gaming menggunakan tipe ontologi `:GamingLaptop`, bukan sekadar kecocokan kata pada sinonim brand, prosesor, atau GPU. Model gaming kelas high-end yang relevan juga diberi tipe `:GamingLaptop`.
- Frontend tidak lagi memakai satu foto Unsplash generik untuk seluruh laptop. Foto produk eksternal yang telah dipetakan memakai CDN resmi ASUS untuk model ASUS yang sesuai. Untuk model lain yang belum memiliki foto langsung terverifikasi, aplikasi menampilkan placeholder yang menyebut model dan tautan pencarian Google Images khusus model tersebut; aplikasi tidak mengklaim placeholder sebagai foto produk.
- Foto eksternal tidak disimpan di ZIP sehingga ukuran project tetap kecil dan memerlukan koneksi internet saat gambar dimuat.


### Sumber foto resmi yang dipetakan

- ASUS Chromebook CX1 (CX1400): ASUS CDN, https://dlcdnwebimgs.asus.com/gain/64361e88-38ba-48d5-8367-502468cc5855/w800
- ASUS Vivobook Go 14 (E1404): ASUS Pressroom, https://press.asus.com/assets/w_640%2Ch_640/1e0abc93-461e-41d2-bb05-4f9e04ace884/Vivobook%20Go%2014_E1404F_Thin%20and%20light.png
- ASUS Vivobook 14 (X1404): ASUS CDN, https://dlcdnwebimgs.asus.com/gain/8e05f618-e30b-4f59-8970-09d828d23b9d/w800
- ASUS Vivobook 15 (X1504): ASUS CDN, https://dlcdnwebimgs.asus.com/gain/d76a41bc-780e-41ef-938b-4ee8ac4a5634/w800
- ASUS ExpertBook B1 (B1502): ASUS CDN, https://dlcdnwebimgs.asus.com/gain/695663ab-72d8-4bc0-9eaf-e971c33872d2/w692

Untuk model lain, tombol Google Images pada panel detail membuat pencarian berdasarkan nama model lengkap agar foto yang ditemukan tidak tertukar dengan model lain. URL CDN produsen tidak harus berakhiran `.jpg`; browser memuat berdasarkan tipe konten respons HTTP.
