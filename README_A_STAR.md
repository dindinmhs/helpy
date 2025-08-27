# Helpy - Aplikasi Navigasi dengan Algoritma A\*

## Deskripsi Project

Helpy adalah aplikasi navigasi Android yang mengimplementasikan algoritma **A\* (A-Star)** untuk mencari rute tercepat. Project ini dibuat untuk mata kuliah **Kecerdasan Buatan** dengan fokus pada implementasi algoritma pencarian optimal.

## Implementasi Algoritma A\*

### Konsep A\* (A-Star)

A\* adalah algoritma pencarian graf yang mencari jalur terpendek dari node awal ke node tujuan. Algoritma ini menggunakan fungsi evaluasi:

```
f(n) = g(n) + h(n)
```

Dimana:

- **g(n)**: Biaya aktual dari start ke node n
- **h(n)**: Estimasi biaya heuristik dari node n ke goal (menggunakan jarak Haversine)
- **f(n)**: Estimasi total biaya jalur melalui node n

### Keunggulan A\*

1. **Optimal**: Selalu menemukan jalur terpendek jika heuristik admissible
2. **Complete**: Akan menemukan solusi jika solusi ada
3. **Efficient**: Lebih cepat dari Dijkstra karena menggunakan heuristik yang mengarahkan pencarian

### Struktur Implementasi

```
domain/
├── pathfinding/
│   └── AStarPathfinder.kt          # Implementasi algoritma A*
├── usecase/
│   └── GetRouteUseCase.kt          # Use case untuk pencarian rute
└── model/
    ├── Graph.kt                    # Model graf jalan
    ├── GraphNode.kt                # Node dalam graf (persimpangan/titik jalan)
    └── GraphEdge.kt                # Edge dalam graf (segmen jalan)

data/
├── repository/
│   ├── RouteRepositoryImpl.kt      # Implementasi repository untuk data jalan
│   └── GraphBuilder.kt             # Builder untuk membuat graf dari data OSM
└── remote/
    └── OverpassApiService.kt       # Service untuk mengambil data jalan dari OpenStreetMap
```

### Algoritma A\* - Langkah demi Langkah

1. **Inisialisasi**:

   - Buat open set (priority queue) dan closed set
   - Masukkan start node ke open set dengan g(start) = 0

2. **Loop Utama**:

   - Ambil node dengan f-score terendah dari open set
   - Jika node adalah goal, rekonstruksi path dan return
   - Pindahkan node ke closed set

3. **Eksplorasi Tetangga**:

   - Untuk setiap tetangga yang belum di closed set:
   - Hitung tentative g-score = g(current) + weight(current, neighbor)
   - Jika ini jalur yang lebih baik, update g-score dan tambahkan ke open set

4. **Rekonstruksi Path**:
   - Ikuti parent pointers dari goal ke start
   - Return list node yang merepresentasikan jalur optimal

### Fungsi Heuristik

Menggunakan **jarak Haversine** untuk menghitung jarak langsung antara dua titik koordinat geografis:

```kotlin
private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0 // Earth's radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}
```

### Sumber Data

- **OpenStreetMap**: Data jalan dan persimpangan
- **Overpass API**: Query untuk mengambil data jalan dalam bounding box
- **Clean Architecture**: Pemisahan concerns antara data, domain, dan UI

### Fitur Aplikasi

1. **Peta Interaktif**: Menggunakan OSMDroid untuk menampilkan peta
2. **Pencarian Rute**: Tap untuk memilih tujuan, algoritma A\* mencari rute optimal
3. **Visualisasi Rute**: Menampilkan rute sebagai polyline biru di peta
4. **Navigasi**: Bottom navigation dengan fitur Peta, SOS, dan Profile
5. **Real-time Location**: Menggunakan GPS untuk mendapatkan lokasi pengguna

### Teknologi Yang Digunakan

- **Kotlin**: Bahasa pemrograman utama
- **Jetpack Compose**: UI framework modern
- **OSMDroid**: Library untuk peta OpenStreetMap
- **Retrofit**: HTTP client untuk API calls
- **Navigation Compose**: Navigasi antar screen
- **Material Design 3**: Design system

### Catatan Pengembangan

- Algoritma A\* dioptimalkan untuk graf jalan dengan implementasi priority queue
- Heuristik distance menggunakan jarak Haversine yang admissible untuk routing geografis
- Graph building menggunakan data real dari OpenStreetMap melalui Overpass API
- Error handling untuk kasus dimana tidak ada rute yang ditemukan

## Cara Menjalankan

1. Clone repository
2. Buka project di Android Studio
3. Build dan run aplikasi
4. Berikan permission lokasi
5. Tap pada peta untuk memilih tujuan
6. Tekan tombol route untuk menjalankan algoritma A\*

---

**Project untuk Mata Kuliah Kecerdasan Buatan**  
**Implementasi Algoritma A\* untuk Pencarian Rute Optimal**
