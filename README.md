# Semantic Laptop — Sistem Rekomendasi & Pencarian Spesifikasi Laptop Berbasis Ontologi

Aplikasi desktop dan web terintegrasi berbasis Java (Maven) dan Web Semantik untuk pencarian serta rekomendasi spesifikasi laptop menggunakan ontologi OWL/Turtle (`ontologi_laptop.ttl`) dan kueri SPARQL.

## Fitur Utama

* **Pencarian Semantik & Intent Detection:** Menganalisis maksud kueri pengguna (`QueryIntent`) dan memberikan skor relevansi pada setiap kandidat laptop (`ScoredLaptop`).
* **Basis Pengetahuan Ontologi:** Menggunakan file ontologi Turtle (`ontologi_laptop.ttl`) yang kompatibel dengan Protégé.
* **Antarmuka Hybrid (Desktop & Web):** Dilengkapi tampilan desktop Java Swing (`LaptopFrame`) sekaligus antarmuka web modern (`index.html`, `style.css`, `app.js`) yang disajikan melalui controller internal.
* **Konfigurasi Port Dinamis:** Mendeteksi dan mengatur port server secara otomatis (`DynamicPortConfig`) untuk mencegah bentrok port saat dijalankan.

## Teknologi yang Digunakan

* **Backend & Desktop:** Java (Maven), Java Swing, Apache Jena (RDF/OWL & SPARQL Engine)
* **Frontend (Embedded Web UI):** HTML5, CSS3, Vanilla JavaScript
* **Ontology & Knowledge Base:** Turtle (`.ttl`), Protégé, SPARQL

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
