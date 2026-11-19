# Semantic Laptop — Sistem Rekomendasi & Pencarian Spesifikasi Laptop Berbasis Ontologi

Aplikasi desktop dan web terintegrasi berbasis Java (Maven) dan Web Semantik untuk pencarian serta rekomendasi spesifikasi laptop menggunakan ontologi OWL/Turtle (`ontologi_laptop.ttl`) dan kueri SPARQL[cite: 1, 2].

## Fitur Utama

* **Pencarian Semantik & Intent Detection:** Menganalisis maksud kueri pengguna (`QueryIntent`) serta pemahaman sinonim berbasis SKOS, lalu memberikan skor relevansi pada setiap kandidat laptop (`ScoredLaptop`)[cite: 1, 2].
* **Basis Pengetahuan Ontologi:** Menggunakan file ontologi Turtle (`ontologi_laptop.ttl`) yang memuat koleksi perangkat laptop & desktop berbasis RDF/OWL, SPARQL, serta kompatibel dengan Protégé[cite: 1, 2].
* **Antarmuka Hybrid (Desktop & Web):** Dilengkapi tampilan desktop Java Swing (`LaptopFrame`) sekaligus antarmuka web modern **Semantic Laptop Explorer** (`index.html`, `style.css`, `app.js`) yang disajikan melalui controller internal[cite: 1, 2].
* **Filter Multi-Kriteria & Pengurutan Instan:** Mendukung penyaringan kombinasi berdasarkan merek, kategori segmen, kapasitas RAM, rentang harga, serta pengurutan hasil secara dinamis[cite: 2].
* **Konfigurasi Port Dinamis:** Mendeteksi dan mengatur port server secara otomatis (`DynamicPortConfig`) untuk mencegah bentrok port saat dijalankan.

## Antarmuka Pengguna (Semantic Laptop Explorer)

![Tampilan Antarmuka Semantic Laptop Explorer](docs/screenshot.png)
*(Catatan: Sesuaikan path gambar di atas dengan lokasi penyimpanan tangkapan layar pada repositori Anda)*

Aplikasi ini menghadirkan antarmuka **Semantic Laptop Explorer** yang terhubung langsung dengan basis pengetahuan ontologi `ontology/ontologi_laptop.ttl` (memuat **101 total koleksi** perangkat laptop & desktop berbasis RDF/OWL, SPARQL, dan SKOS)[cite: 2].

### 1. Pencarian Semantik & Sinonim (Search Bar)
Pengguna dapat mengetikkan kata kunci bebas pada kolom pencarian (*"Cari model, brand, spek, atau sinonim..."*) untuk menemukan perangkat secara cerdas[cite: 2]:
* **Pencarian Spesifikasi & Model:** Mendukung pencarian langsung berdasarkan nama seri/model laptop, merek, prosesor, kapasitas RAM, penyimpanan, hingga jenis GPU[cite: 2].
* **Pemahaman Sinonim & Kebutuhan (SKOS & Query Intent):** Memanfaatkan relasi semantik pada ontologi sehingga pengguna dapat mencari berdasarkan konteks penggunaan atau istilah sinonim (misalnya kebutuhan *gaming*, *editing*, *kuliah*, atau *budget*) tanpa harus menghafal kode seri perangkat[cite: 1, 2].

### 2. Filter Multi-Kriteria & Pengurutan
Untuk mempersempit hasil eksplorasi secara instan, tersedia bilah filter kombinasi dan kontrol pengurutan[cite: 2]:
* **Filter Brand (`Semua brand`):** Menyaring katalog berdasarkan produsen perangkat tertentu (seperti ASUS, Acer, dll.)[cite: 2].
* **Filter Kategori (`Semua kategori`):** Mengelompokkan perangkat berdasarkan segmen kelasnya (seperti *Budget*, *Gaming*, *Ultrabook*, dsb.)[cite: 2].
* **Filter Kapasitas RAM (`Semua RAM`):** Memilih perangkat berdasarkan ukuran memori utama yang dibutuhkan[cite: 2].
* **Filter Rentang Harga (`Semua harga`):** Membatasi hasil pencarian sesuai anggaran belanja pengguna[cite: 2].
* **Tombol Reset:** Mengembalikan seluruh parameter pencarian dan filter ke kondisi awal dalam satu klik[cite: 2].
* **Pengurutan Hasil (`Urutkan`):** Mengatur urutan tampilan katalog, seperti berdasarkan **Harga terendah** maupun kriteria pengurutan lainnya[cite: 2].

