import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final HttpClient httpClient;
    private final Semaphore semaphore;
    private final long intervalInMillis;
    private final String token;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.semaphore = new Semaphore(requestLimit);
        this.intervalInMillis = timeUnit.toMillis(1);
        startSemaphoreRefillThread(requestLimit);

        this.token = "token";
    }

    private void startSemaphoreRefillThread(int requestLimit) {
        new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(intervalInMillis);
                    semaphore.release(requestLimit - semaphore.availablePermits());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    public void createDocument(Document document, String signature) throws IOException, InterruptedException {
        semaphore.acquire();

        String json = buildJson(document, signature);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .timeout(Duration.ofMinutes(1))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to create document: " + response.body());
        }
    }

    private String buildJson(Document document, String signature) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        jsonBuilder.append("\"description\":{");
        jsonBuilder.append("\"participantInn\":\"").append(document.participantInn).append("\"},");
        jsonBuilder.append("\"doc_id\":\"").append(document.docId).append("\",");
        jsonBuilder.append("\"doc_status\":\"").append(document.docStatus).append("\",");
        jsonBuilder.append("\"doc_type\":\"").append(document.docType).append("\",");
        jsonBuilder.append("\"importRequest\":").append(document.importRequest).append(",");
        jsonBuilder.append("\"owner_inn\":\"").append(document.ownerInn).append("\",");
        jsonBuilder.append("\"producer_inn\":\"").append(document.producerInn).append("\",");
        jsonBuilder.append("\"production_date\":\"").append(document.productionDate).append("\",");
        jsonBuilder.append("\"production_type\":\"").append(document.productionType).append("\",");
        jsonBuilder.append("\"products\":[");
        for (int i = 0; i < document.products.length; i++) {
            Product product = document.products[i];
            jsonBuilder.append("{");
            jsonBuilder.append("\"certificate_document\":\"").append(product.certificateDocument).append("\",");
            jsonBuilder.append("\"certificate_document_date\":\"").append(product.certificateDocumentDate).append("\",");
            jsonBuilder.append("\"certificate_document_number\":\"").append(product.certificateDocumentNumber).append("\",");
            jsonBuilder.append("\"owner_inn\":\"").append(product.ownerInn).append("\",");
            jsonBuilder.append("\"producer_inn\":\"").append(product.producerInn).append("\",");
            jsonBuilder.append("\"production_date\":\"").append(product.productionDate).append("\",");
            jsonBuilder.append("\"tnved_code\":\"").append(product.tnvedCode).append("\",");
            jsonBuilder.append("\"uit_code\":\"").append(product.uitCode).append("\",");
            jsonBuilder.append("\"uitu_code\":\"").append(product.uituCode).append("\"");
            jsonBuilder.append("}");
            if (i < document.products.length - 1) {
                jsonBuilder.append(",");
            }
        }
        jsonBuilder.append("],");
        jsonBuilder.append("\"reg_date\":\"").append(document.regDate).append("\",");
        jsonBuilder.append("\"reg_number\":\"").append(document.regNumber).append("\",");
        jsonBuilder.append("\"signature\":\"").append(signature).append("\"");
        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }

    public static class Document {
        public String participantInn;
        public String docId;
        public String docStatus;
        public String docType;
        public boolean importRequest;
        public String ownerInn;
        public String producerInn;
        public String productionDate;
        public String productionType;
        public Product[] products;
        public String regDate;
        public String regNumber;
    }

    public static class Product {
        public String certificateDocument;
        public String certificateDocumentDate;
        public String certificateDocumentNumber;
        public String ownerInn;
        public String producerInn;
        public String productionDate;
        public String tnvedCode;
        public String uitCode;
        public String uituCode;
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);

        Document doc = new Document();
        doc.participantInn = "1234567890";
        doc.docId = "doc123";
        doc.docStatus = "NEW";
        doc.docType = "LP_INTRODUCE_GOODS";
        doc.importRequest = true;
        doc.ownerInn = "0987654321";
        doc.producerInn = "1122334455";
        doc.productionDate = "2023-01-01";
        doc.productionType = "SELF_PRODUCED";
        doc.regDate = "2023-01-01";
        doc.regNumber = "reg123";

        Product product1 = new Product();
        product1.certificateDocument = "certDoc1";
        product1.certificateDocumentDate = "2023-01-01";
        product1.certificateDocumentNumber = "certNum1";
        product1.ownerInn = "0987654321";
        product1.producerInn = "1122334455";
        product1.productionDate = "2023-01-01";
        product1.tnvedCode = "tnved1";
        product1.uitCode = "uit1";
        product1.uituCode = "uitu1";

        Product product2 = new Product();
        product2.certificateDocument = "certDoc2";
        product2.certificateDocumentDate = "2023-01-01";
        product2.certificateDocumentNumber = "certNum2";
        product2.ownerInn = "0987654321";
        product2.producerInn = "1122334455";
        product2.productionDate = "2023-01-01";
        product2.tnvedCode = "tnved2";
        product2.uitCode = "uit2";
        product2.uituCode = "uitu2";

        doc.products = new Product[]{product1, product2};

        try {
            api.createDocument(doc, "signature_string");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
