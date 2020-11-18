package edu.csuci.comp420term.datageneration;

import edu.csuci.comp420term.entities.JSONEntity;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Semaphore;

public class JSONApiHelpers {

    private static final Semaphore apiLimiter = new Semaphore(10);

    public static JSONObject fetchAPICall(String apiEndpoint) throws IOException, InterruptedException {
        apiLimiter.acquire();
        try {
            final HttpClient httpClient = HttpClients.createDefault();
            final HttpGet getRequest = new HttpGet(apiEndpoint);
            final HttpResponse httpResponse = httpClient.execute(getRequest);
            final HttpEntity httpResponseEntity = httpResponse.getEntity();
            String responseBody = "";
            if (httpResponseEntity != null) {
                try (InputStream inputStream = httpResponseEntity.getContent()) {
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
                    responseBody = writer.toString();
                }
            }
            return new JSONObject(responseBody);
        } finally {
            apiLimiter.release();
        }
    }

    public static String getEnglishName(JSONObject json) {
        final JSONArray allNames = json.getJSONArray("names");
        final List<JSONObject> englishNames = JSONEntity.filterJsonArray(allNames, jsonObject -> jsonObject.getJSONObject("language").getString("name").equals("en"));
        return englishNames.size() == 1 ? englishNames.get(0).getString("name") : "";
    }

}