### 3. Kartu Katalog & Tampilan Detail
Setiap hasil pencarian ditampilkan dalam bentuk kartu ringkas yang memuat label merek, kategori segmen, nama model, prosesor, ringkasan spesifikasi inti (RAM, Storage, Layar, GPU), serta harga dalam Rupiah[cite: 2]. Pengguna dapat mengklik **`Lihat detail ->`** pada kartu mana pun untuk membuka informasi spesifikasi lengkap beserta tautan foto resmi model perangkat tersebut[cite: 2].

## Teknologi yang Digunakan

* **Backend & Desktop:** Java (Maven), Java Swing, Apache Jena (RDF/OWL, SKOS & SPARQL Engine)[cite: 1, 2]
* **Frontend (Embedded Web UI):** HTML5, CSS3, Vanilla JavaScript
* **Ontology & Knowledge Base:** Turtle (`.ttl`), Protégé, SPARQL[cite: 1, 2]

## Struktur Proyek

```text
semantic-laptop/
├── docs/
│   ├── PROTEGE-QUICKSTART.md          # Panduan cepat membuka ontologi di Protégé
│   ├── SEMANTIC-SEARCH-IMPROVEMENTS.md# Catatan pengembangan algoritma pencarian
│   └── SPARQL-queries.rq              # Kumpulan kueri SPARQL referensi
├── src/
│   └── main/
│       ├── java/id/ac/semanticlaptop/
│       │   ├── Main.java              # Entry point utama aplikasi
│       │   ├── LaptopFrame.java       # Antarmuka GUI Desktop
│       │   ├── LaptopController.java  # REST/Web Controller untuk UI Web
│       │   ├── LaptopRepository.java  # Logika pemrosesan ontologi, SPARQL & scoring
│       │   ├── Laptop.java            # Model entitas Laptop
│       │   └── DynamicPortConfig.java # Konfigurasi port otomatis
│       └── resources/
│           ├── application.properties # Konfigurasi aplikasi
│           ├── ontology/
│           │   ├── ontologi_laptop.ttl# File basis pengetahuan ontologi laptop
│           │   └── catalog-v001.xml   # Katalog URI ontologi Protégé
│           └── static/
│               ├── index.html         # Halaman utama antarmuka web
│               ├── css/style.css      # Stylesheet antarmuka web
│               └── js/app.js          # Logika interaktif frontend
├── target/
│   └── semantic-laptop-desktop-1.0.0.jar # Executable JAR hasil build
└── pom.xml                            # Konfigurasi dependensi Maven
```

## Prasyarat

* **Java Development Kit (JDK):** Versi 11 atau lebih baru (disarankan JDK 17+)
* **Apache Maven:** Versi 3.6+ (opsional jika menjalankan langsung file `.jar`)
* **Protégé** *(Opsional)*: Jika ingin melihat atau memodifikasi file `ontologi_laptop.ttl` secara visual.

## Cara Menjalankan Aplikasi

### Opsi 1: Menjalankan Langsung File JAR (Tercepat)
Pastikan proyek sudah di-build, lalu jalankan file executable JAR yang tersedia di folder `target/`:

```bash
java -jar target/semantic-laptop-desktop-1.0.0.jar
```[cite: 1]

### Opsi 2: Build dan Jalankan dari Source Code (Maven)
1. Masuk ke direktori utama proyek[cite: 1]:
   ```bash
   cd semantic-laptop
   ```[cite: 1]
2. Lakukan kompilasi dan packaging menggunakan Maven[cite: 1]:
   ```bash
   mvn clean package
