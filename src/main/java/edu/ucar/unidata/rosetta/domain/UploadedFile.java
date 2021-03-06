package edu.ucar.unidata.rosetta.domain;

import org.springframework.web.multipart.commons.CommonsMultipartFile;

/**
 * Object representing an UploadedFile.
 *
 * An arbitrary entity representing a CommonsMultipartFile
 * file uploaded to the local file system by a user.
 */
public class UploadedFile {

    private CommonsMultipartFile file = null;
    private String fileName = null;
    private String uniqueId = null;

    /*
     * Returns the uploaded file in CommonsMultipartFile format.
     *
     * @return  The CommonsMultipartFile file.
     */
    public CommonsMultipartFile getFile() {
        return file;
    }

    /*
     * Sets the uploaded file as a CommonsMultipartFile file.
     * The file is uploaded via an asynchronous AJAX call.
     *
     * @param file  The CommonsMultipartFile file.
     */
    public void setFile(CommonsMultipartFile file) {
        setFileName(file.getOriginalFilename());
        this.file = file;
    }


    /*
     * Returns the name of the uploaded file.
     *
     * @return  The uploaded file name.
     */
    public String getFileName() {
        return fileName;
    }

    /*
     * Sets the file name of the uploaded file.
     *
     * @param fileName  The uploaded file name.
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /*
     * Returns the unique ID of the uploaded file.
     *
     * @return  The unique ID.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /*
     * Sets the unique ID of the uploaded file.
     *
     * @param  The unique ID.
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }


}
