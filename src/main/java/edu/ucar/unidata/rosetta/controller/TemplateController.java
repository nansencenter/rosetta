package edu.ucar.unidata.rosetta.controller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.ucar.unidata.rosetta.converters.EolSoundingComp;
import edu.ucar.unidata.rosetta.converters.TagBaseArchivalTag;
import edu.ucar.unidata.rosetta.converters.XlsToCsv;
import edu.ucar.unidata.rosetta.domain.AsciiFile;
import edu.ucar.unidata.rosetta.domain.UploadedAutoconvertFile;
import edu.ucar.unidata.rosetta.domain.UploadedFile;
import edu.ucar.unidata.rosetta.dsg.NetcdfFileManager;
import edu.ucar.unidata.rosetta.service.FileParserManager;
import edu.ucar.unidata.rosetta.service.FileValidator;
import edu.ucar.unidata.rosetta.service.ResourceManager;
import edu.ucar.unidata.rosetta.util.JsonUtil;
import edu.ucar.unidata.rosetta.util.RosettaProperties;
import edu.ucar.unidata.rosetta.util.ZipFileUtil;

/**
 * Main controller for Rosetta application.
 */
@Controller
public class TemplateController implements HandlerExceptionResolver {

    protected static Logger logger = Logger.getLogger(TemplateController.class);
    @Autowired
    ServletContext servletContext;
    @Resource(name = "resourceManager")
    private ResourceManager resourceManager;
    @Resource(name = "fileParserManager")
    private FileParserManager fileParserManager;
    @Resource(name = "netcdfFileManager")
    private NetcdfFileManager netcdfFileManager;
    @Resource(name = "fileValidator")
    private FileValidator fileValidator;

    private String getDownloadDir() {
        String downloadDir = "";
        downloadDir = RosettaProperties.getDownloadDir();
        return downloadDir;
    }

    private String getUploadDir() {
        String uploadDir = "";
        uploadDir = RosettaProperties.getUploadDir();
        return uploadDir;
    }

    /**
     * Accepts a GET request for template creation, fetches resource information
     * from file system and inject that data into the Model to be used in the
     * View. Returns the view that walks the user through the steps of template
     * creation.
     *
     * @param model The Model object to be populated by Resources.
     * @return The 'create' ModelAndView containing the Resource-populated Model.
     */
    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public ModelAndView createTemplate(Model model) {
        model.addAllAttributes(resourceManager.loadResources());
        model.addAttribute("maxUploadSize", RosettaProperties.getMaxUploadSize(servletContext));
        return new ModelAndView("create");
    }

    /**
     * Accepts a GET request for autoconversion of a known file, fetches resource information
     * from file system and inject that data into the Model to be used in the
     * View. Returns the view that walks the user through the steps of template
     * creation.
     *
     * @param model The Model object to be populated by Resources.
     * @return The 'create' ModelAndView containing the Resource-populated Model.
     */
    @RequestMapping(value = "/autoConvert", method = RequestMethod.GET)
    public ModelAndView autoConvertKnownFile(Model model) {
        model.addAllAttributes(resourceManager.loadResources());
        return new ModelAndView("autoConvert");
    }

    /**
     * Accepts a GET request for workflow to augment metadata of a known file,
     * fetches resource information from file system and injects that data into
     * the Model to be used in the View
     * Returns the view that walks the user through the steps of template
     * creation.
     *
     * @param model The Model object to be populated by Resources.
     * @return The 'create' ModelAndView containing the Resource-populated Model.
     */
    @RequestMapping(value = "/augmentMetadata", method = RequestMethod.GET)
    public ModelAndView augmentMetadata(Model model) {
        model.addAllAttributes(resourceManager.loadResources());
        return new ModelAndView("augmentMetadata");
    }

