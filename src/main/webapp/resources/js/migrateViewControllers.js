DBMigratorApp.controller( 'dbMigratePageController', function ($scope, popupFactory, devUtilities, dbUtilities, $element, $rootScope, $timeout){
	var userdata = jsUserData;
	$scope.jsUserData = jsUserData;
	$scope.collections = [];
	$scope.tables = [];
	$scope.migratePrefs = {allowNull: false};
	$scope.mainScope = $scope;
	$scope.attribEditData = {};
	$scope.source = {
		title:"Source",
		DataBases: userdata.sourceDbMap,
		selectedDB: userdata.NewData.sourceDbName,
		selectedSchema: userdata.NewData.sourceSchemaName
	};
	
	$scope.target = {
		title: "Target",
		DataBases: userdata.targetDbMap,
		selectedDB: userdata.NewData.targetDbName,
		selectedSchema: userdata.NewData.targetSchemaName
	};
	
	$scope.getTablesData = function(tableName){
		for (var i = 0; i < $scope.tables.length; i++) {
			if(tableName == $scope.tables[i].name){
				return $scope.tables[i].nodes;
			}
		}
		return [];
	}
	
	$scope.processJavaJSON = function (oJson){
		$scope.loadedCollection = {};
		var collection = oJson.mapObject;
		if(oJson._id){
			$scope.eventID = oJson._id.$oid;
		}
		$scope.eventName = oJson.mapName;
		$scope.eventDesc = oJson.comments ? oJson.comments : 'No Description Found';
		dbUtilities.clearAliasData();
		dbUtilities.populateJavaCollection($scope.loadedCollection, collection, true);
		$scope.processTableDataAndAlias($scope.loadedCollection.sourceTables, $scope.loadedCollection);
		$scope.collections.push($scope.loadedCollection);
		
		/*$scope.source.selectedDB = $scope.UserData.NewData.sourceDbName = oJson.sourceDbName;
		$scope.source.selectedSchema = $scope.UserData.NewData.sourceSchemaName = oJson.sourceUserName;
		$scope.target.selectedDB = $scope.UserData.NewData.targetDbName = oJson.targetDbName;
		$scope.target.selectedSchema = $scope.UserData.NewData.targetSchemaName = oJson.targetUserName;*/
		
		console.log($scope.source, $scope.target, $scope.source);
	}
	
	$scope.$on("add-collection-attribute-popup", function(e, attribData, collectionScope, isBlankAttribute){
		//console.log(attribData);
		$scope.originalEditAttributeCollectionScope = collectionScope;
		
		$scope.attribEditData = {
			attribError : '',
			attribEditMode: false,
			currentAttribName : attribData.label,
			currentAttribColName : attribData.label,
			currentAttribTableName : attribData.tableName ? attribData.tableName : dbUtilities.getTableFromAlias(attribData.tableName),
			currentAttribColumnType : attribData.type,
			attribTypes : attribData.supportedTypes ? attribData.supportedTypes : dbUtilities.getMongoAttrTypes(attribData.type),
			selectedAttribType : attribData.supportedTypes ? attribData.supportedTypes[0] : dbUtilities.getMongoAttrTypes(attribData.type)[0],
			isParentColumn : attribData.isParentColumn,
			preDefTableAlias : attribData.preDefAlias ? attribData.preDefAlias : null ,
			columnData : attribData.columnData
		}
		
		if(isBlankAttribute){
			$scope.attribEditData.currentAttribColName = "";
			$scope.attribEditData.currentAttribTableName = "";
			$scope.attribEditData.currentAttribColumnType = "";
		}
		
		/*$scope.attribEditData.attribError = '';
		$scope.attribEditData.attribEditMode = false;
		$scope.attribEditData.currentAttribName = attribData.label
		$scope.attribEditData.currentAttribColName = attribData.label;
		$scope.attribEditData.currentAttribTableName = attribData.tableName ? attribData.tableName : dbUtilities.getTableFromAlias(attribData.tableName);
		$scope.attribEditData.currentAttribColumnType = attribData.type;
		$scope.attribEditData.attribTypes = attribData.supportedTypes ? attribData.supportedTypes : dbUtilities.getMongoAttrTypes(attribData.type);
		$scope.attribEditData.selectedAttribType = $scope.attribEditData.attribTypes[0];*/
		$timeout(function () { // You might need this timeout to be sure its run after DOM render.
			$scope.$apply();
	    }, 0, false);
        $element.find('.DBM-edit-attribute-popup').modal();
	});
	
	$scope.$on("edit-collection-attribute-popup", function(e, attribData, collectionScope){
		//console.log(attribData);
		$scope.originalEditAttribute = attribData;
		$scope.originalEditAttributeCollectionScope = collectionScope;
		
		$scope.attribEditData = {
			attribError : '',
			attribEditMode: true,
			currentAttribName : attribData.attributeName,
			ignoreList : attribData.columnData ? (attribData.columnData.ignoreList ? attribData.columnData.ignoreList : []) :[],
			currentAttribColName : attribData.columnData ? attribData.columnData.columnName : '',
			currentAttribTableName : attribData.columnData ? (attribData.columnData.tableName ? attribData.columnData.tableName : dbUtilities.getTableFromAlias(attribData.columnData.tableAlias)) : '',
			currentAttribColumnType : attribData.columnData ? attribData.columnData.columnType : '',
			attribTypes : attribData.supportedTypes ? attribData.supportedTypes : (attribData.columnData ? dbUtilities.getMongoAttrTypes(attribData.columnData.columnType) : ''),
			selectedAttribType : attribData.attributeType,
			defaultValue: attribData.defaultValue,
			columnData : attribData.columnData
		}
		/*$scope.attribEditData.attribError = '';
		$scope.attribEditData.attribEditMode = true;
		$scope.attribEditData.currentAttribName = attribData.attributeName
		$scope.attribEditData.currentAttribColName = attribData.columnData.columnName;
		$scope.attribEditData.currentAttribTableName = attribData.columnData.tableName ? attribData.columnData.tableName : dbUtilities.getTableFromAlias(attribData.columnData.tableAlias);
		$scope.attribEditData.currentAttribColumnType = attribData.columnData.columnType;
		$scope.attribEditData.attribTypes = attribData.supportedTypes ? attribData.supportedTypes : dbUtilities.getMongoAttrTypes(attribData.columnData.columnType);
		$scope.attribEditData.selectedAttribType = attribData.attributeType;*/
        $element.find('.DBM-edit-attribute-popup').modal();
	});
	
	$scope.CancelAddAttribute = function(){
		$scope.attribEditData = {};
		$scope.originalEditAttributeCollectionScope.clearTableRelationsData();
		$element.find('.DBM-edit-attribute-popup').modal('hide');
	};
	
	$scope.attribTypeSelected = function(strType){
		$scope.attribEditData.selectedAttribType = strType;
	};
	
	$scope.addAttribute = function(){
		$scope.attribEditData.attribError = '';
		if($scope.attribEditData.attribEditMode){ // if its editing existing attribute
			var preDefTableAlias = $scope.attribEditData.preDefTableAlias ? $scope.attribEditData.preDefTableAlias : null;
			if(!($scope.attribEditData.currentAttribName == $scope.originalEditAttribute.attributeName)){
				var validate = $scope.originalEditAttributeCollectionScope.isValidAttributeName($scope.attribEditData.currentAttribName);
				if(validate.isValid){
					$scope.originalEditAttribute.attributeName = $scope.attribEditData.currentAttribName;
					$scope.originalEditAttribute.attributeType = $scope.attribEditData.selectedAttribType;
					$scope.originalEditAttribute.columnData.ignoreList = $scope.attribEditData.ignoreList;
					$scope.originalEditAttribute.defaultValue = $scope.attribEditData.defaultValue;
					$scope.originalEditAttribute.columnData = $scope.attribEditData.columnData;
					$element.find('.DBM-edit-attribute-popup').modal('hide');
				} else {
					$scope.attribEditData.attribError = validate.errMsg;
				}
			} else {
				$scope.originalEditAttribute.attributeName = $scope.attribEditData.currentAttribName;
				$scope.originalEditAttribute.attributeType = $scope.attribEditData.selectedAttribType;
				$scope.originalEditAttribute.columnData.ignoreList = $scope.attribEditData.ignoreList;
				$scope.originalEditAttribute.defaultValue = $scope.attribEditData.defaultValue;
				$scope.originalEditAttribute.columnData = $scope.attribEditData.columnData;
				if($scope.attribEditData.currentAttribName == 'FILE_NAME' || $scope.attribEditData.currentAttribName == 'INPUT_STREAM'){
					$scope.originalEditAttributeCollectionScope.editGridAttribute({
						attributeName: $scope.attribEditData.currentAttribName,
						attributeType: $scope.attribEditData.selectedAttribType,
						defaultValue: $scope.attribEditData.defaultValue,
						columnData : $scope.attribEditData.columnData
					},$scope.attribEditData.currentAttribTableName, preDefTableAlias);
				}
				$element.find('.DBM-edit-attribute-popup').modal('hide');
			}
			
		} else { // its adding new attribute
			var validate = $scope.originalEditAttributeCollectionScope.isValidAttributeName($scope.attribEditData.currentAttribName);
			if(validate.isValid){
				var preDefTableAlias = $scope.attribEditData.preDefTableAlias ? $scope.attribEditData.preDefTableAlias : null;
				var columnData = {};
				if($scope.attribEditData.currentAttribTableName && $scope.attribEditData.currentAttribTableName.trim() !== ""){
					columnData = {
							"columnName": $scope.attribEditData.currentAttribColName,
							"tableAlias" : $scope.attribEditData.preDefTableAlias ? $scope.attribEditData.preDefTableAlias : dbUtilities.getTableAlias($scope.attribEditData.currentAttribTableName),
							"columnType": $scope.attribEditData.currentAttribColumnType,
						}
				}
				columnData.ignoreList = $scope.attribEditData.ignoreList;
				columnData.isParentColumn = $scope.attribEditData.isParentColumn;
				
				$scope.originalEditAttributeCollectionScope.addNewAttribute({
					attributeName: $scope.attribEditData.currentAttribName,
					attributeType: $scope.attribEditData.selectedAttribType,
					defaultValue: $scope.attribEditData.defaultValue,
					columnData : columnData
				},$scope.attribEditData.currentAttribTableName, preDefTableAlias);
				$element.find('.DBM-edit-attribute-popup').modal('hide');
			} else {
				$scope.attribEditData.attribError = validate.errMsg;
			}
		}
	};
	
	/*$scope.populateJavaCollection = function(mainCollection, oCollection, isPrimary){
		mainCollection.attributeName = oCollection.attributeName;
		if(isPrimary){
			
		} else {
			mainCollection.attributeType = oCollection.attributeType ? oCollection.attributeType : "COLLECTION";
		}
		mainCollection.attrs = [];
		mainCollection.currentAttribTables = oCollection.currentAttribTables;
		for (var i = 0; i < oCollection.attributes.length; i++) {
			
			if(dbUtilities.isCollectionType(oCollection.attributes[i].attributeType)){
				var subCollection = {};
				mainCollection.attrs.push(subCollection);
				$scope.populateJavaCollection(subCollection, oCollection.attributes[i], false)
				// Handle new collection
			} else {
				mainCollection.attrs.push(oCollection.attributes[i]);
			}	
		}
		if(oCollection.identifiers && oCollection.identifiers.length > 0){
			for (var j = 0; j < oCollection.identifiers.length; j++) {
				var array_element = $.extend({},oCollection.identifiers[j]);
				array_element.isIdentifier = true;
				mainCollection.attrs.push(array_element);
			}
		}
		if(mainCollection.attrs.length > 0){
			mainCollection.attrs.sort($scope.sortAttribute);
		}
		mainCollection.sourceTables = oCollection.sourceTables;
		mainCollection.filters = oCollection.filters ? oCollection.filters : [];
	}
	
	$scope.sortAttribute = function(a,b) {
		  if (a.attributeName < b.attributeName)
		    return -1;
		  else if (a.attributeName > b.attributeName)
		    return 1;
		  else 
		    return 0;
	}*/

	
	$scope.fetchCurrentJSONIfExist = function(){
		if(dbUtilities.isCurrentJSON()){
			var loadJSON = dbUtilities.fetchCurrentJSON();
			console.log(loadJSON);
			$scope.processJavaJSON(loadJSON);
			popupFactory.closeProgressPopup(); // Popup that's shown by home page
		}
	}
	
	/* Process table alias from JSON and add it to current Alias Data */
	$scope.processTableDataAndAlias = function(collectionSourceTables, collection){
		collection.currentAttribTables = [];
		for (var i = 0; i < collectionSourceTables.length; i++) {
			//console.log('Setting tablename : '+collectionSourceTables[i].tableName+' >>> To Alias : '+collectionSourceTables[i].tableAlias);
			dbUtilities.setTableAlias(collectionSourceTables[i].tableName, collectionSourceTables[i].tableAlias);
			collection.currentAttribTables.push(collectionSourceTables[i].tableName);
			if(collectionSourceTables[i].joinedTables && collectionSourceTables[i].joinedTables.length > 0){
				$scope.processTableDataAndAlias(collectionSourceTables[i].joinedTables, collectionSourceTables[i]);
			}
		}
		if(collection.attrs){ //  Will only work for collection chunk....
			for (var j = 0; j < collection.attrs.length; j++) {
				if(dbUtilities.isCollectionType(collection.attrs[j].attributeType) && collection.attrs[j].sourceTables){
					$scope.processTableDataAndAlias(collection.attrs[j].sourceTables, collection.attrs[j]);
				}
			}
		}
	}
	
	$scope.getCurrentFetchedData = function(){
		var fetchedTablesData = {tables: [], columns: []};
		for (var i = 0; i < $scope.tables.length; i++) {
			if($scope.tables[i].nodes.length > 0){
				fetchedTablesData.tables.push($scope.tables[i].name);
				fetchedTablesData.columns.push({
					table: $scope.tables[i].name,
					columns: $scope.tables[i].nodes
				});
			}
		}
		return fetchedTablesData;
	}
	
	/* Get table relations using table name and then remove table name from json and return */
	$scope.getTableRelations = function(tableName, dataCollection){
		var tableRelations = $.grep(dataCollection.currentTableRelations, function(e){ return e.mainTableName == tableName; });
		var tableRelationsCopy = [];
		for (var i = 0; i < tableRelations.length; i++) {
			tableRelationsCopy.push($.extend({}, tableRelations[i]));
			delete tableRelationsCopy[i].mainTableName;
		}
		return tableRelationsCopy;
	}
	
	$scope.fillCollectionData = function(dataCollection, targetCollection, isPrimaryCollection){
		targetCollection.attributeName = dataCollection.attributeName;
		if(!isPrimaryCollection){
			targetCollection.attributeType = dataCollection.attributeType;
		}
		if(dataCollection.filters){
			targetCollection.filters = dataCollection.filters.slice();
		}
		var currentCollectionAttribs = dataCollection.attrs;
		targetCollection.attributes= [];
		if(isPrimaryCollection){
			targetCollection.identifiers= [];
		}
		if(dataCollection.referencedColumns){
			targetCollection.referencedColumns = dataCollection.referencedColumns;
		}
		targetCollection.sourceTables= dataCollection.sourceTables;
		for (var attribIndex = 0; attribIndex < currentCollectionAttribs.length; attribIndex++) {
			if(dbUtilities.isCollectionType(currentCollectionAttribs[attribIndex].attributeType)){
				var collectionObject = {};
				targetCollection.attributes.push(collectionObject);
				$scope.fillCollectionData(currentCollectionAttribs[attribIndex], collectionObject);
			}else {
				var attr = {
						columnData : currentCollectionAttribs[attribIndex].columnData,
						/*attributeName : currentCollectionAttribs[attribIndex].attributeName,
						attributeType : currentCollectionAttribs[attribIndex].attributeType,
	                    */columnType : currentCollectionAttribs[attribIndex].columnType,
	                    ignoreList: currentCollectionAttribs[attribIndex].ignoreList,
	                    defaultValue: currentCollectionAttribs[attribIndex].defaultValue
				}
				if(dataCollection.isGridSpecific !=undefined && dataCollection.isGridSpecific){
					attr.attribute = {
							attributeName : currentCollectionAttribs[attribIndex].attributeName,
							attributeType : currentCollectionAttribs[attribIndex].attributeType,
							isIdentifier : currentCollectionAttribs[attribIndex].isIdentifier
						};
				}else{
					attr.attributeName = currentCollectionAttribs[attribIndex].attributeName;
                    attr.attributeType = currentCollectionAttribs[attribIndex].attributeType;
				}
				if(currentCollectionAttribs[attribIndex].isIdentifier && isPrimaryCollection){
					targetCollection.identifiers.push(attr);
				} else {
					targetCollection.attributes.push(attr);
				}
			}
		}
	};
	
	$scope.fillExportCollectionData = function(dataCollection, targetCollection, isPrimaryCollection){
		targetCollection.attributeName = dataCollection.attributeName;
		if(!isPrimaryCollection){
			targetCollection.attributeType = dataCollection.attributeType;
		}
		if(dataCollection.filters){
			targetCollection.filters = dataCollection.filters.slice();
		}
		var currentCollectionAttribs = dataCollection.attrs;
		targetCollection.attrs= [];
		targetCollection.currentAttribTables = dataCollection.currentAttribTables;
		targetCollection.sourceTables= dataCollection.sourceTables;
		if(dataCollection.referencedColumns){
			targetCollection.referencedColumns = dataCollection.referencedColumns;
		}
		for (var attribIndex = 0; attribIndex < currentCollectionAttribs.length; attribIndex++) {
			if(dbUtilities.isCollectionType(currentCollectionAttribs[attribIndex].attributeType)){
				var collectionObject = {};
				targetCollection.attrs.push(collectionObject);
				$scope.fillExportCollectionData(currentCollectionAttribs[attribIndex], collectionObject);
			}else {
				var attr = {
					columnData : currentCollectionAttribs[attribIndex].columnData,
                    /*attributeName : currentCollectionAttribs[attribIndex].attributeName,
                    attributeType : currentCollectionAttribs[attribIndex].attributeType,
                    */columnType : currentCollectionAttribs[attribIndex].columnType,
                    ignoreList: currentCollectionAttribs[attribIndex].ignoreList,
                    defaultValue: currentCollectionAttribs[attribIndex].defaultValue
				};
				if(dataCollection.isGridSpecific !=undefined && dataCollection.isGridSpecific){
					attr.attribute = {
							attributeName : currentCollectionAttribs[attribIndex].attributeName,
							attributeType : currentCollectionAttribs[attribIndex].attributeType,
							isIdentifier : currentCollectionAttribs[attribIndex].isIdentifier
						};
				}else{
					attr.attributeName = currentCollectionAttribs[attribIndex].attributeName;
                    attr.attributeType = currentCollectionAttribs[attribIndex].attributeType;
                    if(currentCollectionAttribs[attribIndex].isIdentifier){
    					attr.isIdentifier = true;
    				}
				}
				/*if(currentCollectionAttribs[attribIndex].isIdentifier){
					attr.isIdentifier = true;
				}*/
				targetCollection.attrs.push(attr);
			}
			
		}
	};
	
	$scope.buildExportJSON = function(){
		var strPostData = {nullAllowed: $scope.migratePrefs.allowNull};
		for (var i = 0; i < $scope.collections.length; i++) {
			$scope.fillExportCollectionData($scope.collections[i], strPostData, true);
		}
		return strPostData;
	}
	
	$scope.buildMigrateJSON = function(){
		var strPostData = {nullAllowed: $scope.migratePrefs.allowNull, collections: [{}]};
		for (var i = 0; i < $scope.collections.length; i++) {
			$scope.fillCollectionData($scope.collections[i], strPostData.collections[0], true);
		}
		if($scope.eventID){
			strPostData._id = $scope.eventID;
		}
		return strPostData;
	}
	
	$scope.showMigrateJSON = function (){
		var strPostData = $scope.buildMigrateJSON();
		strPostData.eventName = 'EVENT_NAME';
		strPostData.eventDescription = 'EVENT_DESCRIPTION';
		popupFactory.showInfoPopup('Java Migrate JSON : ', "<pre>"+JSON.stringify(strPostData, undefined, 2)+"</pre>");
	},
	
	$scope.showExportJSON = function (){
		var jsonObject = [];
		jsonObject.push($scope.buildExportJSON());
		popupFactory.showInfoPopup('Export JSON : ', "<pre>"+JSON.stringify(jsonObject, undefined, 2)+"</pre>");
	},
	
	$scope.OnMigrate = function(){
		var collectionData = $scope.collections[0];
		if(collectionData.collectionType == 'GridFs'){
			for(var i=0; i<collectionData.attrs.length; i++){
				if(collectionData.attrs[i].attributeName == 'FILE_NAME' || collectionData.attrs[i].attributeName == 'INPUT_STREAM'){
					var colData = collectionData.attrs[i].columnData;
					if(angular.equals(colData,{}) || colData == '' || colData == undefined){
						popupFactory.showInfoPopup('Invalid Map : ', "Attributes File_Name and Input_stream must be mapped to column of table");
						return;
					}
				}
			}
		}
		popupFactory.getTitleDescriptionPopup({
			header: 'Enter name for the migration job.',
			title: $scope.eventName,
			description: $scope.eventDesc
		},function(oData){
			var migrateCollections = $scope.buildMigrateJSON();
			var strPostData = {};
			if($scope.collections[0].collectionType == undefined || !$scope.collections[0].collectionType == 'GridFs'){
				strPostData = {
						'createdBy': $scope.UserData.NewData.userId,
						'mapType': 'OrclToMongo',
						'sourceDbName': $scope.UserData.NewData.sourceDbName,
						'sourceUserName': $scope.UserData.NewData.sourceSchemaName,
						'targetDbName': $scope.UserData.NewData.targetDbName,
						'targetUserName': $scope.UserData.NewData.targetSchemaName,
						'nullAllowed': migrateCollections.nullAllowed,
						'mapObject': migrateCollections.collections[0]
					}
			}else{
				 strPostData = {
						'createdBy': $scope.UserData.NewData.userId,
						'mapType': 'OrclToMongoGridFs',
						'sourceDbName': $scope.UserData.NewData.sourceDbName,
						'sourceUserName': $scope.UserData.NewData.sourceSchemaName,
						'targetDbName': $scope.UserData.NewData.targetDbName,
						'targetUserName': $scope.UserData.NewData.targetSchemaName,
						'nullAllowed': migrateCollections.nullAllowed,
						'attributeName': migrateCollections.collections[0].attributeName,
						'fileNameColumn': migrateCollections.collections[0].attributes[0].columnData,
						'inputStreamColumn': migrateCollections.collections[0].attributes[1].columnData,
						'metaAttributes': migrateCollections.collections[0].attributes[2],
						'filters': migrateCollections.collections[0].filters,
						'streamTable': migrateCollections.collections[0].sourceTables[0]
					}
			}
			
			
			if(migrateCollections._id){
				strPostData._id = migrateCollections._id;
			}
			
			strPostData.mapName  = oData.Name ? oData.Name : 'Unknown';
			strPostData.comments = oData.Desc ? oData.Desc : 'Unknown';
			
			console.log('final json : ');
			console.log(JSON.stringify(strPostData));
			popupFactory.showProgressPopup('verifying migration data....');
			$.ajax({   
				type :"POST",
			    url :"saveMappings",
			    data : "mapJson="+JSON.stringify(strPostData),
			    async: false,
				success: function(data){
					popupFactory.closeProgressPopup();
					popupFactory.showInfoPopup('Success!!', 'Migration Mapping Saved Successfully.', false, function(){
						window.location.hash = '#/Home';
					});
				},
				fail: function(){
					//alert('Migration Not Started !!');
				}
			});
			
		});
	};
	
	$scope.fetchCurrentJSONIfExist();
	$scope.removeGridAttribData = function(){
		$scope.attribEditData.columnData = {};
		$scope.attribEditData.currentAttribTableName = null;
		if($scope.attribEditData.currentAttribName == 'INPUT_STREAM'){
			$scope.collections[0].currentAttribTables = [];
			$scope.collections[0].sourceTables = [];
		}
	}
});



