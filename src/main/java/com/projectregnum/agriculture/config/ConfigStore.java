// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.config;

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;
import java.util.function.Consumer;

public final class ConfigStore {
    private final Path path;
    private final Consumer<String> warn;
    private ServerConfig current = ServerConfig.defaults();

    public ConfigStore(Path path, Consumer<String> warn) { this.path = path; this.warn = warn; }
    public ServerConfig current() { return current; }

    /** Malformed reloads leave the active policy unchanged and never overwrite the user's file. */
    public boolean reload() {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.toAbsolutePath().getParent());
                Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(ServerConfig.defaults()),
                        StandardOpenOption.CREATE_NEW);
            }
            ServerConfig next = ServerConfig.parse(Files.readString(path), warn);
            current = next;
            return true;
        } catch (IOException | RuntimeException e) {
            warn.accept("Cannot load " + path.getFileName() + "; retaining active configuration: " + e.getMessage());
            return false;
        }
    }
}