    /**
     * Accepts a GET request for template creation, fetches resource information
     * from file system and inject that data into the Model to be used in the
     * View. Returns the view that walks the user through the steps of template
     * creation.
     *
     * @param model The Model object to be populated by Resources.
     * @return The 'create' ModelAndView containing the Resource-populated Model.
     */
    @RequestMapping(value = "/createTrajectory", method = RequestMethod.GET)
    public ModelAndView createTrajectoryTemplate(Model model) {
        model.addAllAttributes(resourceManager.loadResources());
        return new ModelAndView("createTrajectory");
    }

    /**
     * Accepts a GET request for template restoration, fetches resource
     * information from file system and inject that data into the Model to be
     * used in the View. Returns the view that walks the user through the steps
     * of template creation.
     *
     * @param model The Model object to be populated by Resources.
     * @return The 'create' ModelAndView containing the Resource-populated Model.
     */
    @RequestMapping(value = "/restore", method = RequestMethod.GET)
    public ModelAndView restoreTemplate(Model model) {
        BasicConfigurator.configure();
        model.addAllAttributes(resourceManager.loadResources());
        model.addAttribute("maxUploadSize", RosettaProperties.getMaxUploadSize(servletContext));
        return new ModelAndView("restore");
    }

    /**
     * Accepts a POST request for an uploaded file, stores that file to disk
     * and, returns the local (unique, alphanumeric) file name of the uploaded
     * ASCII file.
     *
     * @param file    The UploadedFile form backing object containing the file.
     * @param request The HttpServletRequest with which to glean the client IP address.
     * @return A String of the local file name for the ASCII file (or null for an error).
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @ResponseBody
    public String processUpload(UploadedFile file, HttpServletRequest request) {
        // FileOutputStream outputStream = null;
        String uniqueId = createUniqueId(request);
        String filePath = FilenameUtils.concat(getUploadDir(), uniqueId);
        try {
            File localFileDir = new File(filePath);
            if (!localFileDir.exists()) {
                localFileDir.mkdirs();
            }
            File uploadedFile = new File(FilenameUtils.concat(filePath, file.getFileName()));
            try (FileOutputStream outputStream = new FileOutputStream(uploadedFile)) {
                outputStream.write(file.getFile().getFileItem().get());
                outputStream.flush();
                outputStream.close();
            } catch (Exception exc) {
                logger.error("error while saving uploaded file to disk.");
                return null;
            }

            if ((file.getFileName().contains(".xls")) || (file.getFileName().contains(".xlsx"))) {
                String xlsFilePath = FilenameUtils.concat(filePath, file.getFileName());
                boolean conversionSuccessful = XlsToCsv.convert(xlsFilePath, null);
                String csvFilePath = null;
                if (conversionSuccessful) {
                    if (xlsFilePath.contains(".xlsx")) {
                        csvFilePath = xlsFilePath.replace(".xlsx", ".csv");
                    } else if (xlsFilePath.contains(".xls")) {
                        csvFilePath = xlsFilePath.replace(".xls", ".csv");
                    }
                }
                file.setFileName(csvFilePath);
            }
        } catch (Exception e) {
            logger.error("A file upload error has occurred: " + e.getMessage());
            return null;
        }
        return uniqueId;
    }


    /**
     * Accepts a GET request to see if an already uploaded file contains blank lines.
     *
     * @param fileName The UploadedFile form backing object containing the file.
     * @param uniqueId The UploadedFile form backing object containing the file.     *
     * @return The number of blank lines in the file.
     */
    @RequestMapping(value = "/getBlankLines", method = RequestMethod.GET)
    @ResponseBody
    public String getBlankLines(@RequestParam("fileName") String fileName, @RequestParam("uniqueId") String uniqueId) {
        int blankLineCount = 0;
        String filePath = FilenameUtils.concat(getUploadDir(), uniqueId);
        File uploadedFile = new File(FilenameUtils.concat(filePath, fileName));
        blankLineCount = fileParserManager.getBlankLines(uploadedFile);
        return new Integer(blankLineCount).toString();
    }


