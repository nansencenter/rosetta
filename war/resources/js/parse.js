var cfStandards = [];
var cfStandardUnits = {};
var metadata = [];

function drawGrid(data, step) {
    var grid;

    function loadCFStandards() {
        $.get('resources/cf-standard-name-table.xml', 
            function(data) {
                var s = [];
                $(data).find("entry").each(function() {
                    s.push($(this).attr("id"));
                }); 
                cfStandards = s;
            }, 
        "xml");  
    }

    loadCFStandards(); 


    function loadCFStandardUnits() {
        $.get('resources/cf-standard-name-table.xml', 
            function(data) {
                var u = {};
                $(data).find("entry").each(function() {
                    u[$(this).attr("id")] = $(this).find("canonical_units").text();
                }); 
                cfStandardUnits = u;
            }, 
        "xml");  
    }

    loadCFStandardUnits(); 


    function loadMetadata() {
        $.get('resources/metadata.xml', 
            function(data) {
                var m = [];
                $(data).find("entry").each(function() {
                    var e = {};
                    e["entry"] = $(this).attr("id");
                    e["type"] = $(this).find("type").text();
                    e["necessity"] = $(this).find("necessity").text();
                    m.push(e);
                }); 
                metadata = m;
            }, 
        "xml");
    }

    loadMetadata(); 

    var LineNumberFormatter = function (row, cell, value, columnDef, dataContext) {
        if (dataContext.parent != null){
            if (dataContext.id == dataContext.parent) {
                if (dataContext._collapsed) {
                    return " <span class='toggle expand'></span>" + value;
                } else {
                    return " <span class='toggle collapse'></span>" + value;
                }            
            } else {
                return value;
            }
        } else {
            return value;
        }
    };

    var fileData = data.split(/\r\n|\r|\n/g);
    var columns = [
            {
                id: "line_number", 
                name: "#",
                field: "line_number",
                width: 45,
                cssClass: "lineNumber",
                headerCssClass: "lineNumber",
                resizable: false,
                sortable: false
            }

        ];
    var rows = [];

    if (step == "3") {
        var options = {
            editable: false,
            enableAddRow: false,
            enableColumnReorder: false,
            forceFitColumns: true,
        };
        columns.push(
            {
                id: "line_data", 
                name: "Line Data",
                field: "line_data",
                width: 1000,
                resizable: false,
                sortable: false
            }
        );
        $(function () {           
            for (var i = 0; i <fileData.length; i++) {
                if (fileData[i] != "") {
                    rows[i] = {
                        line_number: i,
                        line_data: fileData[i]
                    };
                 }
            }

            var checkboxSelector = new Slick.CheckboxSelectColumn({
                cssClass: "slick-cell-checkboxsel"
            });
            columns.unshift(checkboxSelector.getColumnDefinition());

            grid = new Slick.Grid("#step" + step + " #myGrid", rows, columns, options);
            var rowModel = new Slick.RowSelectionModel({selectActiveRow: false});

            grid.setSelectionModel(rowModel);
            grid.registerPlugin(checkboxSelector);
         
            // if user has landed on this step before and specified what is checked, add that to the grid
            if (sessionStorage.headerLineNumbers) { 
                 var lines = sessionStorage.getItem('headerLineNumbers').split(/,/g);
                 grid.setSelectedRows(lines.sort(function(a,b){return a-b}));
            }
            bindGridEvent(grid);

        });
    } else {  
        var inputSought;
        if (step == "5") {
            inputSought = "Name";
        } else if (step == "6") {
            inputSought = "Units";
        } else {
            inputSought = "Metadata";
        }
        var options = {
            editable: false,
            enableAddRow: false,
            enableColumnReorder: false,
            forceFitColumns: true,
            enableCellNavigation: true,
            showHeaderRow: true
        };

        var dataView;
        columns[0]["formatter"] = LineNumberFormatter;
        $(function () {    
            var headerLines = sessionStorage.getItem('headerLineNumbers').split(/,/g);
            var firstHeaderLine = headerLines[0];
            var delimiter = fileData.shift();
            var colNumber;
            for (var i = 0; i < fileData.length; i++) {  
                var parent = null;  
                if (fileData[i] != "") {               
                    if (i == 0) {
                        bool = 1;
                    }      
                    var obj = {"line_number": i, "id": i};                            
                    if (jQuery.inArray(i.toString(), headerLines) < 0) {
                        var dataItems = fileData[i].split(delimiter);
                        if (bool == 1) { 
                            columns =  makeColumns(columns, dataItems.length, inputSought);
                            colNumber = dataItems.length;
                            bool = 0;
                        }
                        for (var x = 0; x < dataItems.length; x++) {                          
                             obj[x] = dataItems[x];                         
                        } 
                    } else {                                                
                         obj[0] = fileData[i];  
                         parent = firstHeaderLine;
                         if (i == firstHeaderLine) {
                             obj["_collapsed"] = true;     
                         }
                    }
                    obj["parent"] = parent;     
                    rows[i] = obj;                    
                }
   
            }        

            // initialize the model
            dataView = new Slick.Data.DataView({ inlineFilters: true });
            dataView.beginUpdate();
            dataView.setItems(rows);
            dataView.setFilter(headerLineFilter);                 
            dataView.setFilterArgs(rows);
            dataView.endUpdate();

            dataView.getItemMetadata  = function (i) { 
                if (i == firstHeaderLine) {
                    return {
                        "cssClasses": "headerRow",
                        "columns": {
                            1: {
                                "colspan": colNumber
                            }
                         }
                    };
                }
            };  

            grid = new Slick.Grid("#step" + step + " #myGrid", dataView, columns, options);

            grid.onClick.subscribe(function (e, args) {
                if ($(e.target).hasClass("toggle")) {
                    var item = dataView.getItem(args.row);
                    if (item) {
                        if (!item._collapsed) {
                            dataView.getItemMetadata  = function (i) { 
                                if (i == firstHeaderLine) {
                                    return {
                                        "cssClasses": "headerRow",
                                        "columns": {
                                            1: {
                                                "colspan": colNumber
                                            }
                                         }
                                    };
                                } else {
                                    return null;
                                }
                            };  
                            item._collapsed = true;
                        } else {
                            dataView.getItemMetadata  = function (i) { 
                                if (jQuery.inArray(i.toString(), headerLines) >= 0) {
                                    return {
                                        "cssClasses": "headerRow",
                                        "columns": {
                                            1: {
                                                "colspan": colNumber
                                            }
                                         }
                                    };
                                }
                            };  
                            item._collapsed = false;
                        }
                        dataView.updateItem(item.id, item);
                        checkIfColumnIsDisabled (colNumber, inputSought, grid); 
                    }
                    e.stopImmediatePropagation();
                }
            });

            // wire up model events to drive the grid
            dataView.onRowCountChanged.subscribe(function (e, args) {
                grid.updateRowCount();
                grid.render();
            });

            dataView.onRowsChanged.subscribe(function (e, args) {
                grid.invalidateRows(args.rows);
                grid.render();
            });

            grid.onScroll.subscribe(function (e, args) {
                checkIfColumnIsDisabled (colNumber, inputSought, grid); 
            });


            if (inputSought == "Name") {

                var headerMenuPlugin = new Slick.Plugins.HeaderMenu(); 

                headerMenuPlugin.onCommand.subscribe(function(e, args) {   
                    var id = args.column.id;
                    var attributeValue = "variable" + id;
                    var header = grid.getHeaderRowColumn(args.column.id);
                    $(header).empty(); 
                    var item = dataView.getItem(0);

                    populateInput (colNumber, inputSought, grid);
                    checkIfColumnIsDisabled (colNumber, inputSought, grid); 

                    if (args.command == "standard") {   
                        
                        $(".slick-headerrow-columns").removeClass("hideMe");
                        $("#" + attributeValue).autocomplete({
			    source: cfStandards, 
                                    delay: 0
		        });

                        $("#" + attributeValue).bind('autocompletechange', function() {
                            if (validateVariableNames (colNumber, $(this).val())) {
                                $(".jw-step:eq(" + (step - 1) + ")").find("label.error").text("That variable name has already been specified.  Please choose another.");
                            } else {
                                if (item) {
                                    if (!item._collapsed) {
                                        for (var i = 0; i < fileData.length; i++) { 
                                            if (jQuery.inArray(i.toString(), headerLines) < 0) {
                                                enableColumn (grid.getCellNode(i, (id + 1)));    
                                            }
                                        }
                                    } else {
                                        for (var i = 0; i < fileData.length; i++) { 
                                           if (i != firstHeaderLine) {     
                                                 enableColumn (grid.getCellNode(i, (id + 1)));
                                           }
                                        }
                                    }
                                }
                                $(".jw-step:eq(" + (step - 1) + ")").find("label.error").text("");
                                addToSession(attributeValue, $(this).val());
                                grid.updateColumnHeader(id, $(this).val(), $(this).val());
                                $(".slick-headerrow").addClass("hideMe");
                                testIfComplete (colNumber, inputSought);
                            }
                        });

                    } else if (args.command == "other") {
                        $(".slick-headerrow-columns").removeClass("hideMe");
                        $("#" + attributeValue).bind('focusout', function() {
                            if (validateVariableNames (colNumber, $(this).val())) {
                                $(".jw-step:eq(" + (step - 1) + ")").find("label.error").text("That variable name has already been specified.  Please choose another.");
                            } else {
                                if (item) {
                                    if (!item._collapsed) {
                                        for (var i = 0; i < fileData.length; i++) { 
                                            if (jQuery.inArray(i.toString(), headerLines) < 0) {
                                                enableColumn (grid.getCellNode(i, (id + 1)));    
                                            }
                                        }
                                    } else {
                                        for (var i = 0; i < fileData.length; i++) { 
                                            if (i != firstHeaderLine) {     
                                               enableColumn (grid.getCellNode(i, (id + 1)));
                                            }
                                        }
                                    }
                                }
                                $(".jw-step:eq(" + (step - 1) + ")").find("label.error").text("");
                                addToSession(attributeValue, $(this).val());
                                grid.updateColumnHeader(id, $(this).val(), $(this).val());
                                $(".slick-headerrow").addClass("hideMe");
                                testIfComplete (colNumber, inputSought);
                            }  
                        });
                    } else { 
                        if (item) {
                            if (!item._collapsed) {
                                for (var i = 0; i < fileData.length; i++) { 
                                    if (jQuery.inArray(i.toString(), headerLines) < 0) {
                                        disableColumn (grid.getCellNode(i, (id + 1)));    
                                    }
                                }
                            } else {
                                for (var i = 0; i < fileData.length; i++) { 
                                    if (i != firstHeaderLine) {     
                                       disableColumn (grid.getCellNode(i, (id + 1)));
                                    }
                                }
                            }       
                            addToSession(attributeValue, "Do Not Use");                     
                            grid.updateColumnHeader(id, "Do Not Use", "Do not use the data from this column");
                            $(".slick-headerrow-columns").addClass("hideMe");
                            testIfComplete (colNumber, inputSought);
                        }
                    }
                    $(".slick-headerrow").removeClass("hideMe");                    
                });
                grid.registerPlugin(headerMenuPlugin);
                $(".slick-headerrow").addClass("hideMe"); 
            }

            if (inputSought == "Units") {
                $(".slick-headerrow-column").bind('focusout', function() {
                    addToSession($(event.target).attr("name"), $(event.target).attr("value"));
                    testIfComplete (colNumber, inputSought);
                });
            }

            if (inputSought == "Metadata") {
                var headerButtonsPlugin = new Slick.Plugins.HeaderButtons();
                $(".slick-headerrow-columns").addClass("hideMe");
                headerButtonsPlugin.onCommand.subscribe(function(e, args) {
                    var id = args.column.id;
                    var variableValue = "variable" + id;
                    var variableName = sessionStorage.getItem(variableValue);
                    var variableUnits = sessionStorage.getItem(variableValue + "Unit");
                    if (args.command == "setVariableMetadata") {
                        $(function() {
		            $( "#dialog" ).dialog({
                                height: 600,
         			width: 400,
			        modal: true,
                                buttons: {
				    "done": function() {
                                        validateVariableMetadata (variableName, $(this));                                      
                                    }, 
                                    "cancel": function() {
					$(this).dialog("close");
				    }
                                }
		            });
                            $("#dialog").empty();
                             var dialogForm =  "<form id=\"dialog\"><fieldset>" +
                            "<p>Metadata for: <b>" + variableName + "</b></p>" + 
                            "<p>Is this variable a coordinate variable? &nbsp;" + 
		            "<label>Yes<input type=\"radio\" name=\"" + variableName + "CoordVar\" id=\"coordVar\" value=\"yes\"/></label>" +
                            "<label>No<input type=\"radio\" name=\"" + variableName + "CoordVar\" id=\"coordVar\" value=\"no\"/></label> </p>" +
                            "<p>Specify data type: &nbsp;" + 
		            "<label>integer<input type=\"radio\" name=\"" + variableName + "VarDataType\" id=\"varDataType\" value=\"integer\"/></label>" +
		            "<label>float<input type=\"radio\" name=\"" + variableName + "VarDataType\" id=\"varDataType\" value=\"float\"/></label>" +
		            "<label>text<input type=\"radio\" name=\"" + variableName + "VarDataType\" id=\"varDataType\" value=\"text\"/></label> </p>" +
                            "</fieldset></form>";
                            $("#dialog").append(dialogForm);


                            var coordinateVariables = ["time", "latitude", "longitude", "altitude"];
                            for (var i = 0; i < coordinateVariables.length; i++) { 
                                if (variableName == coordinateVariables[i]) {
                                    $('input[id="coordVar"][value="yes"]').attr('checked', true);
                                    addMetadataOptionsToDialog ("coordinate", variableName, variableUnits);
                                } 
                            }
                            
                            $('input#coordVar').bind('click', function() {
                                var coordVarChoice = "non-coordinate";
                                if ($(this).val() == "yes") {
                                    coordVarChoice = "coordinate";
                                } 
                                addMetadataOptionsToDialog (coordVarChoice, variableName, variableUnits);
                            });
	                });
                    }
                });


                grid.registerPlugin(headerButtonsPlugin);
            }


            populateInput (colNumber, inputSought, grid);      
            checkIfColumnIsDisabled (colNumber, inputSought, grid);     
            testIfComplete (colNumber, inputSought);

        });

    } 
}

