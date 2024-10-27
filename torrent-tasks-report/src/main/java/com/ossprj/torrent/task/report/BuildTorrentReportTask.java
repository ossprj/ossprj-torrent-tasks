package com.ossprj.torrent.task.report;

import com.ossprj.commons.text.function.ReplaceTokens;
import com.ossprj.commons.torrent.model.Torrent;
import com.ossprj.commons.torrent.model.TorrentFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

@Component
public class BuildTorrentReportTask {

    final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    final ReplaceTokens replaceTokens = new ReplaceTokens();

    @Bean
    public CommandLineRunner commandLineRunner(final BuildTorrentReportTaskConfiguration configuration) {

        logger.info("configuration: " + configuration);

        return (strings) -> {

            logger.debug("Getting torrent files");
            final List<File> torrentFiles = Arrays.asList(configuration.getTorrentsDirectory().toFile().listFiles());

            final List<String> generatedStrings = new LinkedList<>();

            logger.debug("Processing Torrent Files");
            for (final File torrentFile : torrentFiles) {
                logger.debug("Processing: " + torrentFile.toPath().toString());
                // TODO: Handle files that aren't valid torrents
                final Torrent torrent = new Torrent(Files.readAllBytes(torrentFile.toPath()));

                final Map<String, String> tokenValues = new HashMap<>();
                // Support all the standard torrent fields
                tokenValues.put("announce", torrent.getAnnounce().toString());
                tokenValues.put("createdBy", torrent.getCreatedBy() != null ? torrent.getCreatedBy() : "");
                tokenValues.put("creationDate", torrent.getCreationDate() != null ? torrent.getCreationDate().toString() : "");
                tokenValues.put("comment", torrent.getComment() != null ? torrent.getComment() : "");
                tokenValues.put("files", torrent.getFiles().stream().map(tf -> tf.getLength() + ":" + tf.getPath()).reduce((a, b) -> a + "|" + b).get());
                tokenValues.put("infoHash", torrent.getInfoHashHex());
                tokenValues.put("name", torrent.getName() != null ? torrent.getName() : "");
                tokenValues.put("pieceLength", torrent.getPieceLength().toString());

                // Creation Date in ISO-8601 format
                tokenValues.put("creationDateFormatted", torrent.getCreationDate() != null ? Instant.ofEpochMilli(torrent.getCreationDate() * 1000).toString() : "");

                // Include the file path of the source torrent
                tokenValues.put("torrentFilePath", torrentFile.getPath());

                // Computed token with list of paths only (without the length data present in the "files" token)
                tokenValues.put("filePaths", torrent.getFiles().stream().map(TorrentFile::getPath).reduce((a, b) -> a + "|" + b).get());

                generatedStrings.add(replaceTokens.apply(configuration.getReportFormat(), tokenValues));
            }

            if (!generatedStrings.isEmpty()) {
                logger.debug("Building Report File");
                if (configuration.getReportFile() != null) {
                    final String fileContent = generatedStrings.stream()
                            .reduce((a, b) -> a + "\n" + b).get();
                    Files.write(configuration.getReportFile(), fileContent.getBytes(StandardCharsets.UTF_8));
                } else {
                    logger.debug("Printing");
                    generatedStrings.forEach(System.out::println);
                }
            } else {
                logger.warn("No report content, skipping output");
            }
        };
    }

}
