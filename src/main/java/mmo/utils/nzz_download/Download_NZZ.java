/**
 * Copyright © 2024-2025 by Michael Moser
 * Released under GPL V3 or later
 *
 * @author mmo / Michael Moser / 17732576+mmoser18@users.noreply.github.com
 */

package mmo.utils.nzz_download;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;



@Slf4j
@ToString
public class Download_NZZ
{
	private final static String baseUrl = "https://epaper.nzz.ch/storefront/6";
	private final static String IssueFileName = "Gesamtausgabe_Neue_Zürcher_Zeitung_%s.pdf"; // %s: Datum in <DateFormat>
	private final static String DateFormat = "yyyy-MM-dd";
	private final static int DownloadMaxWait = 60; // [seconds] max. completion wait time before a download is considered failed
	private final static int AppearanceDefaultWait = 5; // [seconds]
	private final static String DefaultDownloadPath = (System.getProperty("os.name").startsWith("Windows") 
	                                                  ? System.getProperty("user.home", "U:") // assuming "U:" points to user's home directory
	                                                  : "~") // for *ix and Mac
	                                                  + File.separator + "Downloads";

	private final static String TempDirName = "NZZ_Downloads";
	private final static String DownloadDirPath = DefaultDownloadPath + File.separator + TempDirName;
	
	private String downloadPath = DownloadDirPath;
	private String targetPath;
	private String usr;
	private String pwd;
	private boolean debug;
	
	private WebDriver driver;
	
	@SuppressWarnings("removal")
	@Override
	protected void finalize() throws Throwable {
		closeBrowser();
		super.finalize();
	}
	
	void setUpBrowser() throws Exception {
		log.info("setUpBrowser.");

		// prepare download location:
		final File file = new File(DownloadDirPath);
		if (!file.exists()) {
			if (!file.mkdirs()) {
				throw new Exception("Not able to create temp. download directory '" + DownloadDirPath + "'");				
			}
		}
		if (!file.isDirectory() || !file.canRead()) {
			throw new Exception("Temp. download directory '" + DownloadDirPath + "' is not a directory or not readable.");							
		}
		if (!debug) file.deleteOnExit();
		
		// Initialize ChromeDriver:
		ChromeOptions chromeOptions = new ChromeOptions();
		// found this "https://medium.com/@akshayshinde7289/how-to-download-pdf-file-in-chrome-using-selenium-6a717ced483b"
		// to disable the built-in PDF previewer:
		HashMap<String, Object> chromeOptionsMap = new HashMap<String, Object>();
		chromeOptionsMap.put("download.default_directory", DownloadDirPath);
		chromeOptionsMap.put("plugins.plugins_disabled", new String[] { "Chrome PDF Viewer" });
		chromeOptionsMap.put("plugins.always_open_pdf_externally", true);
		chromeOptions.setExperimentalOption("prefs", chromeOptionsMap);
		// chromeOptions.addArguments("--remote-allow-origins=*");
		driver = new ChromeDriver(chromeOptions);

		// Navigate to the website.
		log.info("navigating to '" + baseUrl + "':");
		driver.get(baseUrl);
	}
	
	void getRidOfNZZGarbage() throws Exception {
//		WebElement dontAllowButton = waitForApearance(By.id("moe-dontallow_button"), 2);
//		if (dontAllowButton != null) {
//			log.info("Clicking '{}'", dontAllowButton.getText());
//			dontAllowButton.click();
//		}	
		WebElement datenSchutzBlaBla = waitForAppearance("cmpboxWelcomeGDPR", 3);
		if (datenSchutzBlaBla != null) {
			WebElement einstellungen = waitForAppearance("cmptxt_btn_settings", 1);
			if (einstellungen != null) {
				log.info("Clicking '{}'", einstellungen.getText());
				einstellungen.click();
				WebElement speichernUndBeenden = waitForAppearance("cmptxt_btn_save", 1);
				if (speichernUndBeenden != null) {
					log.info("Clicking '{}'", speichernUndBeenden.getText());
					speichernUndBeenden.click();
				}
			}
		} else {
			log.info("No data protection nuissance detected.");			
		}
	}
	
