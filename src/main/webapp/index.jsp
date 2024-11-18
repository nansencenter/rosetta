<!DOCTYPE HTML>
<%@ page import="edu.ucar.unidata.rosetta.service.ServerInfoBean" %>
<html>
<head>
    <style>
        h1 {
            font-size: 250%;
        }

        h2 {
            font-size: 200%;
        }

        td {
            text-align: left;
            vertical-align: top;
            padding: 15px;
            font-size: 150%;
        }

        img {
            padding-top: 25pt;
            width: 200px;
        }
        #about {
        	font-size : 50%
        }
        button {
        	padding: 1em;
        	margin: 1em;
        	width: 30em;
        	border-radius: 1em;
        	border-width: 0px;
        	font-size: 1em;
        	background-color:rgb(230,240,255);
        	box-shadow: 5px 5px 10px 1px black;
        }
        button:hover {
        	background-color:rgb(220,230,255);
        }
        button:active {
        box-shadow: 0.5px 0.5px 10px 1px black;
        }
    </style>
    <title>Rosetta</title>
</head>
<body>
<table>
    <tr>
        <td>
            <a href="https://www.unidata.ucar.edu/software/rosetta/"><img src="resources/img/logo/rosetta-150x150.png" alt="Rosetta"></a>
            <br>
            <a href="http://nmdc.no"><img src="resources/img/logo/nmdc_logo.png" alt="NMDC"></a>
            <br>
            <a href="https://www.nordatanet.no/"><img src="resources/img/logo/NorDataNet-logo.png" alt="NorDataNet"></a>
            <br>
            <a href="https://sios-svalbard.org/"><img src="resources/img/logo/SIOS_logo.png" alt="SIOS"></a>
        </td>
        <td>
            
            <h1>Rosetta</h1>
            
            <p>
                <b><i>This specific version of Rosetta has been tailored for NMDC, NorDataNet and SIOS.</i></b>
            </p>
            <p>
                Welcome to Rosetta, a data transformation tool. Rosetta is a web-based service that
                provides an easy, wizard-based interface for data collectors to transform their
                datalogger generated ASCII output into Climate and Forecast (CF) compliant netCDF
                files. These files will contain the metadata describing what data is contained in
                the file, the instruments used to collect the data, and other critical information
                that otherwise may be lost in one of many dreaded README files.
            </p>

            <p>
                We hope that Rosetta will be of value to the science community users who have needs
                for transforming the data they have collected or stored in non-standard formats.
            </p>
            <p>
                Rosetta is currently under continued further development.
            </p>
            
            <h2>What would you like to do?</h2>
            
            <a href="create"><button type="button">Convert a file to the netCDF format and create a new template</button></a>
            <br>
            <a href="restore"><button type="button">Upload, modify, and use an existing template</button></a>
            <br>
        </td>
    </tr>
    <tr>
    	<td></td>
    	<td><a href="https://drive.google.com/file/d/1Ss7G2kHZipBWLn28CdZooxDiXLztRQlp/view">Rosetta User Manual</a></td>
    </tr>
    <tr><td></td><td><a href='https://drive.google.com/file/d/1xeMNAjI1v-bjTQQEyAnnB-KNrboxEQzS/view?usp=drive_link'>PDF form</a> for General metadata import</td></tr>
    <tr><td colspan=2><hr></td></tr>
    <tr>
        <td id="about" colspan="2">
            <i>
                <br>
                Version : <%=ServerInfoBean.getVersion()%>
                <br>
                Build Date: <%=ServerInfoBean.getBuildDate()%>
            </i>
        </td>
    </tr>



</table>
</body>
</html>