    /**
     * Accepts a POST request for an uploaded file, stores that file to disk, auto converts the
     * file, and, returns a zip file of the converted dataset ASCII file.
     *
     * @param file    The UploadedFile form backing object containing the file.
     * @param request The HttpServletRequest with which to glean the client IP address.
     * @return A String of the local file name for the ASCII file (or null for an error).
     */
    @RequestMapping(value = "/autoConvertKnownFile", method = RequestMethod.POST)
    @ResponseBody
    public String autoConvertKnownFile(UploadedAutoconvertFile file, HttpServletRequest request) {
        List<String> convertedFiles = new ArrayList<>();
        String uniqueId = file.getUniqueId();
        String convertTo = file.getConvertTo();
        String convertFrom = file.getConvertFrom();

        String returnFile = null;

        String filePath = FilenameUtils.concat(getUploadDir(), uniqueId);
        String fileName = file.getFileName();
        String fullFileName = FilenameUtils.concat(filePath, fileName);

        String downloadDir = FilenameUtils.concat(getDownloadDir(), uniqueId);
        File downloadFileDir = new File(downloadDir);
        if (!downloadFileDir.exists()) {
            downloadFileDir.mkdir();
        }

        try {
            if (convertFrom != null) {
                if (convertFrom.equals("esc")) {
                    EolSoundingComp escConvertor = new EolSoundingComp();
                    convertedFiles = escConvertor.convert(fullFileName);
                    if (!convertedFiles.isEmpty()) {
                        logger.info("Success");
                    }
                } else if (convertFrom.equals("tbaff")) {
                    TagBaseArchivalTag tbat = new TagBaseArchivalTag();
                    tbat.parse(fullFileName);
                    String fullFileNameExt = FilenameUtils.getExtension(fullFileName);
                    String ncfile = fileName.replace(fullFileNameExt, "nc");
                    ncfile = FilenameUtils.concat(downloadDir, ncfile);
                    returnFile = tbat.convert(ncfile);
                    returnFile = ncfile;
                }
            }
        } catch (Exception e) {
            logger.error("A file conversion error has occurred: " + e.getMessage());
            return null;
        }

        if (convertedFiles.size() >= 1) {
            String zipFileName = FilenameUtils.concat(downloadDir, "converted_files.zip");
            ZipFileUtil zippedConvertedFiles = new ZipFileUtil(zipFileName);
            zippedConvertedFiles.addAllToZip(convertedFiles);
            returnFile = zipFileName;
        }

        returnFile = returnFile.replaceAll(downloadDir + "/", "");

        return returnFile;
    }


