package com.mahghuuuls.balancekit;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enforces the package dependency rules: which Balance Kit packages each package may import, that nothing imports the
 * root package, and that no main code touches client-only classes.
 */
class PackageDependencyTest {

    private static final String ROOT = "com.mahghuuuls.balancekit";
    private static final Pattern IMPORT = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.*]+)\\s*;", Pattern.MULTILINE);

    static final Map<String, Set<String>> ALLOWED = new HashMap<>();

    static {
        allow("config");
        allow("rules", "config");
        allow("state", "rules");
        allow("attribute", "config");
        allow("mob", "config", "rules", "state", "attribute");
        allow("player", "config", "rules", "state", "attribute");
        allow("damage", "config", "mob");
        allow("breathing", "config");
        allow("command", "config", "rules", "state", "attribute", "mob", "player", "damage", "breathing");
    }

    private static void allow(String pkg, String... targets) {
        ALLOWED.put(pkg, new HashSet<>(Arrays.asList(targets)));
    }

    @Test
    void mainSourcesFollowTheDependencyRules() throws IOException {
        assertEquals(Collections.<String>emptyList(), violations(Paths.get("src/main/java")));
    }

    @Test
    void theCheckCatchesAForbiddenImport() {
        List<String> found = new ArrayList<>();
        check("config", "import com.mahghuuuls.balancekit.mob.MobBalancer;\nimport net.minecraft.client.*;\n"
                + "class X { Object o = net.minecraft.client.Minecraft.getMinecraft(); }\n", "Planted.java", found);
        assertEquals(3, found.size(), found.toString());
    }

    private static List<String> violations(Path sourceRoot) throws IOException {
        Path rootDir = sourceRoot.resolve(ROOT.replace('.', '/'));
        assertTrue(Files.isDirectory(rootDir), "run from the project directory: " + rootDir.toAbsolutePath());
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(rootDir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList())) {
                Path relative = rootDir.relativize(file.getParent());
                String pkg = relative.getNameCount() == 0 || relative.toString().isEmpty() ? "" : relative.getName(0).toString();
                check(pkg, new String(Files.readAllBytes(file), StandardCharsets.UTF_8), file.toString(), found);
            }
        }
        return found;
    }

    private static void check(String pkg, String source, String where, List<String> found) {
        if (source.replaceAll("(?m)^import .*$", "").contains("net.minecraft.client.")) {
            found.add(where + " names a client-only class outside its imports");
        }
        Matcher m = IMPORT.matcher(source);
        while (m.find()) {
            String imported = m.group(1);
            if (imported.startsWith("net.minecraft.client.")) {
                found.add(where + " imports client-only " + imported);
                continue;
            }
            if (!imported.startsWith(ROOT + ".")) {
                continue;
            }
            String rest = imported.substring(ROOT.length() + 1);
            int dot = rest.indexOf('.');
            if (dot < 0) {
                if (!pkg.isEmpty()) {
                    found.add(where + " imports the root package: " + imported);
                }
                continue;
            }
            String target = rest.substring(0, dot);
            if (pkg.isEmpty() || target.equals(pkg)) {
                continue;
            }
            Set<String> allowed = ALLOWED.get(pkg);
            if (allowed == null || !allowed.contains(target)) {
                found.add(where + " (" + pkg + ") may not import " + imported);
            }
        }
    }
}
