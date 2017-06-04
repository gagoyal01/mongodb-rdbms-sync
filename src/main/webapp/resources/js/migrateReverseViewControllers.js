DBMigratorApp.controller( 'dbMigratePageReverseController', function ($scope, popupFactory, devUtilities, dbUtilities, $element, $rootScope, $timeout, $http){
	var userdata = jsUserData;
	$scope.jsUserData = jsUserData;
	$scope.collections = [];
	$scope.tables = [];
	$scope.migratePrefs = {allowNull: false};
	$scope.mainScope = $scope;
	$scope.currentEditingColumn = {};
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
	
	$scope.collections = [];
	$scope.tables = [];
	
	$scope.fetchCurrentJSONIfExist = function(){
		if(dbUtilities.isCurrentJSON()){
			var loadJSON = dbUtilities.fetchCurrentJSON();
			console.log(loadJSON);
			$scope.processReverseJavaJSON(loadJSON);
			
		}
	}
	
	$scope.processTableString = function(nodeGroup, fetchTables, parentNodeName, parentNodePath){
		var tempString = '';
		for (var i = 0; i < nodeGroup.length; i++) {
			var nodeGroupItem = nodeGroup[i];
			if(parentNodeName){
				nodeGroupItem.parentNodeName = parentNodeName;
			}
			if(parentNodePath){
				nodeGroupItem.nodePath = parentNodePath + '.' + nodeGroupItem.nodeName;
			} else {
				nodeGroupItem.nodePath = nodeGroupItem.nodeName;
				nodeGroupItem.rootNodeItem = true;
			}
			if(nodeGroupItem.tableList != null && nodeGroupItem.tableList != undefined){
				for (var j = 0; j < nodeGroupItem.tableList.length; j++) {
					var matchFromFetchTable = $.grep(fetchTables, function(e){ return e.tableName == nodeGroupItem.tableList[j].tableName; });
					if(matchFromFetchTable.length > 1){
						// For a specific table there are two entries>>>> SOME PROBLEM !!!!
						console.warn('Matched table length is more than 1. Needs a check !!')
					}
					if(matchFromFetchTable.length == 0){
						fetchTables.push({
							tableName : nodeGroupItem.tableList[j].tableName,
							tableAlias : nodeGroupItem.tableList[j].tableAlias,
							nodeGroups : [nodeGroupItem],
							keyColumns : nodeGroupItem.tableList[j].keyColumns
						});
					} else {
						matchFromFetchTable[0].nodeGroups.push(nodeGroupItem);
					}
				}
			}
			if(nodeGroupItem.childNodes && nodeGroupItem.childNodes.length){
				$scope.processTableString(nodeGroupItem.childNodes, fetchTables, nodeGroupItem.nodeName, nodeGroupItem.nodePath);
			}
		}
		fetchTables.forEach(function(item, index){
			if(tempString){
				tempString += ',';
			}
			tempString += item.tableName;
		});
		return tempString;
	}
	
	$scope.processReverseJavaJSON = function (oJson, endCallback){
		var fetchTablesString = '';
		var fetchTables = [];
		$scope.currentCollectionName = oJson.collectionName;
		if(oJson._id){
			$scope.eventID = oJson._id.$oid;
		} else {
			$scope.eventID = null;
		}
		$scope.nodeTableGroup = oJson.nodeTableGroup ? oJson.nodeTableGroup : oJson.nodeGroup;
		$scope.eventName = oJson.mapName;
		$scope.eventDesc = oJson.comments ? oJson.comments : 'No Description Found';
		
		fetchTablesString = $scope.processTableString($scope.nodeTableGroup, fetchTables);
		//console.log('Finally >>> '+fetchTablesString);
		$http({
			method: 'POST',
			url :"fetchColumnsForMultipleTables",
			params : {tableName : fetchTablesString, sourceDatabaseName : $scope.UserData.NewData.targetDbName, sourceDatabaseSchema: $scope.UserData.NewData.targetSchemaName, userName: $scope.UserData.NewData.userId}
		}).then(function successCallback(response) {
			
			for (var i = 0; i < response.data.length; i++) {
				var tableFetched = response.data[i];
				
				var matchTableNodesData = $.grep(fetchTables, function(e){ return e.tableName == tableFetched.tableName; });
				if(matchTableNodesData.length == 1){
					 matchTableNode = matchTableNodesData[0];
					 for (var j = 0; j < matchTableNode.nodeGroups.length; j++) {
						var node = matchTableNode.nodeGroups[j];
						if(!node.tables){
							node.tables = [];
						}
						var currentColumns = [];
						tableFetched.columns.forEach(function(column, index){
							if(node.rootNodeItem){
								currentColumns.push($.extend({},column));
								currentColumns[(currentColumns.length - 1)].rootNodeColumn = true;
							} else {
								currentColumns.push($.extend({},column));
							}
							
						});
						//var currentColumns = tableFetched.columns.slice();
						node.tables.push({
							name: matchTableNode.tableName,
							firstAttribute: node.nodePath,
							showtree: false,
							nodes:  currentColumns,
							//keyColumns: matchTableNode.keyColumns // Will be generated at the time of JSON generation. No need to specify here.
							//tableAlias: matchTableNode.tableAlias
							//tableIndex: (tableStoredIndex == undefined) ? tableIndex : tableStoredIndex
						});
						// COPY ATTRIBUTES DATA !!!!
						if(node.columnAttrMap && node.columnAttrMap.length){
							currentColumns.forEach(function(column, index){
								for (var k = 0; k < node.columnAttrMap.length; k++) {
									var attr = node.columnAttrMap[k];
									column.tableFirstColumn = node.nodePath;
									
									
									if(attr.columnData.columnName == column.columnName){
										if(attr.isParentAttribute){
											column.isParentAttribute = true;
										}
										if(attr.parentNode){
											column.parentNode = attr.parentNode;
										}
										
										column.attrData = $.extend({}, attr.attribute);
										column.attrData.collectionName = column.parentNode ? column.parentNode : node.nodeName;
										column.isSeqGenerated= attr.isSeqGenerated;
										column.seqName = attr.seqName;
										column.defaultValue = attr.literalValueForColumn ? attr.literalValueForColumn.literalValue : attr.defaultValue;
										column.columnAlias = attr.columnData.columnAlias;
										column.replacementMap = attr.replacementMap;
									}
									if(matchTableNode.keyColumns && matchTableNode.keyColumns.length){
										matchTableNode.keyColumns.forEach(function(keyColumn, index){
											if(keyColumn.columnName == column.columnName){
												column.isKeyColumn = true;
												column.allowNullKeyColumn = keyColumn.isNullable;
											}
										})
									}
								}
							})
						} else {
							currentColumns.forEach(function(column, index){
								column.tableFirstColumn = node.nodePath;
							});
						}
						
						 
						/*tableFetched.columns.forEach(function(item, index){
							
						});*/
					 }
				} else {
					if(matchTableNodesData.length == 0){
						console.warn('Fectehd Table '+tableFetched.tableName+' not found in nodes data : This can be fatal !!!');
					} else if(matchTableNodesData.length > 1){
						console.warn('Fectehd Table '+tableFetched.tableName+' has more than one ENTRY : This can be fatal !!!');
					}
					
				}
			}
			
			
			/*for (var i = 0; i < $scope.nodeTableGroup.length; i++) {
				var nodeGroup = $scope.nodeTableGroup[i];
				nodeGroup.indentPath = nodeGroup.nodeName.split('.');
				nodeGroup.parentNodeName = $scope.getNodeParentName(nodeGroup.nodeName);
				nodeGroup.tables = [];
				if(nodeGroup.tableList !=null && nodeGroup.tableList != undefined){
					for (var j = 0; j < nodeGroup.tableList.length; j++) {
						var curTableName = nodeGroup.tableList[j].tableName;
						var curTableAlias = nodeGroup.tableList[j].tableAlias;
						var tableRelations = $.grep(response.data, function(e){ return e.tableName == curTableName; });
						var matchedtable = tableRelations[0];
						
						var tableNode = {
							name: matchedtable.tableName,
							firstAttribute: {
									collectionName: nodeGroup.nodeName
								},
							showtree: false,
							nodes:  []
							//tableIndex: (tableStoredIndex == undefined) ? tableIndex : tableStoredIndex
						};
						//console.log('firstAttribute >>> for : '+matchedtable.tableName+' is >>>> '+tableNode.firstAttribute.collectionName);
						
						// Need to make copies of node objects so that original fetched data will not be altered
						matchedtable.columns.forEach(function(item, index){
							var copyItem = $.extend({},item);
							tableNode.nodes.push(copyItem);
							if(nodeGroup.columnAttrMap.length){
								nodeGroup.columnAttrMap.forEach(function(node, nIndex){
									if(node.columnData.columnName == copyItem.columnName && curTableName == matchedtable.tableName){
										copyItem.attrData = $.extend({},node.attribute);
									}
								})
							}
						});
						
						nodeGroup.tables.push(tableNode);
						
					}
				}
			}*/
			
			//$scope.nodeTableGroup.sort($scope.sortIndent);
			
			if(endCallback){
				//$scope.mappingLoaded();
				endCallback();
			}
			
			popupFactory.closeProgressPopup(); // Popup that's shown by home page in case of Editing Mapping 
		});
	},
	
	$scope.getNodeParentName = function(nodeName){
		console.log('Generating node Parent name for : '+nodeName);
		var nodePath = nodeName.split(".");
		if(nodePath.length > 1){
			var parentName = '';
			for (var i = 0; i < (nodePath.length - 1); i++) {
				if(parentName){
					parentName += '.';
				}
				parentName += nodePath[i];
			}
			return parentName;
		} else {
			return 'root';
		}
	},
	
	$scope.sortIndent = function(a,b) {
		  if (a.indentPath.length < b.indentPath.length)
			  return -1;
		  else if (a.indentPath.length > b.indentPath.length)
			  return 1;
		  else if (a.indentPath.length == b.indentPath.length){
			  if(a.indentPath[a.indentPath.length - 1] > b.indentPath[b.indentPath.length - 1])
				  return 1;
			  else 
				  return -1;
		  }
		  else 
		    return 0;
	},
	
	/* TODO !! BELOW FUNCTION IS KEPT FOR REFERENCE REMOVE LATER !!! */
	/*$scope.processReverseJavaJSON_OLD = function (oJson, endCallback){
		var fetchTables = [];
		var tableSequence = [];
		var fetchTablesString = '';
		var nFetchCounter = 0;
		var tableAliasData = {};
		var tableCollectionRelationData = {};
		var nodeTableMap = {};
		$scope.currentCollectionName = oJson.collectionName;
		if(oJson._id){
			$scope.eventID = oJson._id.$oid;
		} else {
			$scope.eventID = null;
		}
		$scope.eventName = oJson.mapName;
		$scope.eventDesc = oJson.comments ? oJson.comments : 'No Description Found';
		for (var i = 0; i < oJson.nodeTableGroup.length; i++) {
			var nodeGroup = oJson.nodeTableGroup[i];
			var tableKeyColumnData = {};
			var tableColumnsLoadData = {};
			if(nodeGroup.tableList !=null && nodeGroup.tableList != undefined){
				for (var j = 0; j < nodeGroup.tableList.length; j++) {
					var tableEl = nodeGroup.tableList[j];
					var tableName = nodeGroup.tableList[j].tableName;
					var tableAlias = nodeGroup.tableList[j].tableAlias;
					var tNodes = [];
					tableSequence.push(nodeGroup.tableList[j].tableName);
					nFetchCounter++;
					if(fetchTables.indexOf(nodeGroup.tableList[j].tableName) == -1){
						fetchTables.push(nodeGroup.tableList[j].tableName);
						if(fetchTablesString){
							fetchTablesString+= ',';
						}
						fetchTablesString += nodeGroup.tableList[j].tableName
						
					}
					
					tableKeyColumnData = tableEl.keyColumns;
					tableCollectionRelationData[tableAlias] = nodeGroup.nodeName;
					if(nodeGroup.columnAttrMap != undefined){
						nodeGroup.columnAttrMap.forEach(function(attrMap){
							if(attrMap.columnData.tableAlias == tableAlias){
								tableColumnsLoadData[attrMap.columnData.columnName] = attrMap;
								if(attrMap.attribute){
									attrMap.attribute.collectionName = nodeGroup.nodeName;
								}
								//console.log('Inserting into columnData into >> '+attrMap.columnData.columnName+' For ALIAS : '+tableAlias);
							}
						});
					}
					
					if(nodeTableMap[tableName]){
						nodeTableMap[tableName].push({
							alias: tableAlias,
							tableIndex: nodeGroup.tableList[j].hasOwnProperty("tableIndex") ? odeGroup.tableList[j].tableIndex : undefined,
							tableColumnsLoadData: tableColumnsLoadData,
							tableKeyColumnData: tableKeyColumnData,
							nodeName : nodeGroup.nodeName
						})
					} else {
						nodeTableMap[tableName] = [{
							alias: tableAlias,
							tableIndex: nodeGroup.tableList[j].hasOwnProperty("tableIndex") ? odeGroup.tableList[j].tableIndex : undefined,
							tableColumnsLoadData: tableColumnsLoadData,
							tableKeyColumnData: tableKeyColumnData,
							nodeName : nodeGroup.nodeName
						}];
					}
				}
			}
		}
		// Tables analysis complete..... Load all tables in One API call and process the columns.
		$http({
			method: 'POST',
			url :"fetchColumnsForMultipleTables",
			params : {tableName : fetchTablesString, sourceDatabaseName : $scope.UserData.NewData.targetDbName, sourceDatabaseSchema: $scope.UserData.NewData.targetSchemaName, userName: $scope.UserData.NewData.userId}
		}).then(function successCallback(response) {
			for (var tableIndex = 0; tableIndex < tableSequence.length; tableIndex++) {
				var tableName = tableSequence[tableIndex];
				var tableRelations = $.grep(response.data, function(e){ return e.tableName == tableName; });
				var matchedtable = tableRelations[0];
				var tableDataArray = nodeTableMap[tableName];
				if(tableDataArray.length < 1){
					console.error('NO DATA FOUND FOR '+tableName+' in tableDataArray')
				} else {
					var tableData = tableDataArray.shift();
					var colAttrData = tableData.tableColumnsLoadData;
					var keyColumns = tableData.tableKeyColumnData;
					var tableStoredIndex = tableData.tableIndex
					var tableAlias = tableData.alias;
					var tableMainNode = tableData.nodeName;
					var tableNode = {
						name: matchedtable.tableName,
						firstAttribute: {
								collectionName: tableMainNode
							},
						showtree: false,
						nodes:  [],
						tableIndex: (tableStoredIndex == undefined) ? tableIndex : tableStoredIndex
					};
					//console.log('firstAttribute >>> for : '+matchedtable.tableName+' is >>>> '+tableNode.firstAttribute.collectionName);
					
					// Need to make copies of node objects so that original fetched data will not be altered
					matchedtable.columns.forEach(function(item, index){
						tableNode.nodes.push($.extend({},item));
					});
					
					for (var j = 0; j < tableNode.nodes.length; j++) {
						var curColumn = tableNode.nodes[j];
						var attrForColumn = colAttrData[curColumn.columnName];
						if(keyColumns){
							if(keyColumns.indexOf(curColumn.columnName) > -1){
								curColumn.isKeyColumn = true;
							}
						}
						if(attrForColumn){
							//if(!tableNode.firstAttribute && attrForColumn.attribute){
								//tableNode.firstAttribute = {
									//collectionName: attrForColumn.attribute.collectionName
								//};
							//}
							curColumn.attrData = $.extend({},attrForColumn.attribute);
						}
					}
					$scope.tables.push($.extend({},tableNode));
					$scope.processColumnsForMigration(tableNode.nodes, tableNode);
				}				
				nFetchCounter--;
				if(!nFetchCounter && endCallback){
					//$scope.mappingLoaded();
					endCallback();
				}
			}
			popupFactory.closeProgressPopup(); // Popup that's shown by home page in case of Editing Mapping
		});
	},*/
	
	$scope.processColumnsForMigration = function(data, tableBlock){
		for (var i = 0; i < data.length; i++) {
			data[i].tableRef = tableBlock;
		}
	},
	
	$scope.OnMigrate = function(){
		
		var validateResult = $scope.validateColumns();
		
		if( typeof validateResult == "boolean" && !validateResult){
			return;
		} else {
			if(typeof validateResult == "object" && validateResult.onlyKeyColumnsErrors){
				popupFactory.showConfirmPopup('Skip following error and save?', validateResult.errorHTML+'<br> Yo ucan still save mapping and edit it later. Just make sure to not make event of this mapping unless its complete.', function (){ /////////////////////
					$scope.saveMigrationMapping();
				});
			} else {
				$scope.saveMigrationMapping();
			}
		}
	}
		
	$scope.saveMigrationMapping = function() {
		popupFactory.getTitleDescriptionPopup({
			header: 'Enter name for the migration job.',
			title: $scope.eventName,
			description: $scope.eventDesc
		},function(oData){
			var  strPostData = {
				'createdBy': $scope.UserData.NewData.userId,
				'mapType': 'MongoToOrcl',
				'sourceDbName': $scope.UserData.NewData.sourceDbName,
				'sourceUserName': $scope.UserData.NewData.sourceSchemaName,
				'targetDbName': $scope.UserData.NewData.targetDbName,
				'targetUserName': $scope.UserData.NewData.targetSchemaName,
				/*'nullAllowed': migrateCollections.nullAllowed,
				'mapObject': migrateCollections.collections[0]*/
			}
			
			if($scope.eventID){
				strPostData._id = $scope.eventID;
			}
			
			$scope.generateReverseJavaJSON(strPostData);
			
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
	
	$scope.validateNodes = function(validationObject, nodeGroup, isMainCollection){
		for (var i = 0; i < nodeGroup.length; i++) {
			var node = nodeGroup[i];
			node.columnAttrMap = [];
			if(node.tables){
				node.tables.forEach(function(table, index){
					isKeyColumnPresent = false;
					isParentColumnPresent = false;
					var curTable = table;
					var invalidNodes = [];
					for (var colIndex = 0; colIndex < table.nodes.length; colIndex++) {
						var currentNode = table.nodes[colIndex];
						
						if(!$scope.isAttributeVallid(currentNode)){
							invalidNodes.push(currentNode.columnName);
						}
						if(currentNode.isKeyColumn){
							isKeyColumnPresent = true;
						}
						
						if(!isMainCollection && currentNode.isParentAttribute){
							isParentColumnPresent = true;
						}
					};
					
					if(invalidNodes.length){
						validationObject.invalidAttrList.push({
							tableName : node.nodePath + ' > ' + table.name,
							errorNodes : invalidNodes
						})
					}
					
					if(!isKeyColumnPresent){
						validationObject.nonkeyColumnTables.push(node.nodePath + ' > ' + table.name);
					}
					if(!isParentColumnPresent && !isMainCollection){
						validationObject.nonParentColumnTables.push(node.nodePath + ' > ' + table.name);
					}
				});
			}
			if(node.childNodes){
				$scope.validateNodes(validationObject, node.childNodes);
			}
			// Delete tables data and copy attribute data to colAttrMap
		}
	}
	
	$scope.validateColumns = function(){
		validationObject = {
			invalidAttrList	: [],
			nonkeyColumnTables: [],
			nonParentColumnTables: []
		};
		
		$scope.validateNodes(validationObject, $scope.nodeTableGroup, true);

		if(validationObject.invalidAttrList.length){
			var strErrorHtml = 'The following table attributes are invalid : <br> <ul>'
			for (var k = 0; k < validationObject.invalidAttrList.length; k++) {
				var invalidTable = validationObject.invalidAttrList[k];
				strErrorHtml += '<li>'+invalidTable.tableName + '<ul>';
				for (var l = 0; l < invalidTable.errorNodes.length; l++) {
					var node = invalidTable.errorNodes[l];
					strErrorHtml += '<li>'+node+'</li>'
				}
				strErrorHtml += '</ul></li>';
			}
			strErrorHtml += '</ul><br><br> You can still continue save the mappping by Pressing "YES". Just make sure you dont create event from incomplete Mapping.'
			popupFactory.showConfirmPopup('Error : ', strErrorHtml, function(){
				$scope.saveMigrationMapping();
			}, true);
			return false;
		}
		
		if(validationObject.nonParentColumnTables.length){
			var strErrorHtml = 'The following tables doesn\'t have any column linked with parent. <i class="fa fa-exclamation-circle more-info-tooltip-icon" title=" Atleast one column should be linked to parent (For subcollection)" aria-hidden="true"></i> : <ul>'
				for (var l = 0; l < validationObject.nonParentColumnTables.length; l++) {
					strErrorHtml += '<li>'+validationObject.nonParentColumnTables[l]+'</li>';
				}
			strErrorHtml += '</ul><br><br> You can still continue save the mappping by Pressing "YES". Just make sure you dont create event from incomplete Mapping.';
			popupFactory.showConfirmPopup('Error : ', strErrorHtml, function(){
				$scope.saveMigrationMapping();
			}, true);
			return false;
		}
		
		if(validationObject.nonkeyColumnTables.length){
			var strErrorHtml = 'The following tables doesn\'t have any KeyColumn defined <i class="fa fa-exclamation-circle more-info-tooltip-icon" title=" Atleast one keyColumn is necessary " aria-hidden="true"></i> : <ul>'
			for (var l = 0; l < validationObject.nonkeyColumnTables.length; l++) {
				strErrorHtml += '<li>'+validationObject.nonkeyColumnTables[l]+'</li>';
			}
			strErrorHtml += '</ul>';
			//popupFactory.showInfoPopup('Error : ', strErrorHtml);
			return {errorHTML : strErrorHtml, onlyKeyColumnsErrors: true };
			// return false;
		}
		
		
		return true;
	}
	
	/*$scope.validateColumns_OLD = function(){
		if(!$scope.checkBasic()){
			return false;
		}
		var invalidAttrList = [];
		//var invalidTables = [];
		var nonkeyColumnTables = [];
		for (var i = 0; i < $scope.tables.length; i++) {
			var table = $scope.tables[i];
			var invalidNodes = [];
			var collectionName = '';
			var isKeyColumnPresent = false;
			for (var j = 0; j < table.nodes.length; j++) {
				var currentNode = table.nodes[j];
				if(!$scope.isAttributeVallid(table.nodes[j])){
					invalidNodes.push(table.nodes[j].columnName)
				}
				if(currentNode.isKeyColumn){
					isKeyColumnPresent = true;
				}
				
				if(currentNode.attrData && currentNode.attrData.attributeName){
					if(!collectionName || collectionName.length < currentNode.attrData.collectionName.length){
						collectionName = currentNode.attrData.collectionName;
					}
				}
			}
			if(!collectionName){
				invalidTables.push(table.name)
			}
			if(invalidNodes.length){
				invalidAttrList.push({
					tableName : table.name,
					errorNodes : invalidNodes
				});
			}
			if(!isKeyColumnPresent){
				nonkeyColumnTables.push(table.name);
			}
		}
		if(invalidTables.length){
			var strError = 'Following tables do not have any mapping, Please remove them : <ul>';
			for (var l = 0; l < invalidTables.length; l++) {
				strError += '<li>'+invalidTables[l]+'</li>';
			}
			strError += '</ul>';
			popupFactory.showInfoPopup('Error : ', strError);
			return false;
		}
		if(invalidAttrList.length){
			var strErrorHtml = 'The following table attributes are invalid : <br>'
			for (var k = 0; k < invalidAttrList.length; k++) {
				var invalidTable = invalidAttrList[k];
				strErrorHtml += invalidTable.tableName + '<ul>';
				for (var l = 0; l < invalidTable.errorNodes.length; l++) {
					var node = invalidTable.errorNodes[l];
					strErrorHtml += '<li>'+node+'</li>'
				}
				strErrorHtml += '</ul>';
			}
			popupFactory.showInfoPopup('Error : ', strErrorHtml);
			return false;
		} 
		if(nonkeyColumnTables.length){
			var strErrorHtml = 'The following tables doesn\'t have any KeyColumn defined (Atleast one keyColumn is necessary) : <ul>'
			for (var l = 0; l < nonkeyColumnTables.length; l++) {
				strErrorHtml += '<li>'+nonkeyColumnTables[l]+'</li>';
			}
			strErrorHtml += '</ul>';
			popupFactory.showInfoPopup('Error : ', strErrorHtml);
			return false;
		}
		return true;
	};*/
	
	$scope.isAttributeVallid = function(attr){
		if(attr.isNullable){
			return true;
		}
		if(attr.isSeqGenerated){
			return true;
		}
		if(attr.defaultValue){
			return true;
		}
		if(attr.attrData){
			return true;
		}
		return false;
	}
	
	$scope.selectMappingPopup = function(){
		if($scope.currentAvailableMappings && $scope.currentAvailableMappings.length){ // TODO : Check if its really gonna have data
			$scope.userMappings = $.extend({},$scope.currentAvailableMappings);
			$element.find('#selectMappingPopup').modal('show');
		} else {
			$http({
				method: 'GET',
				url: 'getMappingByMapType',
				params : {mapType : 'OrclToMongo'}
				
			}).then(function successCallback(response) {
				$scope.userMappings = response.data;
				$element.find('#selectMappingPopup').modal('show')
		   });
		}
	}

	/*$scope.getNodeTableGroup = function(){
		var nodeTablegroup = [];
		var nodeTableMap = {};
		var nodeTableColumnMap = {};
		var parentNodesDragged = {};
		var tablesAttended = [];
		var nodes = [];
		for (var i = 0; i < $scope.tables.length; i++) {
			var table = $scope.tables[i];
			var columnAttrMap = [];
			var collectionName = $scope.tables[i].firstAttribute ? $scope.tables[i].firstAttribute.collectionName : '';
			var attributeName = '';
			var keyColumns = [];
			//console.log('FOR TABLE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>'+table.name)
			for (var j = 0; j < table.nodes.length; j++) {
				var currentNode = table.nodes[j];
				if(currentNode.isKeyColumn){
					keyColumns.push(currentNode.columnName);
				}
				if(currentNode.attrData && currentNode.attrData.attributeName){
					//console.log('Found dragged attribute from collection : '+currentNode.attrData.collectionName);
					if(collectionName){console.log('Previous Colleciton Already exist '+collectionName);}
					if(!collectionName || collectionName.length < currentNode.attrData.collectionName.length){
						//console.log('Smaller collection exist Hence >> ');
						if(collectionName){ // Nodes dragged from parent need to be separately populated in referenceAttributes of that individual collection
							if(parentNodesDragged[collectionName]){
								parentNodesDragged[collectionName].push(attributeName);
							} else {
								parentNodesDragged[collectionName] = [attributeName];
							}
							//console.log('Set previous collection as REFERECE : '+collectionName);
						}
						//console.log('Updating Collection name to :'+currentNode.attrData.collectionName);
						//collectionName = currentNode.attrData.collectionName; // No need to set again Already predefined in firstAttribute
						//attributeName = currentNode.attrData.attributeName; // No need to set again Already predefined in firstAttribute
					} else if(collectionName.length > currentNode.attrData.collectionName.length){
						//console.log('Bigger name already exust as : '+collectionName+' >> Hence Set previous collection as REFERECE : '+currentNode.attrData.collectionName);
						if(parentNodesDragged[currentNode.attrData.collectionName]){
							parentNodesDragged[currentNode.attrData.collectionName].push(currentNode.attrData.attributeName);
						} else {
							parentNodesDragged[currentNode.attrData.collectionName] = [currentNode.attrData.attributeName];
						}
					}
				}
				
				var colData = {
					columnData : {
						columnName : currentNode.columnName,
						columnType : currentNode.columnType,
						columnAlias: currentNode.columnAlias,
						tableAlias: 'T_' + table.tableIndex
						//tableAlias: dbUtilities.getTableAlias(table.name, $scope.getAliasSuffix(tablesAttended, table.name))
					},
					isSeqGenerated: currentNode.isSeqGenerated,
					seqName : currentNode.sequenceName,
					literalValueForColumn : currentNode.defaultValue
				};
				if(currentNode.attrData && currentNode.attrData.attributeName){
					colData.attribute = {
						attributeName : currentNode.attrData.attributeName,
						attributeType : currentNode.attrData.attributeType,
						isIdentifier : currentNode.attrData.isIdentifier
					}
					if(currentNode.isParentAttribute){
						colData.isParentAttribute = currentNode.isParentAttribute;
						colData.parentNode = currentNode.parentNode;
					}
					if(currentNode.isChildAttribute){
						colData.isChildAttribute = currentNode.isChildAttribute;
						colData.ChildAttributeNode = currentNode.ChildAttributeNode;
					}
					
					
					columnAttrMap.push(colData); // Add only columns which has attributes linked to it.
				} else if(currentNode.isSeqGenerated || currentNode.defaultValue){
					columnAttrMap.push(colData); // Add only columns which has attributes linked to it or has some data defined for it.
				}
			}
			if(collectionName){
				if(!nodeTableMap[collectionName]){
					nodes.push(collectionName);
					nodeTableMap[collectionName] = [{
						tableName : table.name,
						//tableAlias : dbUtilities.getTableAlias(table.name, $scope.getAliasSuffix(tablesAttended, table.name)),
						tableAlias : 'T_' + table.tableIndex,
						keyColumns : keyColumns.length ? keyColumns : undefined,
						tableIndex : table.tableIndex
					}]
				} else {
					nodeTableMap[collectionName].push({
						tableName : table.name,
						tableAlias : 'T_' + table.tableIndex,
						//tableAlias : dbUtilities.getTableAlias(table.name, $scope.getAliasSuffix(tablesAttended, table.name)),
						keyColumns : keyColumns.length ? keyColumns : undefined,
						tableIndex : table.tableIndex
					})
				}
			};
			if(nodeTableColumnMap[collectionName]){
				nodeTableColumnMap[collectionName].push.apply(nodeTableColumnMap[collectionName], columnAttrMap);
			} else {
				nodeTableColumnMap[collectionName] = columnAttrMap;
			}
			tablesAttended.push(table.name);
		}
		for (var k = 0; k < nodes.length; k++) {
			var collection = nodes[k];
			var nodetableObject = {
				nodeName : collection,
				tableList : nodeTableMap[collection],
				columnAttrMap : nodeTableColumnMap[collection]
			};
			if(parentNodesDragged[collection]){
				nodetableObject.referenceAttributes = parentNodesDragged[collection];
				delete parentNodesDragged[collection];
			}
			nodeTablegroup.push(nodetableObject);
		}
		if(!$.isEmptyObject(parentNodesDragged)){
			// Add remaining single collection That does not have any table defined
			for(e in parentNodesDragged){
				nodeTablegroup.push({
					nodeName : e,
					tableList: [],
					columnAttrMap: [],
					referenceAttributes: parentNodesDragged[e]
				});
			}
		}
		
		return nodeTablegroup;
	}*/
	
	$scope.getAliasSuffix = function(array, element){
		var count = 0;
		for (var i = 0; i < array.length; i++) {
			if(element === array[i]){
				count++;
			}
			
		}
		return count;
	};
	
	/*$scope.generateReverseJavaJSON_OLD = function(jsonPassed){
		var javaJson = jsonPassed ? jsonPassed : {};
		if(!$scope.collections || !$scope.collections.length){
			javaJson.collectionName = $scope.currentCollectionName ? $scope.currentCollectionName : 'NotDefined';
		} else {
			javaJson.collectionName = $scope.collections[0].attributeName;
		}
		javaJson.nodeTableGroup = $scope.getNodeTableGroup();
		//javaJson.columnAttrMap = $scope.getColumnAttribMap();
		if(!jsonPassed){
			return javaJson;
		}
	}*/
	
	$scope.generateReverseJavaJSON = function(jsonPassed){
		var javaJson = jsonPassed ? jsonPassed : {};
		if(!$scope.collections || !$scope.collections.length){
			javaJson.collectionName = $scope.currentCollectionName ? $scope.currentCollectionName : 'NotDefined';
		} else {
			javaJson.collectionName = $scope.collections[0].attributeName;
		}
		javaJson.nodeTableGroup = $scope.generateCleanJSON(javaJson);
		//javaJson.columnAttrMap = $scope.getColumnAttribMap();
		if(!jsonPassed){
			return javaJson;
		}
	}
	
	$scope.processNodes = function(nodeGroup, oTracker){
		for (var i = 0; i < nodeGroup.length; i++) {
			var node = nodeGroup[i];
			node.columnAttrMap = [];
			if(node.tables){
				node.tableList.forEach(function(tableEntry, index){
					if(tableEntry.keyColumns){
						tableEntry.keyColumns.length = 0;
					}
				})
				node.tables.forEach(function(table, index){
					var curTable = table;
					for (var colIndex = 0; colIndex < table.nodes.length; colIndex++) {
						var column = table.nodes[colIndex];
						if(column.isKeyColumn){
							node.tableList.forEach(function(tableEntry, index){
								if(tableEntry.tableName == curTable.name){
									if(!tableEntry.keyColumns){
										tableEntry.keyColumns = [];
									}
									if(tableEntry.keyColumns.indexOf(column.columnName) == -1){
										tableEntry.keyColumns.push({
											columnName : column.columnName,
											columnType : column.columnType,
											tableAlias: 'T' + oTracker.tableIndex,
											//columnAlias: column.columnAlias,
											isKeyColumn: column.isKeyColumn,
											isNullable: column.allowNullKeyColumn
										});
									}
								}
							})
						}
						if(column.attrData || column.isSeqGenerated || column.defaultValue || column.replacementMap){
							var attrColumn = {
								columnData : {
									columnName : column.columnName,
									columnType : column.columnType,
									tableAlias: 'T' + oTracker.tableIndex,
									//columnAlias: column.columnAlias,
									isKeyColumn: column.isKeyColumn
								},
								isSeqGenerated: column.isSeqGenerated,
								seqName : column.sequenceName,
								literalValueForColumn : column.defaultValue,
								replacementMap: column.replacementMap
							};
							
							node.tableList.forEach(function(tableEntry, index){
								if(tableEntry.tableName == curTable.name){
									tableEntry.tableAlias = 'T' + oTracker.tableIndex; // Update tableAlias as per new generated in columnData
								}
								if(tableEntry.joinedTables){
									delete tableEntry.joinedTables; // JoinedTables info not required in Reverse Migration
								}
							})
							
							if(column.isParentAttribute){
								if(!$scope.refAttributeData){
									$scope.refAttributeData = {};
								}
								if(!$scope.refAttributeData[column.parentNode]){
									$scope.refAttributeData[column.parentNode] = [column.attrData.attributeName];
								} else {
									if($scope.refAttributeData[column.parentNode].indexOf(column.attrData.attributeName) == -1){
										$scope.refAttributeData[column.parentNode].push(column.attrData.attributeName);
									}
								}
								//$scope.addReferenceAttribute(column.parentNode, column.attrData.attributeName);
							}
							attrColumn.isParentAttribute = column.isParentAttribute;
							attrColumn.parentNode = column.parentNode;
							attrColumn.isChildAttribute = column.isChildAttribute;
							attrColumn.ChildAttributeNode = column.ChildAttributeNode;
							
							if(column.attrData && !column.isSeqGenerated){
								attrColumn.attribute = {
									attributeName : column.attrData.attributeName,
									attributeType : column.attrData.attributeType,
									isIdentifier : column.attrData.isIdentifier
								};
							}
							
							node.columnAttrMap.push(attrColumn);
						}
					};
					
					oTracker.tableIndex++;
					
				});
				
				oTracker.nodeIndex++;
				delete node.tables;
				delete node.parentNodeName;
				delete node.nodePath;
			}
			if(node.childNodes){
				$scope.processNodes(node.childNodes, oTracker);
			}
			// Delete tables data and copy attribute data to colAttrMap
		}
	}
	
	/*$scope.addReferenceAttribute = function(NodePath, attributeName){
		
	}*/
	
	$scope.generateCleanJSON = function(javaJson){
		javaJson.nodeGroup = JSON.parse(angular.toJson($scope.nodeTableGroup));
		var oTracker = {
				tableIndex: 0,
				nodeIndex : 0
		}
		$scope.processNodes(javaJson.nodeGroup, oTracker);
		if($scope.refAttributeData){
			$scope.processReferenceData(javaJson.nodeGroup);
		}
		$scope.refAttributeData = {};
	}
	
	$scope.processReferenceData = function(nodeGroup){
		for (var i = 0; i < nodeGroup.length; i++) {
			var node = nodeGroup[i];
			if($scope.refAttributeData[node.nodeName]){
				node.referenceAttributes = $scope.refAttributeData[node.nodeName];
			}
			if(node.childNodes){
				$scope.processReferenceData(node.childNodes);
			}
		}
	}
	
	$scope.checkBasic = function(){
		/*if(!$scope.tables || !$scope.tables.length){
			popupFactory.showInfoPopup('Almost Done!!','No Tables found on the Right Side. Please load a table.');
			return false;
		}*/
		/*if(!$scope.collections || !$scope.collections.length){
			popupFactory.showInfoPopup('Almost Done!!','No collection found on the Left Side. Please load a collection.');
			return false;
		}*/
		return true;
	}
	
	$scope.showMigrateJSON = function(){
		if($scope.checkBasic()){
			var javaJson = $scope.generateReverseJavaJSON();
			popupFactory.showInfoPopup('Java Migrate JSON : ', "<pre>"+JSON.stringify(javaJson, undefined, 2)+"</pre>");
		}
	}
	$scope.showExportJSON = function(){
		
	}
	
	if(devUtilities.isDebugMode()){
		//$scope.collections.push(devUtilities.getReverseCollection());
		//$scope.tables.push(devUtilities.getReverseTables());
	}
	
	$scope.fetchCurrentJSONIfExist();
});

DBMigratorApp.controller('dbAddTableSection', function ($scope, $rootScope, $interval, popupFactory, devUtilities, $element, dbUtilities, $http){
	
	$scope.fetchedTables = [];
	$scope.selectedTable = '';
	
	if($scope.tables.length == 0){
		dbUtilities.clearAliasData();
	}
	/*$scope.AddTableToGroup = function(){
		//
	}*/
	
	
	
	$scope.selectTable = function (table){
		table.selected = !table.selected;
	}
	
	$scope.loadSelectedTables = function (){
		$scope.fetchingColumns = true;
		var tCounter = 0;
		$scope.addTables = [];
		for (var i = 0; i < $scope.fetchedTables.length; i++) {
			if($scope.fetchedTables[i].selected){
				$scope.addTables.push($scope.fetchedTables[i].tableName);
			}
		}
		$scope.fetchedTables.length = 0;
		for (var i = 0; i < $scope.addTables.length; i++) {
			var table = $scope.addTables[i];
			console.log('Calling AJAX with table : '+table)
			tCounter++;
			$http({
				method: 'POST',
				url :"fetchColumnsDetails",
				params : {tableName : table, sourceDatabaseName : $scope.UserData.NewData.targetDbName, sourceDatabaseSchema: $scope.UserData.NewData.targetSchemaName, userName: $scope.UserData.NewData.userId}
			}).then(function successCallback(response) {
				console.log(response);
				var tableBlock = {
						name: response.config.params.tableName,
						nodes: response.data,
						showtree: true
					};
				$scope.tables.push(tableBlock);
				$scope.processColumnsForMigration(response.data, tableBlock);
				dbUtilities.registerTable(table);
				tCounter--;
				if(!tCounter){
					$scope.fetchingColumns = false;
					$scope.addTables.length = 0;
					$element.modal('hide');
				}
			})
		}
	},
	
	$scope.resetAddtablePopup = function(){
		$scope.fetchedTables.length = 0;
		$scope.fetchingData = false;
		$scope.fetchingColumns = false;
		$scope.noTableFound = false;
		$scope.selectedTable = '';
	}
	
	$scope.processFetchedTables = function(tables){
		for (var i = 0; i < tables.length; i++) {
			$scope.fetchedTables.push({
				tableName : tables[i]
			});
		}
	}
	
	$scope.searchTables = function(){
		$scope.noTableFound = false;
		$scope.fetchedTables.length = 0;
		$scope.fetchingData = true;
		if($scope.searchString && $scope.searchString.length > 1){
			var strPostData = "pattern="+$scope.searchString+"&sourceDatabaseName="+$scope.UserData.NewData.targetDbName+"&sourceDatabaseSchema="+$scope.UserData.NewData.targetSchemaName+"&userName="+$scope.UserData.NewData.userId;
			//popupFactory.showProgressPopup('Searching table : '+($scope.searchString ? $scope.searchString : '')+'...');
			$.ajax({				  
				type :"POST",
			    url :"fetchTablesDetails",
			    data : strPostData,
				async: true,			
				success: function(data){
					$scope.processFetchedTables(data);
					//$scope.fetchedTables = data;
					$scope.fetchingData = false;
					if($scope.fetchedTables.length == 0){
						$scope.noTableFound = true;
					}
					$scope.$apply();
				},
				fail: function(){
					popupFactory.showInfoPopup('Error', 'Failed to fetch tables.');
				}
			});
		}
	};
	
	if(devUtilities.isDebugMode()){
		//$scope.processFetchedTables(["CO_ORDER_ENTITY_INDEX_STATUS_V","CO_ORDER","CO_ORDER_ACCESS","CO_ORDER_ADDRESS_ATTRIBUTES","CO_ORDER_ADDRESS_B2B"]);
		$scope.processFetchedTables(["ORDER_ERROR_LOG","ORDER_ORCH_APPLICATION_CONTEXT"]);
	}
});

DBMigratorApp.controller('dbSelectMappingSection', function ($scope, $rootScope, $interval, popupFactory, devUtilities, $element, dbUtilities, $http){
	$scope.selectMapping = function (mapping){
		$scope.mapToLoad = mapping._id.$oid;
		$scope.mapNameToLoad = mapping.mapName;
	}
	
	$scope.resetPopup = function(){
		$scope.mapToLoad = '';
		$scope.mapNameToLoad = '';
		$scope.fetchingMappingData = false;
		$scope.collections.length = 0;
	}
	
	$scope.LoadMapping = function(bAutoGenerate){
		if($scope.mapToLoad){
			$scope.collections.length = 0;
			$scope.fetchingMappingData = true;
			$scope.tables.length = 0;
			$http({
				method: 'GET',
				url: 'loadMapping',
				params : {mappingId : $scope.mapToLoad}
			}).then(function successCallback(response) {
				
				$scope.jsUserData.NewData.sourceDbName = response.data.targetDbName;
				$scope.jsUserData.NewData.sourceSchemaName = response.data.targetUserName;
				$scope.jsUserData.NewData.targetDbName = response.data.sourceDbName;
				$scope.jsUserData.NewData.targetSchemaName = response.data.sourceUserName;
				
				$scope.processMappingJavaJSON(response.data);
				if(bAutoGenerate){
					$scope.tables.length = 0;
					$http({
						method: 'GET',
						url: 'getTranslatedMap', // NEW API
						params : {mappingId : $scope.mapToLoad}
						
					}).then(function successCallback(response) {
						console.log(response);
						$scope.processReverseJavaJSON(response.data, $scope.mappingLoaded);
						//$scope.mappingLoaded();
				   });
				} else {
					$scope.mappingLoaded();
				}
		   });
			
			
		}
	},
	
	$scope.mappingLoaded = function(){
		$scope.mapToLoad = '';
		$scope.mapNameToLoad = '';
		$scope.fetchingMappingData = false;
		$element.modal('hide');
	}
	
	$scope.processMappingJavaJSON = function (oJson){
		$scope.loadedCollection = {};
		var collection = oJson.mapObject;
		$scope.eventName = oJson.mapName;
		$scope.eventDesc = oJson.eventDescription ? oJson.eventDescription : 'No Description Found';
		dbUtilities.populateJavaCollection($scope.loadedCollection, collection, true, true, '', true);
		//$scope.processTableDataAndAlias($scope.loadedCollection.sourceTables, $scope.loadedCollection); // TODO : this might be required
		//console.log(JSON.stringify($scope.loadedCollection));
		$scope.collections.push($scope.loadedCollection);
	}
});

DBMigratorApp.controller('dbRevereseSourceSection', function ($scope, $rootScope, $interval, popupFactory, devUtilities, $element, dbUtilities){
	
	$rootScope.$on('get-parents-collection-details', function(e, data){
		if($scope.collections.length){
			var returnData = {};
			var nCollectionPath = data.collectionName.split(".");
			if(nCollectionPath.length){
				if(nCollectionPath.length == 1){
					returnData.allCollectionTypeParents= true;
				} else {
					returnData.allCollectionTypeParents = $scope.keepCheckingChild(nCollectionPath);
				}
				if(data.fnCallback){
					data.fnCallback(returnData);
				}
			}
		}
	});
	
	$scope.keepCheckingChild = function(arrNodes, mainCollection){
		return true;
		var allParentcCollection = true;
		if(!mainCollection){
			mainCollection = $scope.collections[0];
			arrNodes.shift();
		}
		if(arrNodes.length){
			var colName = arrNodes.shift();
			if(mainCollection.attrs.length > 0){
				for (var i = 0; i < mainCollection.attrs.length; i++) {
					if(mainCollection.attrs[i].attributeName == colName){
						if(mainCollection.attrs[i].attributeType == "COLLECTION"){
							allParentcCollection = true;
							$scope.keepCheckingChild(arrNodes, mainCollection.attrs[i]);
						} else {
							console.log('AS attributeType : '+mainCollection.attrs[i].attributeType+' is not COLLECTION >>>>')
							return false;
						}
					}
					
				}
			} else {
				console.log('AS mainCollection.attrs.length : '+mainCollection.attrs.length+' IS ZERO >>>>')
				return false;
			}
		} else {
			//console.log('RETURNING allParentcCollection '+allParentcCollection+' >>>>')
			return allParentcCollection;
		}
	}
});

DBMigratorApp.controller('dbReverseTargetSection', function ($scope, $rootScope, $interval, popupFactory, devUtilities, $element, dbUtilities){
	$scope.addNewTable = function(){
		$element.find("#AddTableToGroupPopup").modal();
	};
	
	$scope.editColumnConfig = function(node){
		$scope.currentEditingColumnRef = node;
		//$scope.currentEditingColumn = $.extend({},node);
		//console.log('Set $scope.currentEditingColumn : AS');
		//console.log($scope.currentEditingColumn);
		
		$scope.currentEditingColumn = {
			"columnName": node.columnName ? node.columnName : 'Not Defined',
	        "columnType": node.columnType ? node.columnType : "Not Defined",
	        "precision": node.precision ? node.precision : 0,
	        "isNullable": node.isNullable,
	        "isSeqGenerated": node.isSeqGenerated,
	        "sequenceName": node.seqName,
	        "defaultValue": node.defaultValue ? node.defaultValue : '',
        	"attrData": node.attrData ? node.attrData : null,
			"isKeyColumn": node.isKeyColumn ? node.isKeyColumn : false
			//"replacementMap": node.replacementMap ? node.replacementMap : {}
		}
		
		$scope.currentEditingColumn.replacementMap = [];
		if(node.replacementMap){
			for (var key in node.replacementMap) {
			   console.log(' name=' + key + ' value=' + node.replacementMap[key]);
			   $scope.currentEditingColumn.replacementMap.push({
				   replaceKeyName: key,
				   replaceKeyValue: node.replacementMap[key]
			   });
			}
		}
		
		
		$element.find('.column_data_editor_popup').modal('show');
		if(node.seqName){
			$scope.$broadcast('dataloaded', node.seqName);
		} else {
			$scope.$broadcast('dataloaded');
		}
	}
	
	$scope.clearEditingAttribs = function(){
		$scope.currentEditingColumnRef = null;
		$scope.currentEditingColumn = null;
	}
	
	$scope.saveColumnConfig = function(){
		//$scope.currentEditingColumn.columnName
	}
});

DBMigratorApp.controller("DBTargetTableTreeController", function($scope, $rootScope, $interval, popupFactory, $element) {
    
    $scope.getColumns = function(data, showTree, fnCallback){
    	if(!showTree){
    		showTree = false;
    	}
    	data.showtree = !data.showtree;
    };
    
    $scope.hasValidKeyColumn = function(){
    	for (var i = 0; i < $scope.data.nodes.length; i++) {
			if($scope.data.nodes[i].isParentAttribute || $scope.data.nodes[i].rootNodeColumn){
				return true;
			}
		}
    	return false;
    }
    
    $scope.addTableOnTop = function(ev, avoidRefresh){
    	console.log(ev.originalEvent.dataTransfer.getData("text"));
    	var xferData = JSON.parse(ev.originalEvent.dataTransfer.getData("text"));
    	if(xferData.type != "nodeTableDrag"){
    		popupFactory.showInfoPopup('Access Denied!!', 'Only tables allowed to drag over tables to re-arrange.', false);
    		return;
    	}
    	if($scope.nodeGroup.nodeName == xferData.nodeName){
			if($scope.data.name != xferData.tableName){
				var tableMatches = $.grep($scope.nodeGroup.tables, function(e){ return e.name == xferData.tableName; });
				var matchedtable = tableMatches[0];
		    	targetIndex = $scope.nodeGroup.tables.indexOf(matchedtable);
		    	sourceIndex = $scope.nodeGroup.tables.indexOf($scope.data);
		    	
		    	var temp = $scope.nodeGroup.tables[sourceIndex];
		    	$scope.nodeGroup.tables[sourceIndex] = $scope.nodeGroup.tables[targetIndex];
		    	$scope.nodeGroup.tables[targetIndex] = temp;
		    	if(!avoidRefresh){
		    		$scope.$apply();
		    	}
			}
		} else {
			popupFactory.showInfoPopup('Access Denied!!', 'Only tables from same node can be rearranged !!.', false);
		}
    }
    
    $scope.checkKeyColumns = function(){
		if($scope.data.nodes.length){
			for (var i = 0; i < $scope.data.nodes.length; i++) {
				if($scope.data.nodes[i].isKeyColumn){
					return true;
				}
			}
		}
		return false;
	}
    
    /*$scope.removeTable = function(table){
    	popupFactory.showConfirmPopup('Are you sure?', 'Do you want to remove table '+table.name+' from group ?', function (){
    		var iIndex = $scope.tables.indexOf(table);
        	if(iIndex >= 0){
        		$scope.tables.splice(iIndex, 1);
        		$scope.$apply();
        	}
		});
    	
    };*/
    
    $scope.selectKeyColumns = function(){
    	console.log($element);
    	$element.find('#selectKeyColumnsPopup').modal('show');
    }
});

DBMigratorApp.controller('selectKeyColumnController', function ($scope, $rootScope, $interval, popupFactory, dbUtilities, $element){
	$scope.selectKeyColumn = function(node){
		node.isKeyColumn = !node.isKeyColumn;
	}
	
	$scope.setAllowNullKeyColumn = function($event, node){
    	if(node.isKeyColumn){
    		node.allowNullKeyColumn = !node.allowNullKeyColumn;
    		$event.stopPropagation();
    	}
    	
    }
    
});


DBMigratorApp.controller('dbSourceCollectionInstanceController', function ($scope, $rootScope, $interval, popupFactory, dbUtilities, $element){
	//$scope.attribEditMode = false;
	$scope.isExpanded = false;
	
	$scope.toggleExpansion = function(){
		$scope.isExpanded = !$scope.isExpanded;
	},
	
	$scope.$on('expand-node-groups', function(evt){
		$scope.isExpanded = true;
	});
	
	$scope.explandAll = function(){
		$scope.isExpanded = true;
		$scope.$broadcast('expand-node-groups');
	}
	
	$scope.getNodeTemplate = function(attr){
		if(dbUtilities.isCollectionType(attr.attributeType)){
			attr.isChildNode = true;
			return 'source_collection_instance_renderer.html';
		} else {
			return 'source_collection_end_node.html';
		}
	}
});


DBMigratorApp.controller('dbTargetCollectionTablesInstanceController', function ($scope, $rootScope, $interval, popupFactory, dbUtilities, $element){
	//$scope.attribEditMode = false;
	$scope.isExpanded = false;
	
	$scope.isDataToShow = function(){
		if($scope.currentCollectionName){
			if($scope.nodeGroup.tables && $scope.nodeGroup.tables.length){
				return true;
			} else if($scope.nodeGroup.childNodes && $scope.nodeGroup.childNodes.length){
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	$scope.$on('expand-node-groups', function(evt){
		$scope.isExpanded = true;
	});
	
	
	
	$scope.expandAll = function(){
		$scope.isExpanded = true;
		if($scope.nodeGroup.childNodes && $scope.nodeGroup.childNodes.length){
			$scope.$broadcast('expand-node-groups');
			/*$scope.nodeGroup.childNodes.forEach(function(node, index){
				$scope.expandAll(node);
			});*/
		}
	}
	
	$scope.isRootNode = function(){
		if($scope.nodeGroup.nodePath && $scope.nodeGroup.nodePath.length){
			if($scope.nodeGroup.nodePath.indexOf(".") == -1){
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	$scope.toggleExpansion = function(){
		$scope.isExpanded = !$scope.isExpanded;
	},
	
	$scope.initializeParams = function(){
		
	},
	
	$scope.removeNode = function(nodeGroup){
    	popupFactory.showConfirmPopup('Are you sure?', 'Do you want to remove node '+nodeGroup.nodeName+' from group ?', function (){
    		$scope.$emit('delete-node-group', nodeGroup);
		});
    };
    
    $scope.$on('delete-node-group', function(evt, nodeGroup){
    	if($scope.nodeGroup != nodeGroup){ // As same scope can listen to the $emit event
    		if($scope.nodeGroup.childNodes){
    			for (var i = 0; i < $scope.nodeGroup.childNodes.length; i++) {
					var array_element = $scope.nodeGroup.childNodes[i];
					if(array_element.nodeName == nodeGroup.nodeName){
						$scope.nodeGroup.childNodes.splice(i, 1);
        				evt.stopPropagation();
        				$scope.$apply();
					}
				}
        	}
    	}
	});
	
	$scope.removeTable = function(table){
    	popupFactory.showConfirmPopup('Are you sure?', 'Do you want to remove table '+table.name+' from group ?', function (){
    		var iIndex = $scope.nodeGroup.tables.indexOf(table);
        	if(iIndex >= 0){
        		$scope.nodeGroup.tables.splice(iIndex, 1);
        		$scope.$apply();
        	}
		});
    	
    };
	
	$scope.addNodeOnTop = function(ev){
		console.log(ev.originalEvent.dataTransfer.getData("text"));
		var xferData = JSON.parse(ev.originalEvent.dataTransfer.getData("text"));
		if(xferData.type != "nodeDrag"){
    		popupFactory.showInfoPopup('Access Denied!!', 'Only nodes are allowed to drag over sibling nodes to re-arrange.', false);
    		return;
    	}
		if($scope.nodeGroup.parentNodeName == xferData.parentNode){
			var parentNodes = angular.element($($element).closest('.collection-elements-subdiv')[0]).scope().nodeGroup.childNodes;
			var indexNode = parentNodes.indexOf($scope.nodeGroup);
			var targetIndex = 0;
			parentNodes.forEach(function(node, index){
				if(node.nodeName == xferData.nodeName){
					targetIndex = index;
					targetNode = node;
				}
			});
			
			var temp = parentNodes[indexNode]
			parentNodes[indexNode] = parentNodes[targetIndex]
			parentNodes[targetIndex] = temp;
			$scope.$apply();
			
		} else {
			popupFactory.showInfoPopup('Access Denied!!', 'Only sibling nodes can be rearranged !!.', false);
		}
	},
	
	$scope.getTargetNodeTemplate = function(attr){
		if(dbUtilities.isCollectionType(attr.attributeType)){
			return 'source_collection_instance_renderer.html';
		} else {
			return 'source_collection_end_node.html';
		}
	}
});

DBMigratorApp.controller('targetColumnDataEditor', function ($scope, $rootScope, $interval, popupFactory, dbUtilities, $element){
	$scope.saveChanges = function(){
		if($scope.validData()){
	        $scope.currentEditingColumnRef.defaultValue = $scope.currentEditingColumn.defaultValue;
	        $scope.currentEditingColumnRef.isSeqGenerated = $scope.currentEditingColumn.isSeqGenerated;		
	        $scope.currentEditingColumnRef.sequenceName = $scope.currentEditingColumn.sequenceName;
	        $scope.currentEditingColumnRef.isKeyColumn = $scope.currentEditingColumn.isKeyColumn;
	        
	        if($scope.currentEditingColumn.isSeqGenerated){
	        	$scope.currentEditingColumnRef.sequenceName = $scope.currentEditingColumn.sequenceName;
	        }
	        if($scope.currentEditingColumnRef.attrData != $scope.currentEditingColumn.attrData){
	        	$scope.currentEditingColumnRef.attrData = $scope.currentEditingColumn.attrData;
	        	/* If linking data is removed then attributes related to linked data need to be reset */
	        	if($scope.currentEditingColumn.attrData == null){
	        		$scope.currentEditingColumnRef.isParentAttribute = false;
	        	}
	        	/*if(!$scope.currentEditingColumn.attrData){
	        		$scope.checkFirstAttributeReference($scope.currentEditingColumnRef.tableRef);
	        	}*/
	        }
	        
	        //$scope.currentEditingColumnRef.replacementMap = $scope.currentEditingColumn.replacementMap; // Replacement Map should be an objectwith key value pair not an Array
	        $scope.currentEditingColumnRef.replacementMap = {};
	        
	        for (var i = 0; i < $scope.currentEditingColumn.replacementMap.length; i++) {
				var currentMap = $scope.currentEditingColumn.replacementMap[i];
				$scope.currentEditingColumnRef.replacementMap[currentMap.replaceKeyName] = currentMap.replaceKeyValue;
			}
	        
	        $scope.clearEditingAttribs();
			$element.modal('hide');
		}
	}
	
	$scope.addReplaceMap = function(){
		if(!$scope.currentEditingColumn.replacementMap){
			$scope.currentEditingColumn.replacementMap = [];
		}
		
		$scope.currentEditingColumn.replacementMap.push({
			replaceKeyName: '',
			replaceKeyValue: ''
		});
	}
	
	$scope.$on('autocomplete-selected', function(e, seqName){
		$scope.currentEditingColumn.sequenceName = seqName;
	});
	
	$scope.findSeqNames = function(SeqName){
		$scope.currentEditingColumn.fetchingData = true;
		$.ajax({				  
			type :"GET",
		    url :"fetchSequenceNames",
		    /*data : "seqPattern="+$scope.currentEditingColumn.sequenceName+"&sourceDatabaseName="+$scope.UserData.NewData.targetDbName+"&sourceDatabaseSchema="+$scope.UserData.NewData.targetSchemaName,*/
		    data : "seqPattern&sourceDatabaseName="+$scope.UserData.NewData.targetDbName+"&sourceDatabaseSchema="+$scope.UserData.NewData.targetSchemaName,
		    success: function(data){
		    	$scope.autoCompleteValues = [];
		    	for (var i = 0; i < data.length; i++) {
		    		$scope.autoCompleteValues.push({
		    			label:  data[i]
		    		});
				}
		    	$scope.currentEditingColumn.fetchingData = false;
		    	$scope.$apply();
		    	if(!SeqName){
		    		$scope.$broadcast('dataloaded', $scope.currentEditingColumn.sequenceName);
		    	} else {
		    		$scope.$broadcast('dataloaded', SeqName);
		    	}
			}
		});
	}
	
	//$scope.$watch('currentEditingColumn.isSeqGenerated')
	$scope.$watch('currentEditingColumn.isSeqGenerated', function(newValue, oldValue){
		/*if(newValue){
			$scope.$broadcast('dataloaded', {'DBName': $scope.UserData.NewData.sourceDbName, 'DBScheme': $scope.UserData.NewData.sourceSchemaName});
		}*/
		if(newValue){
			if(!$scope.autoCompleteValues || !$scope.autoCompleteValues.length){
				$scope.findSeqNames();
			};
			if($scope.attrData){
				$scope.removeAttribData();
			};
			
			$scope.currentEditingColumn.defaultValue = '';
		}
		
	});
	
	
	
	// If user removes all the dragged atributes from table > We need to remove firstAttribute data as user can select new subcollection limit
	/*$scope.checkFirstAttributeReference = function(table){
		if(table.firstAttribute){
			for (var i = 0; i < table.nodes.length; i++) {
				if(table.nodes[i].attrData){
					return;
				}
			}
			table.firstAttribute = null;
		}
	}*/
	
	$scope.removeAttribData = function(){
		$scope.currentEditingColumn.attrData = null;
	}
	
	$scope.validData = function(){
		if(!$scope.currentEditingColumn.errors){
			$scope.currentEditingColumn.errors = [];
		} else {
			$scope.currentEditingColumn.errors.length = 0;
		}
		var isValid = true;
		if(!$scope.currentEditingColumn.isNullable){
			if(!$scope.currentEditingColumn.isSeqGenerated){
				if(!$scope.currentEditingColumn.defaultValue && !$scope.currentEditingColumn.attrData){
					$scope.currentEditingColumn.errors.push('Please specify default value');
					isValid = false;
				}
			}
		}
		
		if($scope.currentEditingColumn.replacementMap.length){
			for (var i = 0; i < $scope.currentEditingColumn.replacementMap.length; i++) {
				var map = $scope.currentEditingColumn.replacementMap[i];
				var errorStr = '';
				if(map.replaceKeyName.trim() === ""){
					errorStr = 'Replacement Key Name should not be empty';
					if($scope.currentEditingColumn.errors.indexOf(errorStr) < 0){
						$scope.currentEditingColumn.errors.push(errorStr);
						isValid = false;
					}
				}
				if(map.replaceKeyValue.trim() === ""){
					errorStr = 'Replacement Key value should not be empty';
					if($scope.currentEditingColumn.errors.indexOf(errorStr) < 0){
						$scope.currentEditingColumn.errors.push(errorStr);
						isValid = false;
					}
				}
			}
		}
		return isValid;
	}
});

DBMigratorApp.controller('dbTargetColumnAttributeNode', function ($scope, $interval, popupFactory, $rootScope, $controller){
	angular.extend(this, $controller('expandableNode', {$scope: $scope}));
	
	$scope.dropAttribute = function(ev){
		var xferData = JSON.parse(ev.originalEvent.dataTransfer.getData("text"));
		//{"type":"collectionAttr", "name": "CREATED_ON", collection="Check23"}
		if(!xferData.dragType || xferData.dragType != "attributeDrag"){
			popupFactory.showInfoPopup('Access Denied!!', 'Only collection attributes are allowed to drag !!.', false);
			return;
		}
		if(!$scope.node.tableFirstColumn){
			$scope.node.tableFirstColumn = xferData.collection
		};
		console.log($scope.node.tableFirstColumn);
		// Check currently dragged attribute column name is VALID > Has to be parent or itself of $scope.node.tableRef.firstAttribute.collectionName
		if($scope.node.tableFirstColumn.indexOf(xferData.collection) < 0){
			if(xferData.collection.indexOf($scope.node.tableFirstColumn) >= 0){
				/* If its a child attribute [child to firstAttribute] then, only allow if all the parents till root are of type COLLECTION, else don't allow to drag */
				$rootScope.$emit('get-parents-collection-details', {
					collectionName: xferData.collection, 
					fnCallback: function(parentData){
						if(!parentData){
							return;
						}
						if(parentData.allCollectionTypeParents){
							$scope.node.isChildAttribute = true;
							$scope.node.ChildAttributeNode = xferData.collection;
							$scope.node.attrData = {
								attributeName: xferData.name,
								collectionName: xferData.collection,
								attributeType : xferData.type,
								isIdentifier : (xferData.isIdentifier == "true") ? true : false
							};
							$scope.editColumnConfig($scope.node);
							$scope.$apply();
						}
					}
				});
			} else {
				popupFactory.showInfoPopup('Access Denied!!', 'Only attributes from collection "'+$scope.node.tableFirstColumn+'" or its parent are allowed !!.', false);
				return;
			}
			
		} else {
			if($scope.node.tableFirstColumn.length !== xferData.collection.length){
				$scope.node.isParentAttribute = true;
				$scope.node.parentNode = xferData.collection;
			}
		}
		
		$scope.node.attrData = {
			attributeName: xferData.name,
			collectionName: xferData.collection,
			attributeType : xferData.type,
			isIdentifier : (xferData.isIdentifier == "true") ? true : false
		};
		$scope.editColumnConfig($scope.node);
		$scope.$apply();
	}
	
	$scope.isAttributeDefined = function(skipNullCheck){
		if(!skipNullCheck){
			if($scope.node.isNullable){
				return true;
			}
		}
		
		if($scope.node.isSeqGenerated){
			return true;
		}
		if($scope.node.defaultValue){
			return true;
		}
		if($scope.node.attrData){
			return true;
		}
		return false;
	}
});