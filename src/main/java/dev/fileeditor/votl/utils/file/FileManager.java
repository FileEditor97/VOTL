package dev.fileeditor.votl.utils.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import dev.fileeditor.votl.App;
import dev.fileeditor.votl.objects.annotation.Nonnull;
import dev.fileeditor.votl.objects.annotation.Nullable;
import dev.fileeditor.votl.objects.constants.Constants;

import net.dv8tion.jda.api.interactions.DiscordLocale;

import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import ch.qos.logback.classic.Logger;

class KeyIsNull extends Exception {
	public KeyIsNull(String str) {
		super(str);
	}
}

public class FileManager {

	private final Logger logger = (Logger) LoggerFactory.getLogger(FileManager.class);

	private final Configuration CONF = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS);
	
	private final Map<String, File> files = new HashMap<>();
	private final List<DiscordLocale> locales = new ArrayList<>();

	public FileManager() {}

	public FileManager addFile(String name, String internal, String external) {
		createUpdateLoad(name, internal, external);
		
		return this;
	}
	
	// Add new language by locale
	public FileManager addLang(@Nonnull String localeTag) {
		DiscordLocale locale = DiscordLocale.from(localeTag);
		if (locale.equals(DiscordLocale.UNKNOWN)) {
			throw new IllegalArgumentException("Unknown locale tag was provided: "+localeTag);
		}
		locales.add(locale);

		return addFile(localeTag, "/lang/" + localeTag + ".json", Constants.DATA_PATH + "lang" + Constants.SEPAR + localeTag + ".json");
	}
	
	public Map<String, File> getFiles() {
		return files;
	}
	
	public List<DiscordLocale> getLanguages() {
		return locales;
	}
	
	public void createUpdateLoad(String name, String internal, String external) {
		File file = new File(external);
		
		String[] split = external.contains("/") ? external.split(Constants.SEPAR) : external.split(Pattern.quote(Constants.SEPAR));

		try {
			if (!file.exists()) {
				if (App.class.getResource(internal) == null)
					throw new FileNotFoundException("Resource file '"+internal+"' not found.");
				if ((split.length == 2 && !split[0].equals(".")) || (split.length >= 3 && split[0].equals("."))) {
					if (!file.getParentFile().mkdirs() && !file.getParentFile().exists()) {
						logger.error("Failed to create directory {}", split[1]);
					}
				}
				if (file.createNewFile()) {
					if (!export(App.class.getResourceAsStream(internal), Paths.get(external))) {
						logger.error("Failed to write {}!", name);
					} else {
						logger.info("Successfully created {}!", name);
						files.put(name, file);
					}
				}
				return;
			}
			if (external.contains("lang")) {
				File tempFile = File.createTempFile("locale-", ".json");
				if (!export(App.class.getResourceAsStream(internal), tempFile.toPath())) {
					logger.error("Failed to write temp file {}!", tempFile.getName());
				} else {
					if (Files.mismatch(file.toPath(), tempFile.toPath()) != -1) {
						if (export(App.class.getResourceAsStream(internal), Paths.get(external))) {
							logger.info("Successfully updated {}!", name);
							files.put(name, file);
							return;
						} else {
							logger.error("Failed to overwrite {}!", name);
						}
					}
				}
				tempFile.delete();
			}
			files.put(name, file);
			logger.info("Successfully loaded {}!", name);
		} catch (IOException ex) {
			logger.error("Couldn't locate nor create {}", file.getAbsolutePath(), ex);
		}
	}

	/**
	 * @param name - json file to be searched
	 * @param path - string's json path
	 * @return Returns not-null string. If search returns null string, returns provided path. 
	 */
	@Nonnull
	public String getString(String name, String path) {
		String result = getNullableString(name, path);
		if (result == null) {
			logger.warn("Couldn't find \"{}\" in file {}.json", path, name);
			return path;
		}
		return result;
	}
	
	/**
	 * @param name - json file to be searched
	 * @param path - string's json path
	 * @return Returns null-able string. 
	 */
	@Nullable
	public String getNullableString(String name, String path) {
		File file = files.get(name);

		String text;
		try {
			if (file == null)
				throw new FileNotFoundException();

			text = JsonPath.using(CONF).parse(file).read("$." + path);

			if (text != null && text.isBlank()) text = null;
		
		} catch (FileNotFoundException ex) {
			logger.error("Couldn't find file {}.json", name);
			text = "FILE ERROR: file not found";
		} catch (InvalidPathException ex) {
			logger.error("Invalid path '{}'\n{}", path, ex.getMessage());
			text = "PATH ERROR: invalid";
		} catch (IOException ex) {
			logger.error("Couldn't process file {}.json\n{}", name, ex.getMessage());
			text = "FILE ERROR: IO exception";
		}

		return text;
	}
	
	@Nullable
	public Boolean getBoolean(String name, String path){
		File file = files.get(name);
		
		if (file == null) return null;
		
		try {
			return JsonPath.using(CONF).parse(file).read("$." + path);
		} catch (FileNotFoundException ex) {
			logger.error("Couldn't find file {}.json", name);
		} catch (IOException ex) {
			logger.warn("Couldn't find \"{}\" in file {}.json", path, name, ex);
		}
		return null;
	}

	@Nonnull
	public List<String> getStringList(String name, String path){
		File file = files.get(name);
		
		if (file == null) {
			logger.error("Couldn't find file {}.json", name);
			return Collections.emptyList();
		}

		try {
			List<String> array = JsonPath.using(CONF).parse(file).read("$." + path);	
			
			if (array == null || array.isEmpty())
				throw new KeyIsNull(path);
				
			return array;
		} catch (FileNotFoundException ex) {
			logger.error("Couldn't find file {}.json", name);
		} catch (KeyIsNull ex) {
			logger.warn("Couldn't find \"{}\" in file {}.json", path, name);
		} catch (IOException ex) {
			logger.warn("Couldn't process file {}.json", name, ex);
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	@Nonnull
	public Map<String, String> getMap(String name, String path){
		File file = files.get(name);
		if(file == null) {
			logger.error("Couldn't find file {}.json", name);
			return Collections.emptyMap();
		}

		try {
			Map<String, String> map = JsonPath.using(CONF).parse(file).read("$." + path, Map.class);	
			
			if (map == null || map.isEmpty())
				throw new KeyIsNull(path);
				
			return map;
		} catch (FileNotFoundException ex) {
			logger.error("Couldn't find file {}.json", name);
		} catch (KeyIsNull ex) {
			logger.warn("Couldn't find \"{}\" in file {}.json", path, name);
		} catch (IOException ex) {
			logger.warn("Couldn't process file {}.json", name, ex);
		}
		return Collections.emptyMap();
	}

	public boolean export(InputStream inputStream, Path destination){
		boolean success = true;
		try {
			Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException | NullPointerException ex){
			logger.info("Exception at copying", ex);
			success = false;
		}

		return success;
	}
}
