package com.ossprj.torrent.task.locate;

import com.ossprj.commons.file.function.CalculateDirectoryContentHash;
import com.ossprj.commons.file.function.FindAllDirectoriesAtDepth;
import com.ossprj.commons.text.function.ReplaceTokens;
import com.ossprj.commons.text.function.TextEscaper;
import com.ossprj.commons.torrent.function.CalculateTorrentContentHash;
import com.ossprj.commons.torrent.model.Torrent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class LocateFilesetByTorrentTask {

    final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    final FindAllDirectoriesAtDepth findAllDirectoriesAtDepth = new FindAllDirectoriesAtDepth();
    final ReplaceTokens replaceTokens = new ReplaceTokens();
    final TextEscaper textEscaper = new TextEscaper();

    final CalculateDirectoryContentHash calculateDirectoryContentHash = new CalculateDirectoryContentHash();
    final CalculateTorrentContentHash calculateTorrentContentHash = new CalculateTorrentContentHash();

    private void indexTorrentFiles(final Map<String, List<Path>> filesetsByFolderName, final List<Path> filesets) {

        logger.info("Indexing " + filesets.size() + " filesets");

        filesets.forEach(fileset -> {
            // try {
            if (logger.isTraceEnabled()) {
                logger.trace("Indexing: " + fileset.toAbsolutePath());
            }

            if (filesetsByFolderName.containsKey(fileset.toFile().getName())) {
                // Add to the existing List of TorrentRecords
                filesetsByFolderName.get(fileset.toFile().getName()).add(fileset);
                logger.warn("Duplicate filesets with name: " + fileset.toFile().getName());
            } else {
                // Create a new List of Path(s)
                final List<Path> filesetPaths = new ArrayList<>();
                filesetPaths.add(fileset);
                filesetsByFolderName.put(fileset.toFile().getName(), filesetPaths);
            }
        });
    }

    private void handleMissingTorrent(final LocateFilesetByTorrentTaskConfiguration configuration, final File torrentFile) throws IOException {
        if (configuration.getMissingTorrentsDirectory() != null) {
            final Path missingPath = configuration.getMissingTorrentsDirectory().resolve(torrentFile.getName());
            // If we don't already have it in missing folder then move it
            if (!missingPath.toFile().exists()) {
                Files.move(torrentFile.toPath(), missingPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                // Else delete it
                Files.deleteIfExists(torrentFile.toPath());
            }
        }
    }

    private void handleIncompleteTorrent(final LocateFilesetByTorrentTaskConfiguration configuration, final File torrentFile) throws IOException {
        logger.warn("Incomplete: " + torrentFile.getAbsolutePath());
        if (configuration.getIncompleteTorrentsDirectory() != null) {
            final Path incompleteTorrentPath = configuration.getIncompleteTorrentsDirectory().resolve(torrentFile.getName());
            // If we don't already have it in missing folder then move it
            if (!incompleteTorrentPath.toFile().exists()) {
                Files.move(torrentFile.toPath(), incompleteTorrentPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                // Else delete it
                Files.deleteIfExists(torrentFile.toPath());
            }
        }
    }

    @Bean
    public CommandLineRunner commandLineRunner(final LocateFilesetByTorrentTaskConfiguration configuration) {

        logger.info("configuration: {}", configuration);

        return (strings) -> {

            // Should we assume all torrents will have the extension ".torrent" (and filter as we do here)
            // OR assume the user is competent enough to only have torrents in the source directly and process all files
            final List<File> torrentFiles = Arrays.asList(configuration.getTorrentsDirectory().toFile().listFiles(file -> file.getName().endsWith(".torrent")));

            if (torrentFiles.size() == 0) {
                logger.error("No Torrent Files to Process: " + configuration.getTorrentsDirectory());
            } else {

                // Collect all the fileset paths we want to process
                final List<Path> filesetPaths = new LinkedList<>();

                configuration.getFilesetSearchPaths().forEach(filesetSearchPath -> {
                    filesetPaths.addAll(findAllDirectoriesAtDepth.apply(filesetSearchPath.getBasePath(), filesetSearchPath.getSearchDepth()));
                });

                if (logger.isDebugEnabled()) {
                    logger.debug("filesetPaths: " + filesetPaths);
                }

                final Map<String, List<Path>> filesetsByFolderName = new HashMap<>();
                indexTorrentFiles(filesetsByFolderName, filesetPaths);

                final List<LocatedFileset> locatedFilesets = new ArrayList<>();

                for (final File torrentFile : torrentFiles) {
                    final Torrent torrent = new Torrent(Files.readAllBytes(torrentFile.toPath()));


                    // If we have at least one match, process it
                    if (filesetsByFolderName.containsKey(torrent.getName())) {

                        final List<Path> candidateFilesets = filesetsByFolderName.get(torrent.getName());

                        final List<Path> matchedFilesets = candidateFilesets.stream()
                                .filter(filesetPath -> {
                                    final String directoryContentHash = calculateDirectoryContentHash.apply(filesetPath);
                                    final String torrentContentHash = calculateTorrentContentHash.apply(torrent);
                                    final boolean hashesMatch = directoryContentHash.equals(torrentContentHash);

                                    if (hashesMatch) {
                                        logger.debug("Hashes Match (true) : " + directoryContentHash + " - " + torrentContentHash + " : " + filesetPath + " - " + torrentFile);
                                        return true;
                                    } else {
                                        logger.warn("Hashes Match (false) : " + directoryContentHash + " - " + torrentContentHash + " : " + filesetPath + " - " + torrentFile);
                                        return false;
                                    }
                                }).collect(Collectors.toList());

                        // If none of the candidates matched, its Incomplete
                        if (matchedFilesets.size() == 0) {
                            handleIncompleteTorrent(configuration, torrentFile);
                        } else {
                            if (matchedFilesets.size() > 1) {
                                logger.warn("Matched more than one fileset: " + matchedFilesets);
                            }
                            locatedFilesets.add(new LocatedFileset(torrentFile.toPath(), matchedFilesets.get(0), matchedFilesets.get(0).getParent()));
                        }

                    } else {
                        logger.info("Skipping Missing Torrent: " + torrentFile);
                        handleMissingTorrent(configuration, torrentFile);
                    }
                }

                final List<String> generatedStrings = new LinkedList<>();

                for (final LocatedFileset locatedFileset : locatedFilesets) {

                    final Map<String, String> tokenValues = new HashMap<>();
                    tokenValues.put("torrentPath", textEscaper.perform(locatedFileset.getTorrentPath().toString()));
                    tokenValues.put("filesetPath", locatedFileset.getFilesetPath().toString());
                    tokenValues.put("filesetParentPath", locatedFileset.getFilesetParentPath().toString());

                    generatedStrings.add(replaceTokens.apply(configuration.getReportFormat(), tokenValues));
                }

                if (configuration.getReportFile() != null) {
                    final String fileContent = generatedStrings.stream().reduce((a, b) -> a + "\n" + b).orElse("");
                    Files.write(configuration.getReportFile(), fileContent.getBytes(StandardCharsets.UTF_8));
                } else {
                    generatedStrings.forEach(System.out::println);
                }
            }

        };
    }
}