function bindGridEvent(grid) {
    // Step 3: selecting header lines
    grid.onSelectedRowsChanged.subscribe(function() { 
        addToSession("headerLineNumbers", grid.getSelectedRows().sort(function(a,b){return a-b}));
        $("#faux").remove();
        $(".jw-button-next").removeClass("hideMe");
    });
}


function bindAdditionalMetadataChooser(coordVarChoice, variableName, variableUnits) {
    $("img#additionalMetadataChooser").bind('click', function() {
        var metadataSelected = $('form#dialog .additionalElements select').val();
        var metadataChoices = $('form#dialog .additionalElements ol');      
        if ($(this).attr("alt") == "Add Metadata") {
            for (var i = 0; i < metadata.length; i++) {    
                var tag;                       
                var metadataItem = metadata[i];                        
                if (metadataItem.entry == metadataSelected) {
                    var value = getMetadataValue (metadataItem.entry, variableName, variableUnits); 
                    tag =  "<li><label>" + metadataSelected + "<input type=\"text\" name=\"" + variableName + "-" + metadataItem.entry  + "\" value=\"" + value + "\" id=\"" + metadataItem.necessity + "\"/></label></li>\n";                
                }
            }
            if ($('li:contains(' + metadataSelected + ')').length > 0) {
                $(".additionalElements").find("label.error").text("'" + metadataSelected + "' has already been selected.");
            } else {
                $(".additionalElements").find("label.error").text("");
                if ($(metadataChoices).length == 0) {
                    $('form#dialog .additionalElements').append("<ol>" + tag + "</ol>");
                } else {
                    $(metadataChoices).append(tag);
                }     
            }               
        } else {
            if ($('li:contains(' + metadataSelected + ')').length == 0) {
               $(".additionalElements").find("label.error").text("'" + metadataSelected + "' has NOT been selected and therefore cannot be removed.");
            } else {
                $(".additionalElements").find("label.error").text("");
                $('li:contains(' + metadataSelected + ')').remove();
                var listChildren = $('form#dialog .additionalElements ol > li');
                if ($(listChildren).length == 0) {               
                    $(metadataChoices).remove("ol");               
                }    
            }
        }
    });
}


