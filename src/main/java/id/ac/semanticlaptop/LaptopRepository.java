package id.ac.semanticlaptop;

import jakarta.annotation.PreDestroy;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Repository
public class LaptopRepository implements AutoCloseable {
    private static final String NS = "http://example.org/laptop-ontology#";

    /**
     * Daftar kata hubung / kata umum (stopwords) bahasa Indonesia & Inggris
     * agar pencarian per kata seperti "untuk pelajar", "laptop harga pelajar",
     * atau "buat kerja kantoran" fokus pada kata kunci inti.
     */
    private static final Set<String> STOPWORDS = Set.of(
            "untuk", "buat", "bagi", "yang", "dengan", "dan", "atau", "di", "ke", "dari",
            "pada", "dalam", "adalah", "ini", "itu", "cari", "carikan", "rekomendasi",
            "tolong", "tampilkan", "pilihkan", "saya", "aku", "mau", "ingin", "butuh",
            "perlu", "cocok", "bagus", "terbaik", "paling", "harga", "spesifikasi", "spek",
            "maksimal", "max", "maximum", "minimal", "minimum", "sekitar", "kurang", "lebih", "sampai", "hingga", "budget", "anggaran",
            "murah", "termurah", "terjangkau", "ekonomis", "hemat",
            "tipe", "seri", "model", "kategori", "kelas", "laptop", "notebook", "komputer",
            "perangkat", "pc", "for", "with", "the", "a", "an", "in", "on", "of", "to", "and", "or"
    );

    private final OntModel model;

    public LaptopRepository() {
        this("ontology/ontologi_laptop.ttl");
    }

    @Autowired
    public LaptopRepository(@Value("${app.ontology.path:ontology/ontologi_laptop.ttl}") String resourcePath) {
        InputStream in = openResourceStream(resourcePath);
        if (in == null) {
            throw new IllegalStateException("Ontologi tidak ditemukan: " + resourcePath);
        }
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        try (InputStream stream = in) {
            RDFDataMgr.read(model, stream, Lang.TURTLE);
        } catch (Exception e) {
            throw new IllegalStateException("Gagal memuat file ontologi: " + resourcePath, e);
        }
    }

    private InputStream openResourceStream(String resourcePath) {
        try {
            Path directPath = Path.of(resourcePath);
            if (Files.exists(directPath) && Files.isRegularFile(directPath)) {
                return new FileInputStream(directPath.toFile());
            }
            Path resourceDirPath = Path.of("src", "main", "resources").resolve(resourcePath);
            if (Files.exists(resourceDirPath) && Files.isRegularFile(resourceDirPath)) {
                return new FileInputStream(resourceDirPath.toFile());
            }
        } catch (Exception ignored) {
        }
        return getClass().getClassLoader().getResourceAsStream(resourcePath);
    }

