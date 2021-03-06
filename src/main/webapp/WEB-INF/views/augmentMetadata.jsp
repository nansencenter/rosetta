<%--
  ~ Copyright (c) 2012-2017 University Corporation for Atmospheric Research/Unidata
  --%>

<!DOCTYPE HTML>
<%@ include file="/WEB-INF/views/includes/taglibs.jsp" %>
<%@ page import="edu.ucar.unidata.rosetta.service.ServerInfoBean" %>
<html>
<head>
    <title><spring:message code="global.title"/></title>
    <%@ include file="/WEB-INF/views/includes/css.jsp" %>
    <link type="text/css" rel="stylesheet" href="resources/css/augmentMetadata.css"/>
    <link type="text/css" rel="stylesheet" href="resources/css/augmentMetadata.css"/>
    <%@ include file="/WEB-INF/views/includes/javascript.jsp" %>
    <script type="text/javascript" src="resources/js/augmentMetadata.js"></script>
    <script type="text/javascript">
        var generalMetadata = [];
        $.metadata.setType("attr", "validate");
    </script>
</head>
<body>
<a href="http://oip.jpl.nasa.gov" target="_blank"><img id="oiip-logo" src="resources/img/logo/oiip-50x50.png" alt="OIIP - Oceanographic In-situ Interoperability Project" /></a><h1><spring:message code="global.title"/></h1>
<form id="FORM" action="/rosetta/upload" method="POST" enctype="multipart/form-data">
    <div id="step0" title="<spring:message code="step1.title"/>">
        <h5><spring:message code="step1.description"/></h5>
        <%@include file="/WEB-INF/views/jspf/uploadKnownFile.jspf" %>
    </div>

    <div id="step1" title="Select known file type">
        <h5>"What type of file did you upload?"</h5>
        <%@include file="/WEB-INF/views/jspf/selectKnownFileType.jspf" %>
    </div>

    <div id="step2" title="Parsing file">
        <img id="parsing-img" src="resources/img/pacman.gif">
        <div id="finished-parsing"></div>
    </div>

    <div id="step3" title="Augment Metadata">
        <h5>Add or Change the Metadata</h5>
        <c:choose>
            <c:when test="${fn:length(oiipGlobalMetadataItems) gt 0}">
                <ul class="oiipGlobalMetadata">
                    <c:forEach items="${oiipGlobalMetadataItems}" var="oiipGlobalMetadataItem">
                        <script type="text/javascript">
                            var obj = {};
                            obj["tagName"] = "<c:out value="${oiipGlobalMetadataItem.tagName}" />";
                            obj["displayName"] = "<c:out value="${oiipGlobalMetadataItem.displayName}" />";
                            <c:choose>
                            <c:when test="${oiipGlobalMetadataItem.isRequired != null}">
                            obj["isRequired"] = true;
                            </c:when>
                            <c:otherwise>
                            obj["isRequired"] = false;
                            </c:otherwise>
                            </c:choose>
                            <c:choose>
                            <c:when test="${oiipGlobalMetadataItem.units != null}">
                            obj["units"] = true;
                            </c:when>
                            <c:otherwise>
                            obj["units"] = false;
                            </c:otherwise>
                            </c:choose>
                            generalMetadata.push(obj);
                        </script>
                        <li>
                            <c:choose>
                                <c:when test="${oiipGlobalMetadataItem.isRequired}">
                                    <label class="empty required">
                                </c:when>
                                <c:otherwise>
                                    <label class="empty">
                                </c:otherwise>
                            </c:choose>
                                <c:choose>
                                    <c:when test="${oiipGlobalMetadataItem.isRequired}">
                                        *
                                    </c:when>
                                </c:choose>
                                <c:out value="${oiipGlobalMetadataItem.displayName}"/>
                                <c:choose>
                                    <c:when test="${oiipGlobalMetadataItem.description != null}">
                                        <img src="resources/img/help.png"
                                             alt="<c:out value="${oiipGlobalMetadataItem.description}" />"/>
                                    </c:when>
                                </c:choose>
                                <br/>
                                <input type="text" name="<c:out value="${oiipGlobalMetadataItem.tagName}" />"
                                       value=""/>
                            </label>
                            <label class="error"></label>
                        </label>
                    </c:forEach>
                    <div id="containerForCustomAttributes"></div>
                </ul>
            </c:when>
            <c:otherwise>
                <!-- insert error handling -->
            </c:otherwise>
        </c:choose>
        <!--%@include file="/WEB-INF/views/jspf/convertAndDownload.jspf" %-->
    </div>

    <div id="step4" title="<spring:message code="step7.title"/>">
        <h5><spring:message code="step7.description"/></h5>
        <%@include file="/WEB-INF/views/jspf/convertAndDownload.jspf" %>
    </div>

    <%@include file="/WEB-INF/views/jspf/footerNoQs.jspf" %>
</form>
</body>
</html>
