package org.example;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class GenerateWorkshopLinks {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java GenerateWorkshopLinks <input_file>");
            return;
        }

        Path inputFile = Paths.get(args[0]);
        List<String> mods;
        try {
            // читаем весь текст из файла
            String content = new String(Files.readAllBytes(inputFile));

            // делим по ;
            mods = Arrays.asList(content.split(";"));
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }

        // путь для результата
        Path outputFile = Paths.get("workshop_links.txt");

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile))) {
            for (String mod : mods) {
                mod = mod.trim();
                if (!mod.isEmpty()) {
                    // здесь просто вставляем мод ID
                    // если это строка ID = прямое число - нормально
                    // но если это имя модификации - нужно вручную заменить на WorkshopID
                    writer.println("https://steamcommunity.com/sharedfiles/filedetails/?id=" + mod);
                }
            }
            System.out.println("Links generated: " + outputFile.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Error writing output: " + e.getMessage());
        }
    }
}