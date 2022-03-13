package com.ossprj.torrent.task.report;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;

@Component
@ConfigurationProperties("build-torrent-report")
@Data
@Validated
public class BuildTorrentReportTaskConfiguration {

    @NotNull
    private Path torrentsDirectory;

    private String reportFormat = "\"{announce}\",\"{createdBy}\",\"{creationDate}\",\"{creationDateFormatted}\",\"{comment}\",\"{files}\",\"{infoHash}\",\"{name}\",\"{pieceLength}\",\"{filePaths}\",\"{torrentFilePath}\"";

    private Path reportFile;

}