    /**
     * Accepts a POST request Parse the uploaded file. The methods gets called
     * at various times and the file data is parsed according the data it
     * contains.
     *
     * @param file   The AsciiFile form backing object.
     * @param result The BindingResult object for errors.
     * @return A String of the local file name for the ASCII file.
     */
    @RequestMapping(value = "/parse", method = RequestMethod.POST)
    @ResponseBody
    public String parseFile(AsciiFile file, BindingResult result) {
        String filePath = FilenameUtils.concat(getUploadDir(), file.getUniqueId());
        filePath = FilenameUtils.concat(filePath, file.getFileName());

        // SCENARIO 1: no header lines yet (return the file contents to display in grid)
        if (file.getHeaderLineList().isEmpty()) {
            return fileParserManager.parseByLine(filePath);
        } else {
            // SCENARIO 2: header lines and delimiters available
            if (!file.getDelimiterList().isEmpty()) {
                List<String> delimiterList = file.getDelimiterList();
                String selectedDelimiter = delimiterList.get(0);
                /**
                 * // Time for some validation
                 * fileValidator.validateList(delimiterList, result);
                 * fileValidator.validateList(file.getHeaderLineList(), result);
                 * fileValidator.validateDelimiterCount(filePath, file, result);
                 *
                 * if (result.hasErrors()) { List<ObjectError> validationErrors
                 * = result.getAllErrors(); Iterator<ObjectError> iterator =
                 * validationErrors.iterator(); while (iterator.hasNext()) {
                 * logger.error("Validation Error: " +
                 * iterator.next().toString()); } return null;
                 *
                 * } else {
                 **/
                String normalizedFileData = fileParserManager.normalizeDelimiters(filePath, selectedDelimiter, delimiterList, file.getHeaderLineList());
                if (file.getVariableNameMap().isEmpty()) {
                    return StringEscapeUtils.escapeHtml4(selectedDelimiter + "\n" + normalizedFileData);
                } else {
                    // SCENARIO 3: we have variable data!

                    // pull out sessionStorage data
                    HashMap<String, HashMap<String,String>> mm = file.getVariableMetadataMap();
                    HashMap<String, String> coords = new HashMap<String, String>();

                    // fix where session storage where necessairy
                    // FIXME: hardcoded stuff that should not be in here?
                    if (file.getCfType().equals("trajectory")) {
                        // only the time variable should be a coordinate variable, so
                        // clean up session storage to reflect that.

                        for (String mmKey : mm.keySet()) {
                            if (mm.get(mmKey).containsKey("_coordinateVariableType")) {
                                HashMap<String, String> tmp = mm.get(mmKey);
                                String cvt = tmp.get("_coordinateVariableType").toString();
                                boolean chk1 = cvt.equals("relTime");
                                boolean chk2 = cvt.equals("fullDateTime");
                                boolean chk3 = cvt.equals("dateOnly");
                                boolean chk4 = cvt.equals("timeOnly");
                                if (!(chk1 | chk2 | chk3 | chk4)) {
                                    coords.put(cvt, file.getVariableNameMap().get(mmKey.replace("Metadata", "")));
                                    tmp.remove("_coordinateVariableType");
                                    tmp.put("_coordinateVariable", "non-coordinate");
                                    mm.put(mmKey, tmp);
                                }
                            }
                            file.setOtherInfo(coords);
                        }
                    }
                    List<List<String>> parseFileData = fileParserManager
                            .getParsedFileData();
                    List<String> header = fileParserManager.getHeader();

                    // Create the NCML file using the file data
                    String downloadDir = FilenameUtils.concat(getDownloadDir(),
                            file.getUniqueId());

                    // create JSON file that holds sessionStorage information
                    // Should be using file.getJsonStrSessionStorage()
                    // use jsonUtil.strToJson(file.getJsonStrSessionStorage()) to remove
                    // connection specific metadata, like uniqueid,
                    Boolean isQuickSave = false;
                    String userFile = file.getFileName();
                    String jsonFileName = getTemplateFileName(userFile,
                            isQuickSave);

                    String jsonOut = FilenameUtils.concat(downloadDir,
                            FilenameUtils.getFullPath(file.getFileName()));
                    jsonOut = FilenameUtils.concat(jsonOut, jsonFileName);
                    JsonUtil jsonUtil = new JsonUtil(jsonOut);
                    //JSONObject jsonObj = jsonUtil.ssHashMapToJson(mm);
                    JSONObject jsonObj = jsonUtil.strToJson(file.getJsonStrSessionStorage());
                    jsonObj.remove("uniqueId");
                    jsonObj.remove("fileName");
                    //jsonObj.remove("varCoords");
                    jsonUtil.writeJsonToFile(jsonObj);

                    // zip JSON and NcML files
                    // String zipFileName = FilenameUtils.concat(downloadDir,
                    // "rosetta.zip");
                    // ZipFileUtil transactionZip = new
                    // ZipFileUtil(zipFileName);
                    // String[] sourceFiles = {jsonOut};
                    // transactionZip.addAllToZip(sourceFiles);
                    // String netcdfFile = null;

                    String netcdfFile = null;
                    try {
                        NetcdfFileManager dsgWriter;
                        for (NetcdfFileManager potentialDsgWriter : netcdfFileManager.asciiToDsg()) {
                            if (potentialDsgWriter.isMine(file.getCfType())) {
                                netcdfFile = potentialDsgWriter.createNetcdfFile(file,
                                        parseFileData, header, downloadDir);
                                break;
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Caught IOException: "
                                + e.getMessage());
                        return netcdfFile;
                    } catch (IllegalArgumentException e) {
                        //FIXME: possible security issue with revealing too much?
                        return e.getMessage();
                    }

                    String fileOut = FilenameUtils.concat(downloadDir,
                            FilenameUtils.removeExtension(file.getFileName())
                                    + ".nc");
                    String escapedDownloadDir = StringEscapeUtils.escapeJava(downloadDir + File.separator);
                    
                    //New functionality check the NetCDF file and get ncdump -h info.
                    String Netcdfinfo = NetcdfFileManager.checkNetCDF(fileOut);
                    String NetcdfInfoName = FilenameUtils.concat(downloadDir,
                            FilenameUtils.removeExtension(userFile) + ".txt");
                    //Write the ncdump information to file
                    try (FileWriter wr = new FileWriter(NetcdfInfoName)){
                    	wr.write(Netcdfinfo);
                    	wr.flush();
                    } catch (IOException e) {
                        System.err.println("Caught IOException: "
                                + e.getMessage());                    	
                    }
                    
                    return fileOut.replaceAll(escapedDownloadDir, "") + "\n"
                            + jsonOut.replaceAll(escapedDownloadDir, "") + "\n"
                            + NetcdfInfoName.replaceAll(escapedDownloadDir, "");
                }
                // }
            } else {
                logger.error("File header lines are present, but no delimiters have been specified.");
                return null;
            }
        }
    }


    /**
     * Accepts a POST request Get global metadata from the uploaded file.
     *
     * @param file   The AsciiFile form backing object.
     * @param request The BindingResult object for errors.
     * @return A String of the local file name for the ASCII file.
     */
    @RequestMapping(value = "/getMetadataKnownFile", method = RequestMethod.POST)
    @ResponseBody
    public String getMetadataKnownFile(UploadedAutoconvertFile file, HttpServletRequest request) {
        String uniqueId = file.getUniqueId();
        String convertFrom = file.getConvertFrom();

        String metadataStr = "";

        String filePath = FilenameUtils.concat(getUploadDir(), uniqueId);
        String fileName = file.getFileName();
        String fullFileName = FilenameUtils.concat(filePath, fileName);

        String downloadDir = FilenameUtils.concat(getDownloadDir(), uniqueId);
        File downloadFileDir = new File(downloadDir);
        if (!downloadFileDir.exists()) {
            downloadFileDir.mkdir();
        }
        HashMap<String,String> globalMetadata = new HashMap<String, String>();
        // get metadata
        if (convertFrom != null) {
            // tag base archive flat file
            if (convertFrom.equals("tbaff")) {
                TagBaseArchivalTag tbaffConverter = new TagBaseArchivalTag();
                tbaffConverter.parse(fullFileName);
                globalMetadata = tbaffConverter.getGlobalMetadata();
            }

        }
        StringBuilder sb = new StringBuilder();
        for (String key : globalMetadata.keySet()) {
            sb.append(key + ":" + globalMetadata.get(key).trim().replace("\"","") + ",");
        }
        sb.deleteCharAt(sb.length()-1);
        metadataStr = sb.toString();
        // need to return something like this:
        // "title:title here,description:des here,institution:inst here,dataAuthor:data aut here,version:version here,dataSource:source here"
        return metadataStr;
    }

    /**
     * Accepts a POST request to restore session from an NcML file,
     *
     * @param file The UploadedFile form backing object containing the file.
     * @return A String of the local file name for the ASCII file (or null for an error).
     */
    @RequestMapping(value = "/restoreFromZip", method = RequestMethod.POST)
    @ResponseBody
    public String processZip(UploadedFile file) {
        String jsonStrSessionStorage = null;
        String filePath = FilenameUtils.concat(getUploadDir(),
                file.getUniqueId());
        filePath = FilenameUtils.concat(filePath, file.getFileName());
        if (filePath.endsWith(".zip")) {
            ZipFileUtil restoreZip = new ZipFileUtil(filePath);
            jsonStrSessionStorage = restoreZip
                    .readFileFromZip("rosettaSessionStorage.json");
        } else if (filePath.endsWith(".template")) {
            InputStream inStream = null;
            jsonStrSessionStorage = "";
            String line;
            try {
                inStream = new FileInputStream(filePath);
                BufferedReader buffReader = new BufferedReader(
                        new InputStreamReader(inStream));
                while ((line = buffReader.readLine()) != null) {
                    jsonStrSessionStorage = jsonStrSessionStorage + line;
                }
                buffReader.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return jsonStrSessionStorage;
    }

    /**
     * Accepts a POST request to to initiate a quick save (download a temp save
     * template)
     *
     * @return A String of the local file name for the ASCII file (or null for an error).
     */
    @RequestMapping(value = "/QuickSave", method = RequestMethod.POST)
    @ResponseBody
    public String quickSave(String jsonStrSessionStorage) {
        Boolean isQuickSave = true;
        Map<String, String> infoForDownload = new HashMap<>();
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObj = null;
        try {
            jsonObj = (JSONObject) jsonParser.parse(jsonStrSessionStorage);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String userFileName = jsonObj.get("fileName").toString();
        String jsonFileName = getTemplateFileName(userFileName, isQuickSave);

        // map to be returned to front end to construct download request
        infoForDownload.put("uniqueId", jsonObj.get("uniqueId").toString());
        infoForDownload.put("fileName", jsonFileName);

        // crete JSON file that holds sessionStorage information
        String downloadDir = FilenameUtils.concat(getDownloadDir(), jsonObj
                .get("uniqueId").toString());

        String jsonOut = FilenameUtils.concat(downloadDir, jsonFileName);

        jsonObj.remove("uniqueId");
        jsonObj.remove("fileName");

        JsonUtil jsonUtil = new JsonUtil(jsonOut);
        jsonUtil.writeJsonToFile(jsonObj);
        // return enough info to download the template file
        String returnString = JSONObject.toJSONString(infoForDownload);
        return returnString;
    }

/*
    @RequestMapping(value = "/publish", method = RequestMethod.POST)
    @ResponseBody
    public String publish(PublisherInfo publisherObj) {
        BasicConfigurator.configure();

        String userId = publisherObj.getUserName();
        String passwd = publisherObj.getAuth();
        HashMap<String, String> pubMap = (HashMap<String, String>) publisherObj
                .getPublisherInfoMap();
        String server = pubMap.get("pubUrl");
        String parent = pubMap.get("pubDest");

        String entryName = publisherObj.getGeneralMetadataMap().get("title");
        String entryDescription = publisherObj.getGeneralMetadataMap().get(
                "description");

        String downloadDir = FilenameUtils.concat(getDownloadDir(),
                publisherObj.getUniqueId());
        String ncFileName = FilenameUtils.removeExtension(publisherObj
                .getFileName()) + ".nc";
        String filePath = FilenameUtils.concat(downloadDir, ncFileName);
        String msg = "";
        boolean success;
        try {
            if (server.toLowerCase().contains("cadis")) {
                AcadisGatewayProjectReader projectReader = new AcadisGatewayProjectReader(
                        parent);
                parent = projectReader.getDatasetShortName();
                AcadisGateway pub = new AcadisGateway(server, userId, passwd,
                        parent, filePath);
                success = pub.publish();
                if (success) {
                    msg = pub.getGatewayProjectUrl();
                } else {
                    System.err.println("Failed to publish to ACADIS Gateway");
                }
            }
        } catch (Exception e) {
            msg = e.getMessage();
        }

        return msg;
    }
*/

    /**
     * Accepts a GET request to download a file from the download directory.
     *
     * @param uniqueID the sessions unique ID.
     * @param fileName the name of the file the user wants to download from the download directory
     * @return IOStream of the file requested.
     */
    @RequestMapping(value = "/fileDownload/{uniqueID}/{file:.*}", method = RequestMethod.GET)
    public void fileDownload(@PathVariable(value = "uniqueID") String uniqueID,
                             @PathVariable(value = "file") String fileName,
                             HttpServletResponse response) {
        String relFileLoc = uniqueID + File.separator + fileName;
        String fullFilePath = FilenameUtils
                .concat(getDownloadDir(), relFileLoc);
        File requestedFile = new File(fullFilePath);
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(requestedFile);
            String ext = FilenameUtils.getExtension(fileName);
            String contentType = "text/plain";
            if (ext.equals("template")) {
                contentType = "application/rosetta";
            } else if (ext.equals("nc")) {
                contentType = "application/x-netcdf";
            } else if (ext.equals("zip")) {
                contentType = "application/zip";
            }
            response.setHeader("Content-Type", contentType);
            // copy it to response's OutputStream
            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * Attempts to get the client IP address from the request.
     *
     * @param request The HttpServletRequest.
     * @return The client's IP address.
     */
    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = null;
        if (request.getRemoteAddr() != null) {
            ipAddress = request.getRemoteAddr();
            ipAddress = StringUtils.deleteWhitespace(ipAddress);
            ipAddress = StringUtils.trimToNull(ipAddress);
            ipAddress = StringUtils.lowerCase(ipAddress);
            ipAddress = StringUtils.replaceChars(ipAddress, ".", "");
        }
        return ipAddress;
    }

    /**
     * Creates a unique id for the file name from the clients IP address and the
     * date.
     *
     * @param request The HttpServletRequest.
     * @return The unique file name id.
     */
    private String createUniqueId(HttpServletRequest request) {
        String id = new Integer(new Date().hashCode()).toString();
        String ipAddress = getIpAddress(request);
        if (ipAddress != null) {
            id = ipAddress + id;
        } else {
            id = new Integer(new Random().nextInt()).toString() + id;
        }
        return id.replaceAll(":", "_");
    }

    private String getTemplateFileName(String userFileName, Boolean isQuickSave) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd_HHmmss")
                .format(new Date());
        String jsonFileName = FilenameUtils.removeExtension(userFileName);
        jsonFileName = jsonFileName + "-Rosetta_" + currentDate;
        jsonFileName = jsonFileName + ".template";
        if (isQuickSave) {
            jsonFileName = "QuickSave_" + jsonFileName;
        }
        return jsonFileName;
    }

    /**
     * This method gracefully handles any uncaught exception that are fatal in
     * nature and unresolvable by the user.
     *
     * @param arg0      The current HttpServletRequest request.
     * @param arg1      The current HttpServletRequest response.
     * @param arg2      The executed handler, or null if none chosen at the time of the exception.
     * @param exception The exception that got thrown during handler execution.
     * @return The error page containing the appropriate message to the user.
     */
    @Override
    public ModelAndView resolveException(HttpServletRequest arg0,
                                         HttpServletResponse arg1, Object arg2, Exception exception) {
        String message = "";
        if (exception instanceof MaxUploadSizeExceededException) {
            // this value is declared in the /WEB-INF/rosetta-servlet.xml file
            // (we can move it elsewhere for convenience)
            message = "File size should be less than "
                    + ((MaxUploadSizeExceededException) exception)
                    .getMaxUploadSize() + " byte.";
        } else {
            message = "An error has occurred: "
                    + exception.getClass().getName() + ":"
                    + exception.getMessage();
        }
        // log it
        logger.error(message);
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("message", message);
        ModelAndView modelAndView = new ModelAndView("fatalError", model);
        return modelAndView;
    }

}
