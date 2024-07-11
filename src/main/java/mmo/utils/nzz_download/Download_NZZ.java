package mmo.utils.nzz_download;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;


@Slf4j
@ToString
public class Download_NZZ
{
	private final static String baseUrl = "https://epaper.nzz.ch/storefront/6";
	private static final String IssueFileName = "Gesamtausgabe_Neue_Zürcher_Zeitung_%s.pdf"; // %s: Datum in <DateFormat>
	private static final String DateFormat = "yyyy-MM-dd";
	private final static int DownloadMaxWait = 15; // [seconds] max. completion wait time before a download is considered failed
	private final static int AppearanceDefaultWait = 5; // [seconds]
	private final static String DefaultDownloadPath = (System.getProperty("os.name").startsWith("Windows") 
	                                                  ? System.getProperty("user.home", "U:") // assuming "U:" points to user's home directory
	                                                  : "~") // for *ix and Mac
	                                                  + File.separator + "downloads";

	private String downloadPath = DefaultDownloadPath;
	private String targetPath;
	private String usr;
	private String pwd;
	
	private WebDriver driver;
	
	@SuppressWarnings("removal")
	@Override
	protected void finalize() throws Throwable {
		
		closeBrowser();
		super.finalize();
	}
	
	void setUpBrowser() {
		log.info("setUpBrowser.");
		// Initialize ChromeDriver.
		driver = new ChromeDriver();

		// Maximize the browser window size.
		// driver.manage().window().maximize();

		// Navigate to the website.
		driver.get(baseUrl);
	}
	
	void getRidOfNZZGarbage() throws Exception {
//		WebElement dontAllowButton = waitForApearance(By.id("moe-dontallow_button"), 2);
//		if (dontAllowButton != null) {
//			log.info("Clicking '{}'", dontAllowButton.getText());
//			dontAllowButton.click();
//		}	
		WebElement datenSchutzBlaBla = waitForApearance(By.className("cmpboxWelcomeGDPR"), 2);
		if (datenSchutzBlaBla != null) {
			WebElement einstellungen = waitForApearance(By.className("cmptxt_btn_settings"), 1);
			if (einstellungen != null) {
				log.info("Clicking '{}'", einstellungen.getText());
				einstellungen.click();
				WebElement speichernUndBeenden = waitForApearance(By.className("cmptxt_btn_save"), 1);
				if (speichernUndBeenden != null) {
					log.info("Clicking '{}'", speichernUndBeenden.getText());
					speichernUndBeenden.click();
				}
			}
		}		
	}
	
	void login() throws Exception {
		WebElement loginButton = waitForApearance(By.className("fup-login"), 3);
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
				log.info("entering user-id: '{}'", usr);
				loginUsr.sendKeys(usr);
				Thread.sleep(500); // had to introduce this since the page reacted slowly and swallowed some of the entry...
				log.info("entering password: '{}'", pwd);
				loginPwd .sendKeys(pwd);
				Thread.sleep(500);
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

	void downloadEPaper() throws Exception {
		String issueName = String.format(IssueFileName, new SimpleDateFormat(DateFormat).format(Date.from(Instant.now())));
		log.info("looking for issue: '" + issueName + "'");
		
		String downloadFullPath = downloadPath + (downloadPath.endsWith(File.separator) ? "" : File.separator) + issueName;
		File downloadFile = new File(downloadFullPath);
		if (downloadFile.exists()) {
			downloadFile.delete();
		}
		
		WebElement downloadButton = waitForApearance(By.className("fup-s-storefront-download-confirmation"), 5);
		if (downloadButton != null) {
			log.info("Clicking '{}'", downloadButton.getText());
			downloadButton.click(); // Note: this immediately starts downloading the file to the download folder (i.e. without asking for a destination where to save it)!
			log.info("downloading to '{}':", downloadFile);
		} else {
			throw new Exception("download-button not found");			
		}
		
		// wait until download completes:
		int nrWaits = 0;
		while (!downloadFile.exists() && !downloadFile.canRead() && nrWaits <= DownloadMaxWait) {
			log.info("waiting for download of '{}' to complete ({}):", downloadFile, nrWaits++);
			Thread.sleep(1000);
		}
		if (nrWaits > DownloadMaxWait) {
			throw new Exception(String.format("Download  did not complete in '%d' seconds - aborted.", nrWaits));			
		} else {
			log.info("found '{}':", downloadFile.getAbsolutePath());			
		}

		if (downloadFile.exists() && downloadFile.canRead()) {
			if (targetPath == null || targetPath.equals(downloadPath)) {
				log.debug("Downloaded file is already in target folder.");
			} else { // move the downloaded file to the target destination:
				log.info("target path is: '{}'", targetPath);				
				String targetFullPath = targetPath + (targetPath.endsWith(File.separator) ? "" : File.separator) + issueName;
				File targetFile = new File(targetFullPath);
				log.info("full path is: '{}'", targetFullPath);	
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
				} else {
					log.error("Failed to move the downloaded file '{}' to the target destination '{}' - file remains in download folder", downloadFile, targetFile);														
				}
			}
		} else {
			log.error("downloaded file '{}' not found!", downloadFile);
		}
		driver.navigate().back(); // go back to the issue list page in preparation for possible further downloads)
	}

	void closeBrowser() {
		log.info("closeBrowser.");
		if (driver != null) { // terminate the browser.
//			driver.quit();
			driver = null;
		}
	}
	
	WebElement waitForApearance(String className) throws Exception {
		return waitForApearance(className, AppearanceDefaultWait);
	}
	WebElement waitForApearance(String className, int waitMaxSeconds) throws Exception {
		return waitForApearance(By.className(className), waitMaxSeconds);
	}
	
	WebElement waitForApearance(By by, int waitMaxSeconds) throws Exception {
		log.info("waiting for appearance of element '{}'", by);	
		List<WebElement> elems = null;
		WebElement expectedElem = null;
		int nrAttempts = 0;
		do {
			elems = driver.findElements(by);
			if (elems.size() > 0 && (expectedElem = elems.getFirst()).isDisplayed() && expectedElem.isEnabled()) {
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
			log.debug("option {}: '{}'", (char)opt.getId(), opt.getValue());
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
		options.addOption(new Option("d", "download-folder", true, "download-folder [optional - default: '" + DefaultDownloadPath + "']"));
		options.addOption(new Option("t", "target-folder", true, "target-folder [optional - default: same as download-folder]"));
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