# Build Torrent Report

### Tokens Available For Use in a Report

&nbsp;&nbsp;**Standard Torrent Info**

* announce
* createdBy
* creationDate
* comment
* files 
  * List of pipe delimited length:path pairs
  * Paths will have platform dependent file separator generated at runtime 
* infoHash
* name
* pieceLength

&nbsp;&nbsp;**Derived Info**

* creationDateFormatted
  * creationDate in ISO-8601 format (UTC)
* filePaths
  * List of pipe delimited paths
  * Paths will have platform dependent file separator generated at runtime
* torrentFilePath
    * The absolute path to the source torrent

### Token Format

&nbsp;&nbsp;Tokens can be used in the report format by surrounding them with curly brackets as such
* {announce}
* {createdBy}
* ...

### Default Report Format

&nbsp;&nbsp;The default report format is a CSV of all the available tokens

    "{announce}","{createdBy}","{creationDate}","{creationDateFormatted}","{comment}","{files}","{infoHash}","{name}","{pieceLength}","{filePaths}","{torrentFilePath}"

### Configure using the file: 'application.yml' (YAML format)

&nbsp;&nbsp;Print report to the console

    buildTorrentReport:
      # Path to the directory containing the torrents
      torrentsDirectory: /home/user/TorrentTasks/Torrents

&nbsp;&nbsp;Print report to a file

    buildTorrentReport:
      torrentsDirectory: /home/user/TorrentTasks/Torrents
      # Path to the report file
      reportFile: /home/user/TorrentTasks/torrent-report.csv

&nbsp;&nbsp;Print report to a file with a custom format

    buildTorrentReport:
      torrentsDirectory: /home/user/TorrentTasks/Torrents
      # Note the format must be enclosed in single quotes
      reportFormat: '"{infoHash}","{name}","{torrentFilePath}"'
      reportFile: /home/user/TorrentTasks/torrent-report.csv

### Usage:

Ensure the application.yml is in the same directory from which you run the jar

    java -jar torrent-tasks-report-1.0.0-SNAPSHOT.jar 