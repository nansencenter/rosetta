package edu.ucar.unidata.rosetta.dsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import edu.ucar.unidata.rosetta.domain.AsciiFile;
import edu.ucar.unidata.rosetta.dsg.util.DateTimeBluePrint;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayScalar;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;

public class SingleStationProfile extends NetcdfFileManager {

    protected static Logger logger = Logger.getLogger(SingleStationProfile.class);
                    
    public SingleStationProfile() {
        super.setMyCfRole("profile_id");
        super.setMyDsgType("profile");
    }

    @Override
    protected NetcdfFileWriter writeCoordVarsFromPlatform(NetcdfFileWriter ncFileWriter, String name) throws IOException, InvalidRangeException {
        String coordVarVal = getPlatformMetadataMap().get(name);

        String varName = name;
        float value = 0f;
        // TODO: consider using DateTimeBluePrint?
        if (name.equals("time")) {
            Calendar t0C = DatatypeConverter.parseTime(DateTimeBluePrint.getZeroDate());
            Date t0 = t0C.getTime();
            Calendar t1C = DatatypeConverter.parseTime(coordVarVal);
            Date t1 = t1C.getTime();
            // Time since in miliseconds
            value = (float) (t1.getTime() - t0.getTime());
            // seconds
            value /= 1000;
            // minutes
            value /= 60;
            // hours
            value /= 60;
            // days
            value /= 24;
        } else {
            varName = name.substring(0, 3);
            value = Float.parseFloat(coordVarVal);
        }
        Variable theVar = ncFileWriter.findVariable(varName);

        ncFileWriter.write(theVar, new ArrayScalar(value));

        return ncFileWriter;
    }

    @Override
    protected NetcdfFileWriter createCoordVarsFromPlatform(NetcdfFileWriter ncFileWriter, String name) throws IOException, InvalidRangeException {
        String coordVarUnits = getPlatformMetadataMap().get(name + "Units");

        String varName = name;
        //System.out.println("Writing:"+varName);
        if (!name.equals("time")) {
            varName = name.substring(0, 3);
        } else {
            coordVarUnits = "days since " + DateTimeBluePrint.getZeroDate();
        }
        Variable theVar = ncFileWriter.addVariable(null, varName, DataType.FLOAT, "");

        if (varName.equals("alt"))
            ncFileWriter.addVariableAttribute(theVar, new Attribute("axis", "Z"));

        ncFileWriter.addVariableAttribute(theVar, new Attribute("units", coordVarUnits));
        ncFileWriter.addVariableAttribute(theVar, new Attribute("standard_name", name));
        ncFileWriter.addVariableAttribute(theVar, new Attribute("_platformMetadata", "true"));
        ncFileWriter.addVariableAttribute(theVar, new Attribute("long_name", name));

        //System.out.println("Wrote coordvar: " + varName);
        return ncFileWriter;
    }

    @Override
    protected NetcdfFileWriter createPlatformInfo(NetcdfFileWriter ncFileWriter, AsciiFile file) {
        String stationName = getPlatformMetadataMap().get("platformName").toString().replaceAll(" ", "_");

        Variable theVar = ncFileWriter.addStringVariable(null, "station_name", new ArrayList<Dimension>(), stationName.length());

        theVar.addAttribute(new Attribute("cf_role", getMyCfRole()));
        theVar.addAttribute(new Attribute("long_name", "Station or platform name"));
        theVar.addAttribute(new Attribute("_platformMetadata", "true"));

        return ncFileWriter;
    }

    @Override
    protected NetcdfFileWriter writePlatformInfo(NetcdfFileWriter ncFileWriter, AsciiFile file) {
        String stationName = getPlatformMetadataMap().get("platformName").toString().replaceAll(" ", "_");
        char[] stationNameCharArray = stationName.toCharArray();
        Variable theVar = ncFileWriter.findVariable("station_name");

        ArrayChar sa = new ArrayChar.D1(stationName.length());
        for (int i = 0; i < stationName.length(); i++) {
            sa.setChar(i, stationNameCharArray[i]);
        }

        try {
            ncFileWriter.write(theVar, sa);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidRangeException e) {
            e.printStackTrace();
        }
        return ncFileWriter;
    }

    @Override
    protected void createCoordinateAttr(boolean hasRelTime) {
        // if we have a relTime (a complete time variable), use that variable
        // name
        // otherwise, use a (yet-to-be-created) variable called "time"
        // TODO: fix depth and add "postitive:down"
        if (hasRelTime) {
            setCoordAttr(getRelTimeVarName() + " lat lon alt");
        } else {
            setCoordAttr("time lat lon alt");
        }
    }
}
