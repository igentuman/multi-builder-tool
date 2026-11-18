package igentuman.mbtool.client.gui;

import java.util.List;
import java.util.Locale;

/** Search metadata is resolved once, outside the render loop; source indices never change. */
public final class StructureBrowserModel<T> {
    public record Entry<T>(T value, int sourceIndex, String name, String folder, String folderName) {}
    public record Folder(String id, String name, int count) {}

    private final List<Entry<T>> entries;

    public StructureBrowserModel(List<Entry<T>> entries) {
        this.entries = List.copyOf(entries);
    }

    public List<Entry<T>> filter(String query, String folder) {
        String normalized = query.strip().toLowerCase(Locale.ROOT);
        String[] words = normalized.isEmpty() ? new String[0] : normalized.split("\\s+");
        return entries.stream()
                .filter(entry -> folder == null || folder.equals(entry.folder()))
                .filter(entry -> matches(entry, words))
                .toList();
    }

    public List<Folder> folders(String query) {
        List<Entry<T>> matches = filter(query, null);
        return entries.stream().map(Entry::folder).distinct()
                .map(id -> new Folder(id, entries.stream().filter(e -> id.equals(e.folder()))
                        .findFirst().orElseThrow().folderName(),
                        (int) matches.stream().filter(e -> id.equals(e.folder())).count()))
                .sorted(java.util.Comparator.comparing(Folder::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean matches(Entry<T> entry, String[] words) {
        String text = (entry.name() + " " + entry.folderName() + " " + entry.folder()).toLowerCase(Locale.ROOT);
        for (String word : words) if (!text.contains(word)) return false;
        return true;
    }
}