	void login() throws Exception {
		WebElement loginButton = waitForAppearance("fup-login", 3);
		log.debug("loginButton=" + loginButton);
		if (loginButton!= null && loginButton.isDisplayed()) { // we are not logged-in, yet.
			log.info("\"Anmelden\" is displayed - logging in:");
			loginButton.click();
			Thread.sleep(3000);
			
			// the login-panel is an iframe - so we first need to find the correct one, 
			// i.e. the one whose name starts with "piano-id-":
			//finding all the web elements using iframe tag
			List<WebElement> iframeElements = driver.findElements(By.tagName("iframe"));
			log.debug("Total number of iframes found: " + iframeElements.size());

			WebDriver frameDriver = null;
			for (int i = 0; i < iframeElements.size(); i++) {
				String name = iframeElements.get(i).getDomAttribute("name");
				log.debug("Frame-name: '" + name + "'");
				if (name.startsWith("piano-id-")) {
					frameDriver = driver.switchTo().frame(i);
					log.info("iframe for credentials entry found: '" + name + "'");			
					break;
				}
			}
			if (frameDriver != null) {
				WebElement loginUsr = frameDriver.findElement(By.xpath("//input[@name='email']"));
				WebElement loginPwd = frameDriver.findElement(By.xpath("//input[@type='password']"));
				WebElement anmeldenButton = frameDriver.findElement(By.className("prime"));
				if (loginUsr == null) {
					throw new Exception("email entry-field not found");
				}
				if (loginPwd == null) {
					throw new Exception("password entry-field not found");
				}
				if (anmeldenButton == null) {
					throw new Exception("Anmelden-button not found");
				}
				// filling out the login form:
				Thread.sleep(250); // had to introduce this since the page reacted slowly and swallowed some of the entry...
				log.info("entering user-id: '{}'", usr);
				typeSlowly(loginUsr, usr); // the input was only partially accepted when typing full speed... ||-(
				Thread.sleep(250); // had to introduce this since the page reacted slowly and swallowed some of the entry...
				log.info("entering password: '{}'", pwd);
				typeSlowly(loginPwd, pwd); // the input was only partially accepted when typing full speed... ||-(
				Thread.sleep(250);
				log.info("clicking '{}':", anmeldenButton);
				anmeldenButton.click();	
				log.info("we should be logged-in now...");
			} else {
				throw new Exception("expected frame for login credentials not found");
			}
		} else {
			log.info("'Anmelden' is NOT displayed - assuming that we already logged in.");			
		}
		driver.switchTo().defaultContent();
	}

	/**
	 * Had to introduce this since some input fields constantly swallowed characters when typing full speed... ||-(
	 */
	void typeSlowly(final WebElement inputField, final String textToType) throws InterruptedException {
		for (int i = 0; i < textToType.length(); i++) {
			final StringBuffer sb = new StringBuffer();
			sb.append(textToType.charAt(i));
			inputField.sendKeys(sb.toString());
			Thread.sleep(10);
		}
	}
	
	/*
	 * Newly the download file gets some random names which we first need to figure out.
	 * Found here: https://stackoverflow.com/questions/34548041/selenium-give-file-name-when-downloading
	 */
	void waitUntilDownloadCompletes(final File downloadFile) throws Exception {
		log.info("downloading to '{}':", downloadFile);
		// Store the current window handle
		final String mainWindow = driver.getWindowHandle();   
		log.trace("currently on: title: '{}' / handle: '{}'", driver.getTitle(), driver.getWindowHandle());
		
		try {
			log.trace("handles are: '{}'", driver.getWindowHandles());
			// open a new tab:
			driver.switchTo().newWindow(WindowType.TAB);
			int nrAttempts = 0;
			while (driver.getWindowHandles().size() < 2) {
				if (++nrAttempts > 3) {
					throw new Exception(String.format("Opening of new Tab did not complete in '%d' seconds - aborted.", nrAttempts));								
				}
			}
			log.trace("handles2 are: '{}'", driver.getWindowHandles());
			driver.switchTo().window((String)driver.getWindowHandles().toArray()[1]);
			log.trace("currently on: title: '{}' / handle: '{}'", driver.getTitle(), driver.getWindowHandle());

			// navigate to chrome downloads in that new tab:
			driver.get("chrome://downloads");
			
			JavascriptExecutor js = (JavascriptExecutor)driver;
			// wait until the file is downloaded:
			final String baseQuery = "return document.querySelector('downloads-manager').shadowRoot.querySelector('#downloadsList downloads-item')";
			final String query = baseQuery + ".shadowRoot.querySelector('div#content #file-link')";
			try {
				// get the latest downloaded file's name:
				nrAttempts = 0;
				String fileName = null;
				while (++nrAttempts < DownloadMaxWait) {
					try {
						fileName = (String)js.executeScript(query + ".text"); // get the latest downloaded file's name
						if (fileName != null) break;
					} catch (Exception ex) {
						if (!ex.getMessage().contains("Cannot read properties of null (reading 'shadowRoot')")) { // this one is expected while the download is not complete, yet 
							log.info("Exception {}: {}", ex.getClass(), ex.getMessage());
						}
					}
					log.info("waiting ({})...", nrAttempts);			
					Thread.sleep(1000);
				}
				log.info("downloaded file: '" + fileName + "'");
				// String downloadURL = (String)js.executeScript(query + ".href"); // get the latest downloaded file's URL
				// log.trace("download URL: '" + downloadURL + "'");
				if (fileName == null || fileName.isBlank()) {
					throw new Exception("Download of PDF-file failed.");
				}
				
				final String fullDownloadFileName = DownloadDirPath + File.separator + fileName;
				final File downloadedFile = new File(fullDownloadFileName);

				nrAttempts = 0;
				while (++nrAttempts < DownloadMaxWait) {
					if (downloadedFile.exists() && downloadedFile.canRead()) {
						break;
					}
					log.info("waiting for '{}' ({})...", fullDownloadFileName, nrAttempts);			
					Thread.sleep(1000);
				}
				if (nrAttempts >= DownloadMaxWait) {
					throw new Exception("Expected a readable file '" + downloadedFile.getAbsolutePath() + "' but didn't find such!?");
				}
				log.info("downloaded '{}':", downloadedFile.getAbsolutePath());	
				
// oddly the names are now correct again (for some time they were numeric monsters simmilar to UUIDs). 
//				// rename the downloaded file, i.e. give it back a reasonable, speaking name:
//				if (!downloadFile.getAbsolutePath().equals(downloadedFile.getAbsolutePath())) {				
//					if (downloadFile.exists()) { // just in case it exists from an earlier but failed runs
//						log.info("deleting '{}':", downloadFile);
//						downloadFile.delete();
//					}
//					log.info("renaming '{}' to '{}':", downloadedFile.getAbsolutePath(), downloadFile);						
//					downloadedFile.renameTo(downloadFile);
//				}
			} catch (Exception ex) {
				log.info("Exception " + ex.getMessage());
				throw ex;
			}
		// we don't catch any exception - rather the program will terminate 
		} finally {
			// close the downloads tab2
			driver.close();
			// switch back to main window
			driver.switchTo().window(mainWindow);
		}
	}
	
