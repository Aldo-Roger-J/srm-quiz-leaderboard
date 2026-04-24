import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SRMLeaderboard {

    private static final String REG_NO = "2024CS101";

    private static final String BASE_URL =
            "https://devapigw.vidalhealthtpa.com/srm-quiz-task";

    private static final String GET_API = BASE_URL + "/quiz/messages";
    private static final String POST_API = BASE_URL + "/quiz/submit";

    static class Event {
        String roundId;
        String participant;
        int score;
    }

    static class ApiResponse {
        List<Event> events;
    }

    public static void main(String[] args) {

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        Gson gson = new Gson();

        Set<String> seen = new HashSet<>();
        Map<String, Integer> scores = new HashMap<>();

        System.out.println("=== STARTING POLLING ===\n");

        try {

            for (int poll = 0; poll < 10; poll++) {

                String url = GET_API + "?regNo=" + REG_NO + "&poll=" + poll;

                System.out.println("Polling: " + url);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = null;

                // Retry logic
                int retries = 3;
                while (retries-- > 0) {
                    try {
                        response = client.send(request, BodyHandlers.ofString());

                        if (response.statusCode() == 200) break;

                        System.out.println("Retrying...");
                        Thread.sleep(2000);

                    } catch (Exception e) {
                        System.out.println("Retry failed...");
                    }
                }

                // Skip if still failed
                if (response == null || response.statusCode() != 200) {
                    System.out.println("Skipping poll after retries\n");
                    continue;
                }

                String body = response.body().trim();

                System.out.println("Status: " + response.statusCode());
                System.out.println("Response: " + body);

                // Skip invalid JSON
                if (!body.startsWith("{")) {
                    System.out.println("Invalid JSON, skipping...\n");
                    continue;
                }

                ApiResponse apiResponse;

                try {
                    apiResponse = gson.fromJson(body, ApiResponse.class);
                } catch (Exception e) {
                    System.out.println("JSON parse failed, skipping...\n");
                    continue;
                }

                if (apiResponse != null && apiResponse.events != null) {

                    for (Event e : apiResponse.events) {

                        String key = e.roundId + "_" + e.participant;

                        if (!seen.contains(key)) {
                            seen.add(key);

                            scores.put(
                                    e.participant,
                                    scores.getOrDefault(e.participant, 0) + e.score
                            );
                        } else {
                            System.out.println("Duplicate ignored: " + key);
                        }
                    }
                }

                // Delay
                if (poll < 9) {
                    System.out.println("\nWaiting 5 seconds...\n");
                    TimeUnit.SECONDS.sleep(5);
                }
            }

            // Sort leaderboard
            List<Map.Entry<String, Integer>> leaderboard =
                    new ArrayList<>(scores.entrySet());

            leaderboard.sort((a, b) -> b.getValue() - a.getValue());

            // Print leaderboard
            System.out.println("\n=== FINAL LEADERBOARD ===");

            for (Map.Entry<String, Integer> e : leaderboard) {
                System.out.println(e.getKey() + " -> " + e.getValue());
            }

            int grandTotal = scores.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();

            System.out.println("Grand Total: " + grandTotal);

            if (leaderboard.isEmpty()) {
                System.out.println("\nNo data received. Try again later.");
                return;
            }

            // Prepare POST
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("regNo", REG_NO);

            List<Map<String, Object>> leaderboardList = new ArrayList<>();

            for (Map.Entry<String, Integer> e : leaderboard) {
                Map<String, Object> obj = new HashMap<>();
                obj.put("participant", e.getKey());
                obj.put("totalScore", e.getValue());
                leaderboardList.add(obj);
            }

            requestBody.put("leaderboard", leaderboardList);

            String json = gson.toJson(requestBody);

            System.out.println("\nPOST BODY:");
            System.out.println(json);

            HttpRequest postRequest = HttpRequest.newBuilder()
                    .uri(URI.create(POST_API))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> postResponse =
                    client.send(postRequest, BodyHandlers.ofString());

            System.out.println("\nPOST RESPONSE:");
            System.out.println(postResponse.body());

            try {
                JsonObject result = JsonParser.parseString(postResponse.body())
                        .getAsJsonObject();

                if (result.has("isCorrect") && result.get("isCorrect").getAsBoolean()) {
                    System.out.println("\nSUCCESS - Correct leaderboard!");
                } else {
                    System.out.println("\nIncorrect result");
                }

            } catch (Exception e) {
                System.out.println("Could not parse POST response");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}