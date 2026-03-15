@echo off
rem Copyright © 2024-2026 by Michael Moser / 17732576+mmoser18@users.noreply.github.com
rem Released under GPL V3 or later
echo running NZZ downloader:

set prj=%HOME%\Documents\eclipse\workspace\nzz_download
rem target folder:
set tgt=%prj%\target
rem program executable location:
set jar=%tgt%\nzz_download-1.3.0.jar
rem download location:
set dst=Y:\Things to read\NZZ\rem download location:

cd %tgt%
java -jar "%jar%"
rem with short options:
rem -u "<user-id here>" -p "<password here>" -t "%dst%"
rem with spelled-out options:
java -jar "%jar%" --username "<user-id here>" --password "<password here>" --target-folder "%dst%"
pause