function makeColumns(columns, columnCount, inputSought) {
    for (var x = 0; x < columnCount; x++) {  
        var colObject =  {
                id: x,  
                name: x,
                field: x,
                width: 100,
                resizable: false,
                sortable: false
        };
 
        if (inputSought == "Name") {
            colObject["headerCssClass"] = "dropdown";
            colObject["header"] = {
                menu: {
                    tooltip: "Specify Variable " + inputSought,
                    items: [
                        {
                            title: "Use a Standard Variable "  + inputSought,
                            command: "standard",
                            tooltip: "Specify an existing standard " + inputSought.toLowerCase()
                        },
                        {
                            title: "Other",
                            command: "other",
                            tooltip: "Specify a value not suggested"
                        },
                        {
                            title: "Do Not Use",
                            command: "do-not-use",
                            tooltip: "Do not use this column of data"
                        }
                    ]
                }
            }
        }

        if (inputSought == "Metadata") {
            colObject["headerCssClass"] = "metadata";
            colObject["header"] = {
                buttons: [
                    {
                        command: "setVariableMetadata",
                        tooltip: "Specify variable " + inputSought.toLowerCase()
                    }
                ]
            }
        }
        columns.push(colObject);
    }
    return columns;
}



function headerLineFilter(item, rows) {
    if (item.parent != null) {
        var parent = rows[item.parent];
        if (parent._collapsed) { 
            if (item.parent != item.id) {
                return false;
            }
        }
        parent = rows[parent.parent];
    }
    return true;
}

