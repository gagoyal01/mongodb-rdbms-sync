<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ page import="com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncUser" %>
<%@ page import="com.cisco.app.dbmigrator.migratorapp.constants.SyncConstants" %>
<%@ page import="com.cisco.app.dbmigrator.migratorapp.logging.entities.SyncConnectionInfo" %>
<%@ page import="java.util.*" %>
<%@ page import="com.google.gson.Gson" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
   
<!-- Metadata is data (information) about data. The <meta> tag provides metadata about the HTML document. Metadata will not be displayed on the page, but will be machine parsable. -->
        <meta http-equiv="X-UA-Compatible" content="IE=Edge,chrome=1"/>          <!-- Forces the browser to render as that particular version's standards -->
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />    <!-- Media type defined is 'text/html' and 'UTF-8' is the charset for HTML -->

<html>
<head>

<!-- <script type="text/javascript" src="/MigratorApp/src/main/webapp/WEB-INF/views/js/jquery-2.1.4.min.js" ></script> -->
<script  type="text/javascript" src="<%=request.getContextPath()%>/resources/js/jquery-2.1.4.min.js" >"></script>
<%-- <script type="text/javascript" src="<%=request.getContextPath()%>/WEB-INF/views/js/jquery-2.1.4.min.js">"></script> --%>

<script src="<%=request.getContextPath()%>/resources/js/lib/jquery-2.1.4.min.js"></script>
<script src="<%=request.getContextPath()%>/resources/js/lib/jquery-ui.min.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/lib/bootstrap.min.js"></script>
        <%-- <script src="<%=request.getContextPath()%>/resources/js/lib/angular.min.js"></script> --%>
        <script src="<%=request.getContextPath()%>/resources/js/lib/angular.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/lib/angular-route.min.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/lib/angular-resource.min.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/lib/angular-cookies.min.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/lib/smart-table.min.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/lib/angular-material.min.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/lib/angular-animate.min.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/lib/ui-bootstrap-tpls-1.2.4.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/lib/angular-aria.min.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/lib/angular-messages.min.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/lib/tabs.js"></script>
        <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/lib/bootstrap.min.css">
        <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/lib/jquery-ui.min.css">
        <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/lib/angular-material.min.css">
        <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/lib/bootstrap-theme.min.css">
        <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/font-awesome.min.css">
        <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/migrator.css" type="text/css"/>
        
        <script src="<%=request.getContextPath()%>/resources/js/DBMigratorApp.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/directives.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/homeViewControllers.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/controllers.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/selectDBViewControllers.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/migrateViewControllers.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/migrateViewSourceControllers.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/migrateViewTargetControllers.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/migrateReverseViewControllers.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/tableRelationsEditorController.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/adminViewControllers.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/DBAssignManagerView.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/services.js"></script>

<title>Sync</title>

</head>

<%
	SyncUser userInfo = (SyncUser)request.getSession().getAttribute(String.valueOf(SyncConstants.USER));
	Gson gson = new Gson();
	String userDetailJson = gson.toJson(userInfo);
	String userID = userInfo.getUserid();
	String filesPath = request.getContextPath();
	String userRole = "Admin";
%>

<%@ include file="/WEB-INF/views/jsp/header.jsp"%>

<body ng-app="DBMigratorApp">
	<%-- <div ng-include src="<%=filesPath%>/resources/templates/plugInTemplates.html"></div>
	<div ng-include src="<%=filesPath%>/resources/templates/popupFactoryTemplates.html"></div>
	<div ng-include src="<%=filesPath%>/resources/templates/tableRelationsEditorTemplate.html"></div> --%>
	<div ng-controller='mainPageView'>
		<div ng-include src="plugInTemplatesURL"></div>
		<div ng-include src="tableRelationTemplateURL"></div>
		<div ng-include src="popupFactoryTemplatesURL" onload="loadServices()"></div>
		
		 <script type="text/ng-template" id="tpl-autocomplete-dropdown">
			<form ng-submit="$event.preventDefault()">
		    	<md-autocomplete
			         ng-disabled="ctrl.isDisabled"
			         md-no-cache="ctrl.noCache"
			         md-selected-item="ctrl.selectedItem"
			         md-search-text-change="ctrl.searchTextChange(ctrl.searchText)"
			         md-search-text="ctrl.searchText"
			         md-selected-item-change="ctrl.selectedItemChange(item)"
			         md-items="item in ctrl.querySearch(ctrl.searchText)"
			         md-item-text="item.columnName"
			         md-min-length="0"
			         placeholder="Enter column name...">
		        <md-item-template>
		        	<span md-highlight-text="ctrl.searchText" md-highlight-flags="^i" title="{{item.columnName}}">{{item.columnName}}</span>
		        </md-item-template>
		        <md-not-found>
		        	No states matching "{{ctrl.searchText}}" were found.
		        </md-not-found>
		      	</md-autocomplete>
		    </form>
		</script>

		<!-- <button type="button" ng-show="isDebugMode()" class="btn btn-warning" ng-click="resetDebugMode()">Debug:ON</button> -->
		<div class="debug-container" ng-click="resetDebugMode()"><i class="fa fa-2x fa-bug debug-icon" ng-show="isDebugMode()" aria-hidden="true" title="Debug mode is ON. Click to OFF"></i></div>
			
		<script>
			var jsUserData =<%=userDetailJson%>;
			var filesPath2 ="<%=filesPath%>";
		</script>
		<br>	
		<ng-view></ng-view>		<!--  EVERYTHING IN ANGULAR WILL BE RENDERED IN THIS TAG -->
	</div>
</body>

<%@ include file="/WEB-INF/views/jsp/footer.jsp"%>

</html>
