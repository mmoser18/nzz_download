rem Copyright © 2024-2025 by Michael Moser / 17732576+mmoser18@users.noreply.github.com
rem Released under GPL V3 or later

echo running NZZ downloader:
rem program executable location:
set loc=%HOME%\Documents\eclipse\workspace\nzz_download\target\
set jar=%loc%nzz_download-1.3.0.jar
rem download location:
set tgt=Y:\Things to read\NZZ\
cd %loc%
java -jar "%jar%"
rem with short options:
rem -u "<user-id here>" -p "<password here>" -t "%tgt%"
rem with spelled-out options:
java -jar "%jar%" --username "<user-id here>" --password "<password here>" --target-folder "%tgt%"
pause