function testIfComplete (colNumber, inputSought) {
    for (var i = 0; i < colNumber; i++) {  
        if (inputSought == "Name") {    

            if (sessionStorage.getItem("variable" + i)) {
                if (i == (colNumber - 1)) {
                    $("#faux").remove();
                    $(".jw-button-next").removeClass("hideMe");
                } else {
                    continue;
                } 
            } else { 
                break;         
            }   
        } else if (inputSought == "Units") {
            if (sessionStorage.getItem("variable" + i + "Unit")) {
                if (i == (colNumber - 1)) {
                    $("#faux").remove();
                    $(".jw-button-next").removeClass("hideMe");
                } else {
                    continue;
                } 
            } else { 
                break;         
            }  
        } else {
        }         
    } 
}




function populateInput (colNumber, inputSought, grid) {       
    for (var i = 0; i < colNumber; i++) {  
        var header = grid.getHeaderRowColumn(i);
        $(header).empty(); 
        var variableName = "variable" + i;
        var variableValue = sessionStorage.getItem(variableName);   
        var input;
        var type = "text";
        if (inputSought == "Name") {  
            if (variableValue) {  
                input = "<input type=\"text\" name=\"" + variableName + "\" id=\"" + variableName + "\" value = \"" + variableValue + "\"/>"; 
            } else {
                input = "<input type=\"text\" name=\"" + variableName + "\" id=\"" + variableName + "\" value = \"\"/>"; 
            }  
        } else if (inputSought == "Units") {
            var variableUnitName= "variable" + i + "Unit";
            var variableUnitValue = sessionStorage.getItem(variableUnitName); 
            if (variableUnitValue) {
                if (variableValue == "Do Not Use") {
                    type = "hidden";
                }
                input = "<input type=\"" + type + "\" name=\"" + variableUnitName + "\" id=\"" + variableUnitName + "\" value = \"" + variableUnitValue + "\"/>"; 
            } else {                
                if (cfStandardUnits[variableValue]) {
                    variableUnitValue = cfStandardUnits[variableValue];
                    addToSession(variableUnitName, variableUnitValue);
                } else {
                    if (variableValue == "Do Not Use") {
                        type = "hidden";
                        variableUnitValue = variableValue;
                        addToSession(variableUnitName, variableUnitValue);
                    } else {
                        variableUnitValue = "";
                    }
                }
                input = "<input type=\"" + type + "\" name=\"" + variableUnitName + "\" id=\"" + variableUnitName + "\" value = \"" + variableUnitValue + "\"/>"; 
            }
        } else {
            // metadata
        }
        $(input).appendTo(header);       
        grid.updateColumnHeader(i, variableValue, variableValue);               
    } 
}




