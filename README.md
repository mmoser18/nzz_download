# c't Downloader
This is a utility to regularly download today's issue of the [NZZ newspaper](https://www.nzz.ch) in PDF format.

It uses [Selenium](https://www.selenium.dev/) to start a Chrome-Browser instance, navigates to 
[https://https://epaper.nzz.ch/storefront/6](https://epaper.nzz.ch/storefront/6) and then 
selects the PDF version to be downloaded (unless that issue is already present in the target folder).

Thus, running this daily will make sure that you have all issues of NZZ downloaded to your destination folder 
in PDF format for your reading pleasure.

Copyright © 2024-2025 by Michael Moser / 17732576+mmoser18@users.noreply.github.com

## Setup:
The basic command-line is 
`java -jar <jar-file> -u <userId> -p <password> <further options here...>`

See the `downloadNZZ.cmd`-file for an example how to run this via a Windows command-file. 
(Note: you will have to enter your user-id and password into that file to get going...).

```
usage: Download_CT
 -u,--username <arg>          user-id for login to NZZ wensite [required]
 -p,--password <arg>          password for login to NZZ wensite [required]
 -d,--download-folder <arg>   download-folder  [optional - default: %HOME%\downloads-folder will be used]
 -t,--target-folder <arg>     target-folder [optional - default: leave file in the download-folder (above)]
```
