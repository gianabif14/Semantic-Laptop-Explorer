package id.ac.semanticlaptop;

public record Laptop(
        String model,
        String brand,
        String processor,
        int ramGb,
        int storageGb,
        String storageType,
        int priceIdr,
        float screenInch,
        String gpu,
        String operatingSystem,
        String category,
        String synonyms
) {
    public Laptop(
            String model,
            String brand,
            String processor,
            int ramGb,
            int storageGb,
            String storageType,
            int priceIdr,
            float screenInch,
            String gpu,
            String operatingSystem
    ) {
        this(model, brand, processor, ramGb, storageGb, storageType, priceIdr, screenInch, gpu, operatingSystem, "", "");
    }
}