DBMigratorApp.controller('dbTargetSection', function ($scope, $rootScope, $interval, popupFactory, devUtilities, $element, dbUtilities){
	
	// Populating dummy data for developer use
	if(devUtilities.isDebugMode()){
		if($scope.collections.length == 0){
			$scope.collections.push({	
				attributeName: "DumyModeCollection",
				attributeType: 'AUTO',
				attrs : [],
				sourceTables: []
			});
		}
	}
	//-------------
	
	$scope.addNewCollection = function(){
		if($scope.collections.length > 0){
			popupFactory.showInfoPopup('Coming soon!!', 'Multiple collection support feature is coming soon.');
		}else {
			popupFactory.getInputPopup('Enter collection name : ', $scope.addCollection);
		}
	}
	
	$scope.downloadJSON = function(){
		var jsonObject = [];
		jsonObject.push($scope.buildExportJSON());
		$element.find('#exportTargetJSONBtn').attr({
            "download": "mirateData.json",
            "href": "data:application/json;charset=utf8;base64," 
                    + window.btoa(JSON.stringify(jsonObject))
		});
	}
	
	$scope.importJSON = function(fileElement){		
		var uploadedFile = fileElement.files[0]; 

	    if (uploadedFile) {
	        var readFile = new FileReader();
	        readFile.onload = function(e) { 
	            var contents = e.target.result;
	            
	            try{
	            	 var json = JSON.parse(contents);
	            }catch(e){
	                //alert(e); //error in the above string(in this case,yes)!
	            	popupFactory.showInfoPopup("Error !!", "The file doesn't seem a valid JSON file ! Please select valid JSON file.");
	            }
	            //var json = JSON.parse(contents);
	            console.log('JSON Loaded and Parsed : ');
	            console.log(json);
	            $scope.$parent.collections = json;
	            dbUtilities.clearAliasData();
	            $scope.processTableAliases($scope.$parent.collections[0].sourceTables);
	            $scope.$apply();
	        };
	        readFile.readAsText(uploadedFile);
	    } else { 
	        console.log("Failed to load file");
	    }
	}
	
	/* Process table alias from JSON and add it to current Alias Data */
	$scope.processTableAliases = function(collections){
		for (var i = 0; i < collections.length; i++) {
			dbUtilities.setTableAlias(collections[i].tableName, collections[i].tableAlias);
			if(collections[i].joinedTables && collections[i].joinedTables.length > 0){
				$scope.processTableAliases(collections[i].joinedTables);
			}
		}
	}
	
	$scope.addCollection = function(collectionName){
		$scope.collections.push({	
			attributeName: collectionName,
			attributeType: 'AUTO',
			attrs : [],
			sourceTables: []
		});
		$scope.$apply();
	}
	
	$scope.getNodeTemplate = function(attr){
		if(dbUtilities.isCollectionType(attr.attributeType)){
			return 'collection_instance_renderer.html';
		} else {
			return 'collection_end_node.html';
		}
	}
	
	$scope.getNodeController = function(attr){
		if(dbUtilities.isCollectionType(attr.attributeType)){
			return 'dbCollectionInstanceController';
		} else {
			return 'dbColumnAttributeNode';
		}
	}
	
	$scope.addNewCollectionForGrid = function(){
		if($scope.collections.length > 0){
			popupFactory.showInfoPopup('Coming soon!!', 'Multiple collection support feature is coming soon.');
		}else {
			popupFactory.getInputPopup('Enter collection name : ', $scope.addGridFsCollection);
		}
	}
	
	$scope.addGridFsCollection = function(collectionName){
		$scope.isGridSubcollectionAdded = false;
		$scope.collections.push({
			collectionType : 'GridFs',
			attributeName: collectionName,
			attributeType: 'AUTO',
			attrs : [{"attributeName": "FILE_NAME","attributeType": "STRING","defaultValue": null,"columnData":{}},
			         {"attributeName": "INPUT_STREAM","attributeType": "B_ARRAY","defaultValue": null,"columnData":{}}/*,
			         {"attributeName": "META_ATTRIBUTES","attributeType": "AUTO","defaultValue": null,attrs:[],sourceTables: []}*/],
			sourceTables: []
		});
		$scope.$apply();
	}
	
	$scope.isColumnMapped = function(element){
		if(element.columnData != undefined && element.columnData.columnName!=undefined){
			return true;
		}
		return false;
	}
	
	$scope.isGridAttribute = function(element){
		if(element.attributeName =='FILE_NAME' || element.attributeName =='INPUT_STREAM'){
			return true;
		}
		return false;
	}
});