function checkIfColumnIsDisabled (colNumber, inputSought, grid) {        
    for (var i = 0; i < colNumber; i++) {  
        var userSelected = sessionStorage.getItem("variable" + i);                     
        if (userSelected) {
            if (userSelected == "Do Not Use") {
                var headerLines = sessionStorage.getItem('headerLineNumbers').split(/,/g);
                var firstHeaderLine = headerLines[0];
                var item = grid.getDataItem(0);
                if (item) {
                    var rowLength = grid.getDataLength();
                    if (!item._collapsed) {
                        for (var x = 0; x < rowLength; x++) { 
                            if (jQuery.inArray(x.toString(), headerLines) < 0) {
                                disableColumn (grid.getCellNode(x, (i + 1)));    
                            }
                        }
                    } else {
                        for (var x = 0; x < rowLength; x++) { 
                           if (x != firstHeaderLine) {     
                               disableColumn (grid.getCellNode(x, (i + 1)));
                           }
                        }
                    }
                }
            }
        }                   
    } 
}


function validateVariableNames (colNumber, userInput) {
    for (var i = 0; i < colNumber; i++) {                          
        var storedValue = sessionStorage.getItem("variable" + i);                        
        if (storedValue == userInput) {
            return true;
        }                      
    } 
    return false; 
}