    public List<String> findBrands() {
        String q = """
            PREFIX : <http://example.org/laptop-ontology#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            SELECT DISTINCT ?label WHERE {
                { ?b :namaBrand ?label }
                UNION
                { ?b a :Brand ; rdfs:label ?label }
            } ORDER BY ?label
            """;
        List<String> out = new ArrayList<>();
        try (QueryExecution qe = QueryExecutionFactory.create(q, model)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                out.add(rs.next().getLiteral("label").getString());
            }
        }
        return out;
    }

    public List<Laptop> search(String brand, int minRam, int maxPrice) {
        return search(brand, minRam, maxPrice, "");
    }

    public List<Laptop> search(String brand, int minRam, int maxPrice, String keyword) {
        String q = """
            PREFIX : <http://example.org/laptop-ontology#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
            SELECT ?l ?model ?brandName ?cpu ?ram ?ramType ?capacity ?storageType ?price ?screen ?panel ?resolution ?gpu ?os ?category
                   (GROUP_CONCAT(DISTINCT STR(?syn); separator=", ") AS ?synonyms)
                   (GROUP_CONCAT(DISTINCT STR(?typeSyn); separator=", ") AS ?typeSynonyms)
                   (GROUP_CONCAT(DISTINCT STR(?rangeSyn); separator=", ") AS ?rangeSynonyms)
            WHERE {
                ?l :namaModel ?model ;
                   :diproduksiOleh ?b ;
                   :memilikiProcessor ?p ;
                   :memilikiRAM ?r ;
                   :memilikiStorage ?s ;
                   :harga ?price ;
                   :memilikiDisplay ?d ;
                   :memilikiGPU ?g ;
                   :memilikiOperatingSystem ?o .
                ?b :namaBrand ?brandName .
                ?p :namaProcessor ?cpu .
                ?r :kapasitasGB ?ram .
                OPTIONAL { ?r :jenisRAM ?ramType }
                ?s :kapasitasStorageGB ?capacity ;
                   :jenisStorage ?storageType .
                ?d :ukuranLayarInch ?screen .
                OPTIONAL { ?d :jenisPanel ?panel }
                OPTIONAL { ?d :namaResolusi ?resolution }
                ?g :namaGPU ?gpu .
                ?o :namaOS ?os .
                OPTIONAL { ?l :memilikiPriceRange ?pr . ?pr :namaRange ?category }
                OPTIONAL { ?l rdf:type ?laptopType . ?laptopType :memilikiSinonim ?typeSyn }
                OPTIONAL { ?l :memilikiPriceRange ?priceRangeNode . ?priceRangeNode :memilikiSinonim ?rangeSyn }

                OPTIONAL {
                    ?l (:diproduksiOleh|:memilikiProcessor|:memilikiRAM|:memilikiStorage|:memilikiDisplay|:memilikiGPU|:memilikiOperatingSystem|:memilikiPriceRange)? / rdf:type? / (:memilikiSinonim|skos:altLabel) ?syn .
                }

                FILTER(?ram >= ?minRam && ?price >= ?minPrice && ?price <= ?maxPrice)
                FILTER(?brandFilter = "" || LCASE(STR(?brandName)) = LCASE(?brandFilter))
            }
            GROUP BY ?l ?model ?brandName ?cpu ?ram ?ramType ?capacity ?storageType ?price ?screen ?panel ?resolution ?gpu ?os ?category
            ORDER BY ?price ?model
            """;

        String normalizedBrand = (brand == null || "Semua brand".equalsIgnoreCase(brand.trim()))
                ? ""
                : brand.trim();

        String rawKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        QueryIntent intent = parseQueryIntent(rawKeyword, normalizedBrand);
        if (normalizedBrand.isEmpty() && !intent.brand().isEmpty()) {
            normalizedBrand = intent.brand();
        }
        int effectiveMaxPrice = Math.min(maxPrice, intent.maxPrice());

        ParameterizedSparqlString p = new ParameterizedSparqlString(q);
        p.setLiteral("minRam", minRam);
        p.setLiteral("minPrice", (double) intent.minPrice());
        p.setLiteral("maxPrice", (double) effectiveMaxPrice);
        p.setLiteral("brandFilter", normalizedBrand);

        List<String> effectiveTokens = extractSearchTokens(intent.cleanedKeyword());

        List<ScoredLaptop> allMatches = new ArrayList<>();
        List<Laptop> fullTokenMatches = new ArrayList<>();

        try (QueryExecution qe = QueryExecutionFactory.create(p.asQuery(), model)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution s = rs.next();

                String modelName = getString(s, "model");
                String brandName = getString(s, "brandName");
                String cpu = getString(s, "cpu");
                int ram = parseIntSafe(s, "ram");
                String ramType = getString(s, "ramType");
                int capacity = parseIntSafe(s, "capacity");
                String storageType = getString(s, "storageType");
                int price = (int) Math.round(parseDoubleSafe(s, "price"));
                float screen = (float) parseDoubleSafe(s, "screen");
                String panel = getString(s, "panel");
                String resolution = getString(s, "resolution");
                String gpu = getString(s, "gpu");
                String os = getString(s, "os");
                String category = getString(s, "category");
                String synonyms = (getString(s, "synonyms") + ", " + getString(s, "typeSynonyms") + ", " + getString(s, "rangeSynonyms")).trim();

                // Kueri kategori gaming harus cocok dengan kelas kategori laptop,
                // bukan sekadar kata "gaming" yang kebetulan muncul pada sinonim brand/CPU/GPU.
                if (isGamingIntent(intent.cleanedKeyword())) {
                    var laptopResource = s.getResource("l");
                    var gamingClass = model.getResource(NS + "GamingLaptop");
                    if (laptopResource == null || !model.contains(laptopResource, org.apache.jena.vocabulary.RDF.type, gamingClass)) {
                        continue;
                    }
                }

                String displayResolution = resolution.isEmpty() ? panel : resolution;

                Laptop laptop = new Laptop(
                        modelName,
                        brandName,
                        cpu,
                        ram,
                        capacity,
                        storageType,
                        price,
                        screen,
                        gpu,
                        os,
                        category,
                        synonyms
                );

                if (effectiveTokens.isEmpty()) {
                    fullTokenMatches.add(laptop);
                } else {
                    int matchedWords = countMatchedTokens(laptop, ramType, displayResolution, effectiveTokens, intent.cleanedKeyword());
                    if (matchedWords == effectiveTokens.size()) {
                        fullTokenMatches.add(laptop);
                    } else if (matchedWords > 0) {
                        allMatches.add(new ScoredLaptop(laptop, matchedWords));
                    }
                }
            }
        }

        // Jika ada laptop yang cocok dengan seluruh kata kunci bermakna (setelah stopword dibuang),
        // tampilkan hasil tersebut. Jika tidak ada, fallback ke pencocokan per-kata (OR) berdasarkan skor tertinggi.
        if (!fullTokenMatches.isEmpty() || effectiveTokens.isEmpty()) {
            return fullTokenMatches;
        }

        // Untuk kueri dengan beberapa kriteria, jangan menampilkan hasil OR yang gagal
        // memenuhi seluruh token sebagai seolah-olah cocok. Alternatif parsial hanya
        // ditampilkan untuk kueri satu kata agar tidak menimbulkan false positive.
        if (effectiveTokens.size() > 1) {
            return Collections.emptyList();
        }
        allMatches.sort(Comparator.comparingInt(ScoredLaptop::score).reversed()
                .thenComparingInt(item -> item.laptop().priceIdr()));
        List<Laptop> partialOut = new ArrayList<>(allMatches.size());
        for (ScoredLaptop item : allMatches) {
            partialOut.add(item.laptop());
        }
        return partialOut;
    }

    private record QueryIntent(String brand, int minPrice, int maxPrice, String cleanedKeyword) {}

    /**
     * Memisahkan intent merek dan anggaran dari kata kunci. Harga tidak pernah dipakai
     * sebagai petunjuk kategori laptop; "ASUS 4 juta" tetap berarti brand ASUS + batas harga.
     */
    private boolean isGamingIntent(String keyword) {
        List<String> tokens = extractSearchTokens(keyword);
        return tokens.stream().anyMatch(t -> Set.of("gaming", "game", "gamer", "esports", "esport").contains(t));
    }

    private QueryIntent parseQueryIntent(String keyword, String selectedBrand) {
        String cleaned = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        String detectedBrand = "";
        if (selectedBrand == null || selectedBrand.isBlank()) {
            Map<String, List<String>> aliases = new LinkedHashMap<>();
            aliases.put("ASUS", List.of("asus", "asus laptop", "laptop asus", "notebook asus", "merk asus", "merek asus"));
            aliases.put("Acer", List.of("acer", "laptop acer", "notebook acer", "merk acer", "merek acer"));
            aliases.put("Lenovo", List.of("lenovo", "laptop lenovo", "notebook lenovo", "leno"));
            aliases.put("HP", List.of("hewlett packard", "hewlett-packard", "laptop hp", "notebook hp", "merk hp", "merek hp", " hp "));
            aliases.put("Dell", List.of("dell", "laptop dell", "notebook dell"));
            aliases.put("Apple", List.of("apple", "macbook", "laptop apple"));
            aliases.put("MSI", List.of("msi", "laptop msi", "notebook msi"));
            aliases.put("Microsoft", List.of("microsoft", "surface", "laptop surface"));
            for (Map.Entry<String, List<String>> entry : aliases.entrySet()) {
                for (String alias : entry.getValue()) {
                    String padded = " " + cleaned + " ";
                    if (padded.contains(" " + alias + " ") || cleaned.equals(alias)) {
                        detectedBrand = entry.getKey();
                        cleaned = cleaned.replace(alias, " ");
                        break;
                    }
                }
                if (!detectedBrand.isEmpty()) break;
            }
        }

        int parsedMin = 0;
        int parsedMax = 100_000_000;
        java.util.regex.Pattern pricePattern = java.util.regex.Pattern.compile(
                "(?i)(?:rp\\s*)?(\\d{1,3}(?:[.,]\\d{3})+|\\d+)\\s*(juta(?:an)?|jt|million|m)?");
        java.util.regex.Matcher matcher = pricePattern.matcher(cleaned);
        List<Long> amounts = new ArrayList<>();
        List<String> matchedPriceText = new ArrayList<>();
        while (matcher.find()) {
            String number = matcher.group(1);
            String unit = matcher.group(2);
            String digits = number.replace(".", "").replace(",", "");
            try {
                long value = Long.parseLong(digits);
                if (unit != null && !unit.isBlank()) {
                    if (unit.toLowerCase(Locale.ROOT).startsWith("j") || unit.equalsIgnoreCase("m") || unit.equalsIgnoreCase("million")) value *= 1_000_000L;
                } else if (value < 100_000L) {
                    continue; // angka seperti generasi CPU bukan nominal harga
                }
                amounts.add(value);
                matchedPriceText.add(matcher.group());
            } catch (NumberFormatException ignored) { }
        }
        if (!amounts.isEmpty()) {
            long first = amounts.get(0);
            boolean isRange = amounts.size() > 1 && (cleaned.contains(" sampai ") || cleaned.contains(" hingga ") || cleaned.contains("-") || cleaned.contains("antara "));
            boolean isMinimum = cleaned.matches("(?s).*(minimal|min\\.|paling sedikit|lebih dari|di atas|ke atas).*" );
            boolean isApprox = cleaned.contains("sekitar") || cleaned.contains("kurang lebih") || cleaned.contains("kira-kira");
            if (isRange) {
                parsedMin = (int) Math.min(Integer.MAX_VALUE, Math.min(first, amounts.get(1)));
                parsedMax = (int) Math.min(Integer.MAX_VALUE, Math.max(first, amounts.get(1)));
            } else if (isMinimum) {
                parsedMin = (int) Math.min(Integer.MAX_VALUE, first);
            } else if (isApprox) {
                parsedMin = (int) Math.max(0, Math.round(first * 0.90d));
                parsedMax = (int) Math.min(Integer.MAX_VALUE, Math.round(first * 1.10d));
            } else {
                // Nominal tanpa operator diperlakukan sebagai batas anggaran maksimum.
                parsedMax = (int) Math.min(Integer.MAX_VALUE, first);
            }
            for (String priceText : matchedPriceText) cleaned = cleaned.replace(priceText, " ");
        }
        cleaned = cleaned.replaceAll("(?i)\\b(rp|idr|juta(?:an)?|jt|million|budget|anggaran|maksimal|max|maximum|minimal|minimum|sekitar|kurang|lebih|dari|sampai|hingga|ke atas|di bawah)\\b", " ")
                .replaceAll("\\s+", " ").trim();
        return new QueryIntent(detectedBrand, parsedMin, parsedMax, cleaned);
    }

    /**
     * Memecah kalimat pencarian menjadi kata-kata (per kata) dan membuang kata sambung (stopwords)
     * seperti "untuk", "buat", "yang", "harga", "laptop" agar pencarian "untuk pelajar"
     * fokus pada kata kunci bermakna ("pelajar").
     */
    public static List<String> extractSearchTokens(String rawKeyword) {
        if (rawKeyword == null || rawKeyword.isBlank()) {
            return Collections.emptyList();
        }
        String[] rawTokens = rawKeyword.toLowerCase(Locale.ROOT)
                .split("[^a-z0-9.+\\-]+");

        List<String> allTokens = new ArrayList<>();
        List<String> filteredTokens = new ArrayList<>();

        for (String t : rawTokens) {
            String clean = t.trim();
            if (clean.isEmpty()) continue;
            allTokens.add(clean);
            if (!STOPWORDS.contains(clean)) {
                filteredTokens.add(clean);
            }
        }

        // Jika seluruh kata yang diketik adalah stopword (misal user hanya mengetik "laptop"),
        // gunakan token asli agar tetap menampilkan hasil.
        return filteredTokens.isEmpty() ? allTokens : filteredTokens;
    }

    private int countMatchedTokens(
            Laptop l,
            String ramType,
            String displayResolution,
            List<String> effectiveTokens,
            String rawKeyword
    ) {
        String searchableCorpus = String.join(" | ",
                l.model(),
                l.brand(),
                l.processor(),
                l.ramGb() + "gb",
                l.ramGb() + " gb",
                ramType,
                l.storageGb() + "gb",
                l.storageGb() + " gb",
                l.storageType(),
                l.screenInch() + " inch",
                displayResolution,
                l.gpu(),
                l.operatingSystem(),
                l.category(),
                l.synonyms()
        ).toLowerCase(Locale.ROOT);

        if (searchableCorpus.contains(rawKeyword)) {
            return effectiveTokens.size();
        }

        int matched = 0;
        for (String token : effectiveTokens) {
            if (searchableCorpus.contains(token)) {
                matched++;
            }
        }
        return matched;
    }

    private String getString(QuerySolution s, String varName) {
        Literal lit = s.getLiteral(varName);
        return lit == null ? "" : lit.getString();
    }

    private int parseIntSafe(QuerySolution s, String varName) {
        Literal lit = s.getLiteral(varName);
        if (lit == null) return 0;
        return (int) Math.round(Double.parseDouble(lit.getLexicalForm()));
    }

    private double parseDoubleSafe(QuerySolution s, String varName) {
        Literal lit = s.getLiteral(varName);
        if (lit == null) return 0.0;
        return Double.parseDouble(lit.getLexicalForm());
    }

    private record ScoredLaptop(Laptop laptop, int score) {}

    @PreDestroy
    @Override
    public void close() {
        model.close();
    }
}
