package com.ossprj.torrent.task.verify;

import com.ossprj.commons.constraint.IsValidPath;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.Date;

@Component
@ConfigurationProperties("verify-torrent-fileset")
@Data
@Validated
public class VerifyTorrentFilesetTaskConfiguration {

    @Min(1)
    private Integer numberOfThreads = 1;

    @NotNull
    @IsValidPath(isDirectory = true, isReadable = true)
    private Path filesetsDirectory;

    @NotNull
    @IsValidPath(isDirectory = true, isReadable = true)
    private Path torrentsDirectory;

    @IsValidPath(isDirectory = true, isWriteable = true)
    private Path verifiedFilesetsDirectory;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date torrentOnOrAfterDate;

}
