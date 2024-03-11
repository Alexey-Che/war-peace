package org.example;

import lombok.val;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {

    public static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(NUM_THREADS);
    public static final String FILE_PATH = "war_and_peace.txt";
    public static final String SEARCH_TEXT = "Пьер";

    public static void main(String[] args) {
        val text = getTextFromFile(FILE_PATH);

        if (!text.isBlank()) {
            val chunkSize = text.length() / NUM_THREADS;
            val occurrences = findOccurrences(text, SEARCH_TEXT, chunkSize);

            System.out.println("Количество найденных вхождений - \"" + SEARCH_TEXT + "\" в тексте: " + occurrences.size());
            System.out.println(occurrences);
        }

        shutdownExecutorService();
    }

    /**
     * Получение списка индексов всех вхождений искомого слова в тексте,
     * текст разбивается на фрагменты, в каждом из которых будет произведен поиск вхождений
     *
     * @param text      текст для поиска
     * @param word      поисковая строка
     * @param chunkSize размер фрагмента текста
     * @return список индексов вхождений искомого слова в тексте
     */
    public static List<Integer> findOccurrences(String text, String word, int chunkSize) {
        val currentTimeMillis = System.currentTimeMillis();
        List<Future<List<Integer>>> futures = new ArrayList<>();

        for (int i = 0; i < NUM_THREADS; i++) {
            val start = i * chunkSize;
            val end = (i == NUM_THREADS - 1) ? text.length() : (i + 1) * chunkSize;

            Callable<List<Integer>> task = () -> {
                List<Integer> occurrences = new ArrayList<>();
                int index = text.indexOf(word, start);
                while (index >= 0 && index < end) {
                    occurrences.add(index);
                    index = text.indexOf(word, index + 1);
                }
                return occurrences;
            };

            futures.add(EXECUTOR_SERVICE.submit(task));
        }

        List<Integer> allOccurrences = new ArrayList<>();
        for (var future : futures) {
            try {
                allOccurrences.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                System.out.println(e.getMessage());
            }
        }
        System.out.println("Время поиска в миллисекундах: " + (System.currentTimeMillis() - currentTimeMillis));
        return allOccurrences;
    }

    /**
     * Чтение текста из файла
     * @param path путь до текстового файла
     * @return текст из файла
     */
    public static String getTextFromFile(String path) {
        String text = null;
        try {
            text = new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
        }
        return text;
    }

    public static void shutdownExecutorService() {
        EXECUTOR_SERVICE.shutdown();
    }
}