function disableColumn (node) {
    $(node).addClass("columnDisabled");  
}

function enableColumn (node) {
    $(node).removeClass("columnDisabled");  
}

function populateAdditionalMetadata (varType, variableName, variableUnits) {
    var additional = "<select name=\"variableName\" id=\"additionalMetadata\">\n" 
    for (var i = 0; i < metadata.length; i++) {                          
        var metadataItem = metadata[i];                        
        var validate = "";
        var value = getMetadataValue (metadataItem.entry, variableName, variableUnits); 
        var tag;
        if (metadataItem.type == varType) {
            tag = createTagElement(metadataItem.entry, variableName + "-" + metadataItem.entry, value, metadataItem.necessity, validate); 
            if (metadataItem.necessity == "additional") {
                additional = additional + tag;
            }
        }
        if (metadataItem.type != "global") {
            if (metadataItem.type == "both") {
                tag = createTagElement(metadataItem.entry, variableName + "-" + metadataItem.entry, value, metadataItem.necessity, validate); 
                if (metadataItem.necessity == "additional") {
                    additional = additional + tag;
                }
            }
        }
    } 
    var error = "<label for=\"additionalMetadata\" class=\"error\"></label>\n";
    var additionalMetadataChooser = "<img src=\"resources/img/expand.png\" id=\"additionalMetadataChooser\" alt=\"Add Metadata\"/>" + "<img src=\"resources/img/collapse.png\" id=\"additionalMetadataChooser\" alt=\"Remove Metadata\"> ";
    additional = additional + "</select>\n";
    return additionalMetadataChooser + additional + error ;
}


function populateRecommendedMetadata (varType, variableName, variableUnits) {
    var recommended = "<ol>";
    for (var i = 0; i < metadata.length; i++) {                          
        var metadataItem = metadata[i];                        
        var validate = "";
        var value = getMetadataValue (metadataItem.entry, variableName, variableUnits); 
        var tag;
        if (metadataItem.type == varType) {
            tag = createTagElement(metadataItem.entry, variableName + "-" + metadataItem.entry, value, metadataItem.necessity, validate); 
            if (metadataItem.necessity == "recommended") {
                recommended = recommended + tag;
            }
        }
        if (metadataItem.type != "global") {
            if (metadataItem.type == "both") {
                tag = createTagElement(metadataItem.entry, variableName + "-" + metadataItem.entry, value, metadataItem.necessity, validate); 
                if (metadataItem.necessity == "recommended") {
                    recommended = recommended + tag;
                }
            }
        }
    } 
    recommended = recommended + "</ol>\n";
    return recommended;
}



