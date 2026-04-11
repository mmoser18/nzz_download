@echo off
rem Copyright © 2024-2026 by Michael Moser / 17732576+mmoser18@users.noreply.github.com
rem Released under GPL V3 or later
echo running NZZ downloader:

set prj=%HOME%\Documents\eclipse\workspace\nzz_download
rem target folder:
set tgt=%prj%\target
rem program executable location:
rem set jar=%tgt%\nzz_download-1.4.0.jar - trying to find the correct jar automatically:
for /f "tokens=*" %%i in ('dir /b "%tgt%\nzz_download-*.jar"') do set jar=%%i
echo executing: %jar%

rem download location:
set dst=Y:\Things to read\NZZ\rem download location:

cd %tgt%
rem Note - the "file:" is essential so that log4j can find its config:
set log4j.configurationFile=file:log4j2.xml

rem with short options:
rem -u "<user-id here>" -p "<password here>" -t "%dst%"
rem with spelled-out options:
java -jar "%jar%" --username "<user-id here>" --password "<password here>" --target-folder "%dst%"
pause