	void downloadEPaper() throws Exception {
		try {
			String issueName = String.format(IssueFileName, new SimpleDateFormat(DateFormat).format(Date.from(Instant.now())));
			log.info("looking for issue: '" + issueName + "'");
			
			String issueFullName = downloadPath + (downloadPath.endsWith(File.separator) ? "" : File.separator) + issueName;
			File downloadFile = new File(issueFullName);
			if (downloadFile.exists()) {
				downloadFile.delete();
			}
			// it often takes forever and a day until the login-panel has disappeared
			Thread.sleep(5000);
			WebElement downloadButton = waitForAppearance("download", 30);
			if (downloadButton != null) {		
				Thread.sleep(5000); // it typically takes several seconds until that button gets visible and active:
				new WebDriverWait(driver, Duration.ofSeconds(30)).until(ExpectedConditions.elementToBeClickable(downloadButton));
				log.info("Clicking '{}'", downloadButton.getText());
				downloadButton.click(); // Note: this immediately starts downloading the file to the download folder (i.e. without asking for a destination where to save it)!
			} else {
				throw new Exception("download-button not found");			
			}
			
			waitUntilDownloadCompletes(downloadFile);
			
			if (downloadFile.exists() && downloadFile.canRead()) {
				if (targetPath == null || targetPath.equals(downloadPath)) {
					log.debug("Downloaded file is already in target folder.");
					showLocalURL(downloadFile);				
				} else { // move the downloaded file to the target destination:
					log.info("target path is: '{}'", targetPath);				
					String targetFullPath = targetPath + (targetPath.endsWith(File.separator) ? "" : File.separator) + issueName;
					File targetFile = new File(targetFullPath);
					log.info("full target name is: '{}'", targetFullPath);	
					// if target exists already: delete it:
					if (targetFile.exists()) {
						log.info("deleting prior existing file '{}':", targetFile);
						if (targetFile.delete()) {
							log.info("prior existing file deleted.");					
						} else {
							log.warn("unabled to delete prior existing file '{}' - the following move will likely fail:", targetFile);										
						}
					}
					log.info("moving the downloaded file '{}' to the target destination '{}':", downloadFile, targetFile);
					if (downloadFile.renameTo(targetFile)) {
						log.info("done.");
						showLocalURL(targetFile);
					} else {
						log.error("Failed to move the downloaded file '{}' to the target destination '{}' - file remains in download folder", downloadFile, targetFile);														
					}
				}
			} else {
				log.error("downloaded file '{}' not found!", downloadFile);
			}
		} finally {
			driver.navigate().back(); // go back to the issue list page in preparation for possible further downloads)
		}
	}

