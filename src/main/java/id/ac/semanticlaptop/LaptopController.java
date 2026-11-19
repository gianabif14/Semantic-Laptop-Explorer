package id.ac.semanticlaptop;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LaptopController {
    private final LaptopRepository repository;

    public LaptopController(LaptopRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/brands")
    public List<String> getBrands() {
        return repository.findBrands();
    }

    @GetMapping("/laptops")
    public List<Laptop> searchLaptops(
            @RequestParam(name = "brand", defaultValue = "") String brand,
            @RequestParam(name = "minRam", defaultValue = "0") int minRam,
            @RequestParam(name = "maxPrice", defaultValue = "100000000") int maxPrice,
            @RequestParam(name = "keyword", defaultValue = "") String keyword
    ) {
        if (minRam < 0) {
            throw new IllegalArgumentException("RAM minimum tidak boleh bernilai negatif.");
        }
        if (maxPrice < 0) {
            throw new IllegalArgumentException("Harga maksimum harus berupa angka Rupiah positif tanpa titik/koma.");
        }
        return repository.search(brand, minRam, maxPrice, keyword);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String param = ex.getName();
        String msg = "maxPrice".equals(param)
                ? "Harga maksimum harus berupa angka Rupiah bulat tanpa titik/koma."
                : "Parameter '" + param + "' tidak valid.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Query SPARQL gagal: " + ex.getMessage()));
    }
}
