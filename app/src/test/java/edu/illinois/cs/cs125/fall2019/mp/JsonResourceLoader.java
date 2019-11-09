package edu.illinois.cs.cs125.fall2019.mp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Scanner;

final class JsonResourceLoader {

    private static final JsonParser PARSER = new JsonParser();

    private JsonResourceLoader() { }

    static JsonElement load(String title) {
        try (Scanner scanner = new Scanner(JsonResourceLoader.class.getResourceAsStream("/" + title + ".json"))) {
            return PARSER.parse(scanner.nextLine());
        }
    }

    static JsonObject[] loadArray(String title) {
        JsonArray jsonArray = load(title).getAsJsonArray();
        JsonObject[] result = new JsonObject[jsonArray.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = jsonArray.get(i).getAsJsonObject();
        }
        return result;
    }

    static int[] getIntArray(JsonArray array) {
        int[] result = new int[array.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = array.get(i).getAsInt();
        }
        return result;
    }

    static double[] getDoubleArray(JsonArray array) {
        double[] result = new double[array.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = array.get(i).getAsDouble();
        }
        return result;
    }

}