	private String showLocalURL(File file) throws IOException { 
		String url = "file:///" + file.getCanonicalPath().replace('\\', '/');
		log.info("To open issue go to '" + url + "'.");
		return url;
	}
	
	void closeBrowser() {
		log.info("closeBrowser.");
		if (driver != null) { // terminate the browser.
			try {
				if (!debug) driver.quit();
			} catch (Exception ex) {
				log.error("quitting driver threw an exception: " + ex.getMessage());
				// ignore - we were only trying to gracefully shut down anyway...
			}
			driver = null;
		}
	}
	
	WebElement waitForAppearance(String className) throws Exception {
		return waitForAppearance(className, AppearanceDefaultWait);
	}
	WebElement waitForAppearance(String className, int waitMaxSeconds) throws Exception {
		return waitForAppearance(By.className(className), waitMaxSeconds);
	}
	
	WebElement waitForAppearance(By by, int waitMaxSeconds) throws Exception {
		log.info("waiting for appearance of element '{}'", by);	
		List<WebElement> elems = null;
		WebElement expectedElem = null;
		int nrAttempts = 0;
		do {
			elems = driver.findElements(by);
			if (elems.size() > 0 && (expectedElem = elems.getFirst()).isDisplayed()/* && expectedElem.isEnabled()*/) {
				break;
			}
			if (nrAttempts++ >= waitMaxSeconds) {
				log.info("no element '" + by + "' found within " + waitMaxSeconds + " seconds");
				return null;
			}
			log.info("waiting ({})...", nrAttempts);			
			Thread.sleep(1000);
		} while (true);
		return expectedElem;
	}

	private void processCommandLine(CommandLine line, Options options) throws Exception {
		for (Option opt: line.getOptions()) {
			log.info("option {}: '{}'", (char)opt.getId(), opt.getValue());
			switch (opt.getId()) {
			case 'd':
				this.downloadPath = opt.getValue().replace('/', File.separatorChar);
				if (this.downloadPath.endsWith("\"")) { // for some odd reason the trailing quote from the cmd-file makes in into the argument ||-(
					this.downloadPath = this.downloadPath.substring(0, this.downloadPath.length()-1);
				}				
				break;
			case 't':
				this.targetPath = opt.getValue().replace('/', File.separatorChar); 
				if (this.targetPath.endsWith("\"")) { // for some odd reason the trailing quote from the cmd-file makes in into the argument ||-(
					this.targetPath = this.targetPath.substring(0, this.targetPath.length()-1);
				}
				break;
			case 'u':
				this.usr = opt.getValue();
				break;
			case 'p':
				this.pwd = opt.getValue();
				break;
			case 'x':
				this.debug = !this.debug;
				break;
			default:
				log.error("Unexpected option: '{}' - ignored.");
				usage(options, -4);
			}	
		}
		if (this.usr == null || this.pwd == null || line.getArgList().size() > 0) {
			usage(options, -5);
		}
	}

	private static void usage(Options options, int exitCode) {
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(100, "java -jar <jar.file> { <options> }.\n\n", "options are:", options, "");
		if (exitCode != 0) System.exit(exitCode);
	}
	
	private static Options createOptions() {
		final Options options = new Options();
		options.addOption(new Option("u", "username", true, "user-id for login to NZZ website [required]"));
		options.addOption(new Option("p", "password", true, "password for login to NZZ website [required]"));
		options.addOption(new Option("d", "download-folder", true, "download-folder [optional - default: '" + DownloadDirPath + "']"));
		options.addOption(new Option("t", "target-folder", true, "target-folder [optional - default: same as download-folder]"));
		options.addOption(new Option("x", "debug", true, "toggle debug mode (do not delete temp. files at exit, etc.)"));
		return options;
	}	

	public static void main(String[] arguments) {
		Options options = null;
		try {
			options = createOptions();
		} catch (Exception exp) {
			System.err.println("Internal error initializing the program options: " + exp.getMessage());
			exp.printStackTrace();
			System.exit(-1);
		}
		CommandLine line = null;
		try {
			// create the parser
			CommandLineParser parser = new DefaultParser();
			// parse the command line arguments:
			line = parser.parse(options, arguments);
		} catch (Exception exp) {
			System.err.println("Illegal or malformed option(s): " + exp.getMessage());
			usage(options, -2);
		}
		
		try {
			Download_NZZ downloader = new Download_NZZ();
			downloader.processCommandLine(line, options);
			downloader.setUpBrowser();
			downloader.getRidOfNZZGarbage();
			downloader.login();
			downloader.downloadEPaper();
			downloader.closeBrowser();
		} catch (Throwable t) {
			System.err.println("error executing " + Download_NZZ.class.getSimpleName());
			t.printStackTrace();
		}
	}
}