function populateRequiredMetadata (varType, variableName, variableUnits) {
    var required = "<ol>";
    for (var i = 0; i < metadata.length; i++) {                          
        var metadataItem = metadata[i];                        
        var validate = "validate=\"required:true\"";
        var value = getMetadataValue (metadataItem.entry, variableName, variableUnits); 
        var tag;
        if (metadataItem.type == varType) {
            tag = createTagElement(metadataItem.entry, variableName + "-" + metadataItem.entry, value, metadataItem.necessity, validate); 
            if (metadataItem.necessity == "required") {
                required = required + tag;
            }
        }
        if (metadataItem.type != "global") {
            if (metadataItem.type == "both") {
                tag = createTagElement(metadataItem.entry, variableName + "-" + metadataItem.entry, value, metadataItem.necessity, validate); 
                if (metadataItem.necessity == "required") {
                    required = required + tag;
                }
            }
        }
    } 
    required = required + "</ol>\n";
    return required;
}



function getMetadataValue (entry, variableName, variableUnits) {
    var value = "";
    if (entry == "units") {
        value = variableUnits;
    }
    if (entry == "standard_name") {
        if (isCFStandardName(variableName)) {
            value = variableName;
        }
    }
    return value;
}

function isCFStandardName (value) {
    for (var i = 0; i < cfStandards.length; i++) {                          
        if (value == cfStandards[i]) {
            return true;
        } else {
            if (i == (cfStandards.length - 1)) {
                return false;
            }
        }
    }
}


function createTagElement (metadata, name, value, id, validate) {
    var tag;
    if (id == "additional") {
        tag =  "<option value=\"" + metadata + "\">" + metadata + "</option>\n";
    } else {
        tag = "<li>" + "<label for=\"" + name + "\" class=\"error\"></label>\n" + "<label>" + metadata + "<input type=\"text\" name=\"" + name  + "\" value=\"" + value + "\" id=\"" + id + "\" " + validate + "/></label></li>\n";
    }
    return tag;
}



function addMetadataOptionsToDialog (coordVarChoice, variableName, variableUnits) {
   $(".requiredElements").remove();
   var requiredTitle = "<p>Required metadata:</p>\n";
   var required = populateRequiredMetadata (coordVarChoice, variableName, variableUnits);
   $("#dialog fieldset").append("<div class=\"requiredElements\">" + requiredTitle + required + "</div>");

   $(".recommendedElements").remove();
   var recommendedTitle = "<p>Recommended metadata:</p>\n";
   var recommended = populateRecommendedMetadata (coordVarChoice, variableName, variableUnits);
   $("#dialog fieldset").append("<div class=\"recommendedElements\">" + recommendedTitle + recommended + "</div>");

   $(".additionalElements").remove();
   var additionalTitle = "<p>Additional metadata:</p>\n";
   var additional = populateAdditionalMetadata (coordVarChoice, variableName, variableUnits);
   $("#dialog fieldset").append("<div class=\"additionalElements\">" + additionalTitle + additional + "</div>");
   bindAdditionalMetadataChooser(coordVarChoice, variableName, variableUnits); 
}

function validateVariableMetadata (variableName, dialog) {
    var required = $($(dialog) + ".requiredElements :input").serializeArray();
    var error = false;
    for (var i = 0; i < required.length; i++) {    
        if (required[i].value == "") {
            $(".requiredElements").find("label[for=\"" + required[i].name + "\"]").text("This field is required:"); 
            error = true;
        } else {
            $(".requiredElements").find("label[for=\"" + required[i].name + "\"]").text(""); 
        }
        if (!error) {
            if (i == (required.length - 1)) {      
                saveVariableMetadata(variableName, dialog);          
                $(this).dialog("close");
            }
        }
    } 
}


function saveVariableMetadata (variableName, dialog) {
    var coordVar = $($(dialog) + "input[id='coordVar']").serializeArray();
    var varType = $($(dialog) + "input[id='varDataType']").serializeArray();
    var required = $($(dialog) + ".requiredElements :input").serializeArray();
    var recommended = $($(dialog) + ".recommendedElements :input").serializeArray();
    var additional = $($(dialog) + ".additionalElements ol :input").serializeArray();
    var input = coordVar.concat(coordVar, varType, required, recommended, additional);
    console.log(input);
    var metadata;
    for (var i = 0; i < required.length; i++) {    
        name = required[i].name.replace(variableName + "-", "");
        metadata = metadata + name + ":" + required[i].value;
    } 
    addToSession(variableName, metadata);
}

